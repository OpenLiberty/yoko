/*
 * Copyright 2026 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.openliberty.yoko.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.FindSourceFiles;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.openrewrite.Tree.randomId;

/**
 * Turns a chain of if / else-if tests of one variable against constants into a {@code switch} statement:
 * <pre>
 * if (s.equals("a") || s.equals("b")) { ... } else if (s.equals("c")) { ... } else if (...) { ... } else { ... }
 * </pre>
 * Only chains that can be converted without any change in behaviour are converted. In particular
 * <ul>
 * <li>the tested expression must be a variable, field or array element, tested at least three times;</li>
 * <li>strings are tested with {@code x.equals("literal")} (so a null throws, just as in a switch),
 *     and enum constants with {@code x.equals(Enum.CONSTANT)};</li>
 * <li>there must be at least two separate branches;</li>
 * <li>{@code char}, {@code int}, {@code short} and {@code byte} values are tested with {@code ==} against literals;</li>
 * <li>enums compared with {@code ==} are left alone, because {@code switch} would throw on null where {@code ==} does not;</li>
 * <li>bodies that declare variables, contain comments in the chain or use an unlabelled {@code break} are left alone.</li>
 * </ul>
 * The result is an old style statement switch, so it compiles at any source level. Arrow switches, pattern switches
 * and switches over classes need Java 14 to 21, which the Yoko build does not yet use.
 */
public class YokoSwitch extends Recipe {
    private static final int MIN_TESTS = 3;

    @Override
    public String getDisplayName() {
        return "Yoko: use switch for chains of constant comparisons";
    }

    @Override
    public String getDescription() {
        return "Replace if / else-if chains that compare one variable with constants by a switch statement, when that is guaranteed to behave identically.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles("**/*.java"), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitIf(J.If iff, ExecutionContext ctx) {
                // only convert from the head of a chain
                if (getCursor().getParentTreeCursor().getValue() instanceof J.If.Else) return super.visitIf(iff, ctx);
                // inner statements first, so that nested chains are converted too
                J.If visited = (J.If) super.visitIf(iff, ctx);
                J converted = convert(visited);
                return converted == null ? visited : converted;
            }
        });
    }

    /** One test of the chain: a branch is taken when the subject equals any of its constants. */
    private static final class Branch {
        final List<Expression> labels = new ArrayList<>();
        Statement body; // null for the final else, which is the default
    }

    private static J convert(J.If head) {
        List<Branch> branches = new ArrayList<>();
        Expression[] subject = {null};
        Kind[] kind = {null};
        J.If.Else lastElse = null;
        Statement defaultBody = null;
        int tests = 0;
        for (J.If cur = head; cur != null; ) {
            Branch b = new Branch();
            if (!collect(cur.getIfCondition().getTree(), b, subject, kind)) return null;
            b.body = cur.getThenPart();
            tests += b.labels.size();
            branches.add(b);
            J.If.Else e = cur.getElsePart();
            if (e == null) break;
            if (!e.getPrefix().getComments().isEmpty()) return null;
            if (!e.getBody().getPrefix().getComments().isEmpty()) return null;
            if (e.getBody() instanceof J.If) {
                cur = (J.If) e.getBody();
            } else {
                defaultBody = e.getBody();
                break;
            }
        }
        // (a single if with several alternatives in one condition reads better than a switch)
        if (tests < MIN_TESTS || branches.size() < 2 || subject[0] == null) return null;
        if (!head.getPrefix().getLastWhitespace().contains("\n")) return null;

        // The labels must be distinct, or the switch would not compile
        Set<String> seen = new HashSet<>();
        for (Branch b : branches) for (Expression l : b.labels) if (!seen.add(label(l))) return null;

        String indent = head.getPrefix().getIndent();
        String caseIndent = "\n" + indent;
        String bodyIndent = "\n" + indent + "    ";

        List<JRightPadded<Statement>> cases = new ArrayList<>();
        for (Branch b : branches) {
            List<JRightPadded<Statement>> body = bodyOf(b.body, bodyIndent);
            if (body == null) return null;
            for (int i = 0; i < b.labels.size(); i++) {
                boolean last = i == b.labels.size() - 1;
                cases.add(JRightPadded.build(newCase(caseIndent, b.labels.get(i).withPrefix(Space.format(" ")), last ? body : Collections.emptyList())));
            }
        }
        if (defaultBody != null) {
            List<JRightPadded<Statement>> body = bodyOf(defaultBody, bodyIndent);
            if (body == null) return null;
            Expression dflt = new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, Collections.emptyList(), "default", null, null);
            cases.add(JRightPadded.build(newCase(caseIndent, dflt, body)));
        }

        J.Block block = new J.Block(randomId(), Space.format(" "), Markers.EMPTY, JRightPadded.build(false), cases, Space.format(caseIndent));
        J.ControlParentheses<Expression> selector = new J.ControlParentheses<>(randomId(), Space.format(" "), Markers.EMPTY,
                JRightPadded.build(subject[0].withPrefix(Space.EMPTY)));
        return new J.Switch(randomId(), head.getPrefix(), head.getMarkers(), selector, block);
    }

    private static J.Case newCase(String prefix, Expression label, List<JRightPadded<Statement>> body) {
        return new J.Case(randomId(), Space.format(prefix), Markers.EMPTY, J.Case.Type.Statement,
                JContainer.build(Space.EMPTY, Collections.singletonList(JRightPadded.build(label)), Markers.EMPTY),
                JContainer.build(Space.EMPTY, body, Markers.EMPTY), null);
    }

    /** The statements of a branch, followed by a break unless they can't complete. Null if the body can't be moved safely. */
    private static List<JRightPadded<Statement>> bodyOf(Statement body, String bodyIndent) {
        List<JRightPadded<Statement>> result = new ArrayList<>();
        if (body instanceof J.Block) {
            J.Block block = (J.Block) body;
            if (!block.getEnd().getComments().isEmpty()) return null;
            for (JRightPadded<Statement> s : block.getPadding().getStatements()) {
                if (s.getElement() instanceof J.VariableDeclarations || s.getElement() instanceof J.ClassDeclaration) return null;
                result.add(s);
            }
        } else {
            if (body instanceof J.VariableDeclarations) return null;
            result.add(JRightPadded.build(body.withPrefix(Space.format(bodyIndent))));
        }
        if (hasBreakForSwitch(result)) return null;
        if (result.isEmpty() || completesNormally(result.get(result.size() - 1).getElement())) {
            result.add(JRightPadded.build((Statement) new J.Break(randomId(), Space.format(bodyIndent), Markers.EMPTY, null)));
        }
        return result;
    }

    private static boolean completesNormally(Statement s) {
        return !(s instanceof J.Return || s instanceof J.Throw || s instanceof J.Continue || s instanceof J.Break);
    }

    /** Would a break in the body, which now ends the loop, end the switch instead? */
    private static boolean hasBreakForSwitch(List<JRightPadded<Statement>> body) {
        boolean[] found = {false};
        for (JRightPadded<Statement> s : body) {
            new JavaIsoVisitor<Integer>() {
                @Override
                public J.Break visitBreak(J.Break b, Integer p) {
                    found[0] = true; // (any break, labelled or not: the cost of being cautious is small)
                    return b;
                }

                @Override
                public J.Lambda visitLambda(J.Lambda l, Integer p) {
                    return l;
                }

                @Override
                public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration c, Integer p) {
                    return c;
                }

                @Override
                public J.ForLoop visitForLoop(J.ForLoop l, Integer p) {
                    return l;
                }

                @Override
                public J.ForEachLoop visitForEachLoop(J.ForEachLoop l, Integer p) {
                    return l;
                }

                @Override
                public J.WhileLoop visitWhileLoop(J.WhileLoop l, Integer p) {
                    return l;
                }

                @Override
                public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop l, Integer p) {
                    return l;
                }

                @Override
                public J.Switch visitSwitch(J.Switch sw, Integer p) {
                    return sw;
                }
            }.visit(s.getElement(), 0);
        }
        return found[0];
    }

    private enum Kind { STRING, ENUM, NUMBER }

    /** Adds the labels of this condition to the branch; false if the condition is not a test of the (common) subject against constants. */
    private static boolean collect(Expression cond, Branch b, Expression[] subject, Kind[] kind) {
        if (cond instanceof J.Binary && ((J.Binary) cond).getOperator() == J.Binary.Type.Or) {
            J.Binary or = (J.Binary) cond;
            return collect(or.getLeft(), b, subject, kind) && collect(or.getRight(), b, subject, kind);
        }
        Expression x;
        Expression label;
        Kind k;
        if (cond instanceof J.MethodInvocation) {
            J.MethodInvocation m = (J.MethodInvocation) cond;
            if (!m.getSimpleName().equals("equals") || m.getSelect() == null || m.getArguments().size() != 1) return false;
            x = m.getSelect();
            Expression arg = m.getArguments().get(0);
            if (arg instanceof J.Literal && ((J.Literal) arg).getValue() instanceof String && TypeUtils.isString(x.getType())) {
                k = Kind.STRING;
                label = arg;
            } else if (arg instanceof J.FieldAccess && isEnum(arg.getType()) && TypeUtils.isOfType(x.getType(), arg.getType())) {
                k = Kind.ENUM;
                J.FieldAccess fa = (J.FieldAccess) arg;
                label = fa.getName().withPrefix(Space.EMPTY);
            } else return false;
        } else if (cond instanceof J.Binary && ((J.Binary) cond).getOperator() == J.Binary.Type.Equal) {
            J.Binary eq = (J.Binary) cond;
            x = eq.getLeft();
            if (!(eq.getRight() instanceof J.Literal) || !isSwitchablePrimitive(x.getType())) return false;
            J.Literal lit = (J.Literal) eq.getRight();
            if (lit.getValue() == null || lit.getValue() instanceof Boolean || lit.getValue() instanceof String) return false;
            k = Kind.NUMBER;
            label = lit;
        } else return false;

        if (!isSimpleSubject(x)) return false;
        if (subject[0] == null) {
            subject[0] = x;
            kind[0] = k;
        } else if (kind[0] != k || !SemanticallyEqual.areEqual(subject[0], x)) {
            return false;
        }
        b.labels.add(label);
        return true;
    }

    private static boolean isEnum(JavaType t) {
        return t instanceof JavaType.FullyQualified && ((JavaType.FullyQualified) t).getKind() == JavaType.FullyQualified.Kind.Enum;
    }

    private static boolean isSwitchablePrimitive(JavaType t) {
        return t == JavaType.Primitive.Char || t == JavaType.Primitive.Int || t == JavaType.Primitive.Short || t == JavaType.Primitive.Byte;
    }

    /** Evaluating it repeatedly or once makes no difference: a variable, a field, or an element of an array variable. */
    private static boolean isSimpleSubject(Expression x) {
        if (x instanceof J.Identifier) return true;
        if (x instanceof J.FieldAccess) return isSimpleSubject(((J.FieldAccess) x).getTarget());
        if (x instanceof J.ArrayAccess) {
            J.ArrayAccess a = (J.ArrayAccess) x;
            return isSimpleSubject(a.getIndexed()) && (a.getDimension().getIndex() instanceof J.Identifier || a.getDimension().getIndex() instanceof J.Literal);
        }
        return false;
    }

    private static String label(Expression e) {
        if (e instanceof J.Literal) {
            J.Literal l = (J.Literal) e;
            // 'a' and 97 are the same label
            Object v = l.getValue();
            return v instanceof Character ? "n" + (int) (Character) v : v instanceof Number ? "n" + ((Number) v).longValue() : "s" + l.getValueSource();
        }
        return "e" + (e instanceof J.Identifier ? ((J.Identifier) e).getSimpleName() : e.toString());
    }
}
