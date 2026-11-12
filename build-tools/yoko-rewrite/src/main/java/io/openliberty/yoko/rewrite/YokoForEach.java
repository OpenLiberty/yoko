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

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FindSourceFiles;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import static org.openrewrite.Tree.randomId;

/**
 * Turns indexed loops into for-each loops, but only when that cannot change what the program does:
 * <pre>
 * for (int i = 0; i &lt; xs.length; i++) { ... xs[i] ... }
 * for (int i = 0; i &lt; xs.size(); i++) { ... xs.get(i) ... }    // xs a local List
 * </pre>
 * Every use of the index must be an element read, the collection must not be used otherwise in the loop,
 * and the elements must not be assigned. If the body starts by declaring a variable to hold the element
 * ({@code T t = xs[i];}) that variable becomes the loop variable. Otherwise the variable is named by singularising
 * the name of the collection, provided that the name is not otherwise in use.
 * Java 8 compatible: the result never uses {@code var}.
 */
public class YokoForEach extends Recipe {
    @Override
    public String getDisplayName() {
        return "Yoko: use for-each loops where possible";
    }

    @Override
    public String getDescription() {
        return "Replace indexed for loops over arrays and local lists with for-each loops, when the index is only used to read elements.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles("**/*.java"), new Loops());
    }

    private static final class Loops extends JavaVisitor<ExecutionContext> {
        @Override
        public J visitForLoop(J.ForLoop loop, ExecutionContext ctx) {
            // inner loops first
            J.ForLoop l = (J.ForLoop) super.visitForLoop(loop, ctx);
            J converted = convert(l, getCursor());
            return converted == null ? l : converted;
        }
    }

    // ---- the analysis ----

    private static final class Candidate {
        String index;
        Expression collection;
        boolean isList;
    }

    /** Returns the equivalent for-each loop, or null if the loop has to stay as it is. */
    static J convert(J.ForLoop loop, Cursor cursor) {
        Candidate c = candidate(loop.getControl());
        if (c == null) return null;
        String xText = text(c.collection);
        if (xText == null) return null;
        if (c.isList && !isLocalList(c.collection)) return null;

        // Find the element reads and everything that could stop them being replaced
        Scan scan = new Scan(c.index, xText, lastSegment(xText), c.isList);
        scan.visit(loop.getBody(), 0, cursor);
        if (scan.reads.isEmpty() || scan.indexUses != scan.reads.size() || scan.collectionUses != scan.reads.size() || scan.blocked) return null;

        // The element variable: either declared by the first statement of the body or invented
        J.VariableDeclarations loopVar;
        J body = loop.getBody();
        J.VariableDeclarations leading = leadingDeclaration(loop.getBody(), scan.reads.get(0), c.index, xText, c.isList, scan);
        String name;
        if (leading != null) {
            loopVar = leading.withPrefix(Space.EMPTY)
                    .withVariables(Collections.singletonList(leading.getVariables().get(0).withInitializer(null)));
            name = leading.getVariables().get(0).getSimpleName();
            J.Block b = (J.Block) loop.getBody();
            List<JRightPadded<Statement>> rest = new ArrayList<>(b.getPadding().getStatements());
            rest.remove(0);
            body = b.getPadding().withStatements(rest);
        } else {
            name = elementName(xText);
            if (name == null || inUse(name, loop, cursor)) return null;
            TypeTree type = elementType(c, scan.reads.get(0), loop, cursor);
            if (type == null) return null;
            JavaType elemType = scan.reads.get(0).getType();
            loopVar = new J.VariableDeclarations(randomId(), Space.EMPTY, Markers.EMPTY, Collections.emptyList(), Collections.emptyList(),
                    type.withPrefix(Space.EMPTY), null, Collections.emptyList(),
                    Collections.singletonList(JRightPadded.build(new J.VariableDeclarations.NamedVariable(randomId(), Space.format(" "), Markers.EMPTY,
                            new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, Collections.emptyList(), name, elemType, null),
                            Collections.emptyList(), null, null))));
        }

        // Now replace the element reads by the loop variable
        final String varName = name;
        final J.VariableDeclarations.NamedVariable named = loopVar.getVariables().get(0);
        body = new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitArrayAccess(J.ArrayAccess a, ExecutionContext ctx) {
                return scan.reads.contains(a) ? ident(a.getPrefix(), varName, named, a.getType()) : super.visitArrayAccess(a, ctx);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation m, ExecutionContext ctx) {
                return scan.reads.contains(m) ? ident(m.getPrefix(), varName, named, m.getType()) : super.visitMethodInvocation(m, ctx);
            }
        }.visit(body, new org.openrewrite.InMemoryExecutionContext());

        J.ForEachLoop.Control control = new J.ForEachLoop.Control(randomId(), loop.getControl().getPrefix(), Markers.EMPTY,
                new JRightPadded<>(loopVar, Space.format(" "), Markers.EMPTY),
                JRightPadded.build(c.collection.withPrefix(Space.format(" "))));
        return new J.ForEachLoop(randomId(), loop.getPrefix(), loop.getMarkers(), control, loop.getPadding().getBody().withElement((Statement) body));
    }

    private static J.Identifier ident(Space prefix, String name, J.VariableDeclarations.NamedVariable decl, JavaType type) {
        return new J.Identifier(randomId(), prefix, Markers.EMPTY, Collections.emptyList(), name, type, null);
    }

    /** Matches {@code for (int i = 0; i < xs.length|xs.size(); i++)} */
    private static Candidate candidate(J.ForLoop.Control control) {
        if (control.getInit().size() != 1 || control.getUpdate().size() != 1) return null;
        if (!(control.getInit().get(0) instanceof J.VariableDeclarations)) return null;
        J.VariableDeclarations init = (J.VariableDeclarations) control.getInit().get(0);
        if (init.getVariables().size() != 1 || !(init.getTypeExpression() instanceof J.Primitive)) return null;
        if (((J.Primitive) init.getTypeExpression()).getType() != JavaType.Primitive.Int) return null;
        J.VariableDeclarations.NamedVariable v = init.getVariables().get(0);
        if (!(v.getInitializer() instanceof J.Literal) || !Integer.valueOf(0).equals(((J.Literal) v.getInitializer()).getValue())) return null;
        String i = v.getSimpleName();

        if (!(control.getUpdate().get(0) instanceof J.Unary)) return null;
        J.Unary u = (J.Unary) control.getUpdate().get(0);
        if (u.getOperator() != J.Unary.Type.PostIncrement && u.getOperator() != J.Unary.Type.PreIncrement) return null;
        if (!isIdent(u.getExpression(), i)) return null;

        if (!(control.getCondition() instanceof J.Binary)) return null;
        J.Binary cond = (J.Binary) control.getCondition();
        if (cond.getOperator() != J.Binary.Type.LessThan || !isIdent(cond.getLeft(), i)) return null;

        Candidate c = new Candidate();
        c.index = i;
        Expression r = cond.getRight();
        if (r instanceof J.FieldAccess && ((J.FieldAccess) r).getSimpleName().equals("length")) {
            J.FieldAccess fa = (J.FieldAccess) r;
            if (!(fa.getTarget().getType() instanceof JavaType.Array)) return null;
            c.collection = fa.getTarget();
        } else if (r instanceof J.MethodInvocation) {
            J.MethodInvocation m = (J.MethodInvocation) r;
            if (!m.getSimpleName().equals("size") || !m.getArguments().stream().allMatch(a -> a instanceof J.Empty) || m.getSelect() == null) return null;
            if (!TypeUtils.isAssignableTo("java.util.List", m.getSelect().getType())) return null;
            c.collection = m.getSelect();
            c.isList = true;
        } else return null;
        return c;
    }

    private static boolean isIdent(Expression e, String name) {
        return e instanceof J.Identifier && ((J.Identifier) e).getSimpleName().equals(name);
    }

    /** A chain of field names, such as {@code a}, {@code this.a} or {@code a.b.c}. Anything else could have side effects. */
    private static String text(Expression e) {
        if (e instanceof J.Identifier) return ((J.Identifier) e).getSimpleName();
        if (e instanceof J.FieldAccess) {
            String t = text(((J.FieldAccess) e).getTarget());
            return t == null ? null : t + '.' + ((J.FieldAccess) e).getSimpleName();
        }
        return null;
    }

    private static String lastSegment(String text) {
        return text.substring(text.lastIndexOf('.') + 1);
    }

    /** Lists are only converted if they are variables of the method, so nobody else can see them to change them during the loop. */
    private static boolean isLocalList(Expression x) {
        return x instanceof J.Identifier && ((J.Identifier) x).getFieldType() != null && ((J.Identifier) x).getFieldType().getOwner() instanceof JavaType.Method;
    }

    /** Collects what the loop body does with the index and the collection. */
    private static final class Scan extends JavaIsoVisitor<Integer> {
        final String index, xText, xLast;
        final boolean isList;
        final List<Expression> reads = new ArrayList<>();
        int indexUses, collectionUses;
        boolean blocked;

        Scan(String index, String xText, String xLast, boolean isList) {
            this.index = index;
            this.xText = xText;
            this.xLast = xLast;
            this.isList = isList;
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier id, Integer p) {
            if (id.getSimpleName().equals(index)) indexUses++;
            if (id.getSimpleName().equals(xLast)) collectionUses++;
            return id;
        }

        @Override
        public J.ArrayAccess visitArrayAccess(J.ArrayAccess a, Integer p) {
            if (!isList && isIdent(a.getDimension().getIndex(), index) && xText.equals(text(a.getIndexed()))) recordRead(a);
            return super.visitArrayAccess(a, p);
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation m, Integer p) {
            if (isList && m.getSimpleName().equals("get") && m.getSelect() != null && xText.equals(text(m.getSelect()))
                    && m.getArguments().size() == 1 && isIdent(m.getArguments().get(0), index)) recordRead(m);
            return super.visitMethodInvocation(m, p);
        }

        private void recordRead(Expression read) {
            reads.add(read);
            Object parent = getCursor().getParentTreeCursor().getValue();
            // the element must only be read, so it can't be assigned to or incremented
            if (parent instanceof J.Assignment && ((J.Assignment) parent).getVariable() == read) blocked = true;
            if (parent instanceof J.AssignmentOperation && ((J.AssignmentOperation) parent).getVariable() == read) blocked = true;
            if (parent instanceof J.Unary && ((J.Unary) parent).getOperator().isModifying()) blocked = true;
        }

        @Override
        public J.Assignment visitAssignment(J.Assignment a, Integer p) {
            if (isIdent(a.getVariable(), xLast) || isIdent(a.getVariable(), index)) blocked = true;
            return super.visitAssignment(a, p);
        }

        @Override
        public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation a, Integer p) {
            if (isIdent(a.getVariable(), xLast) || isIdent(a.getVariable(), index)) blocked = true;
            return super.visitAssignmentOperation(a, p);
        }

        @Override
        public J.Unary visitUnary(J.Unary u, Integer p) {
            if (u.getOperator().isModifying() && (isIdent(u.getExpression(), xLast) || isIdent(u.getExpression(), index))) blocked = true;
            return super.visitUnary(u, p);
        }
    }

    /** {@code T t = xs[i];} as the first statement of the body, where t is not otherwise tied up. */
    private static J.VariableDeclarations leadingDeclaration(J body, Expression firstRead, String index, String xText, boolean isList, Scan scan) {
        if (!(body instanceof J.Block)) return null;
        List<Statement> stmts = ((J.Block) body).getStatements();
        if (stmts.isEmpty() || !(stmts.get(0) instanceof J.VariableDeclarations)) return null;
        J.VariableDeclarations d = (J.VariableDeclarations) stmts.get(0);
        if (d.getVariables().size() != 1 || !d.getVariables().get(0).getDimensionsAfterName().isEmpty() || !d.getLeadingAnnotations().isEmpty()) return null;
        if (d.getTypeExpression() == null || d.getVarargs() != null) return null;
        J.VariableDeclarations.NamedVariable v = d.getVariables().get(0);
        if (!scan.reads.contains(v.getInitializer())) return null;
        // `var` is no good, and a modifier other than `final` can't be used in a for-each
        if (d.getTypeExpression() instanceof J.Identifier && ((J.Identifier) d.getTypeExpression()).getSimpleName().equals("var")) return null;
        for (J.Modifier m : d.getModifiers()) if (m.getType() != J.Modifier.Type.Final) return null;
        return d;
    }

    /** {@code policies} -&gt; {@code policy}, {@code labels_} -&gt; {@code label}. */
    private static String elementName(String xText) {
        String n = lastSegment(xText);
        // (IDL style names have leading and trailing underscores; "_" alone is not a legal name since Java 9)
        while (n.startsWith("_")) n = n.substring(1);
        while (n.endsWith("_")) n = n.substring(0, n.length() - 1);
        if (n.isEmpty()) return null;
        if (n.endsWith("ies") && n.length() > 3) n = n.substring(0, n.length() - 3) + "y";
        else if (n.length() > 4 && (n.endsWith("sses") || n.endsWith("xes") || n.endsWith("ches") || n.endsWith("shes"))) n = n.substring(0, n.length() - 2);
        else if (n.endsWith("s") && !n.endsWith("ss") && n.length() > 1) n = n.substring(0, n.length() - 1);
        else return null; // no obvious name: leave the loop for a human
        if (n.equals("class")) return "clazz";
        return n.isEmpty() || !Character.isJavaIdentifierStart(n.charAt(0)) || isKeyword(n) ? null : n;
    }

    private static boolean isKeyword(String n) {
        switch (n) {
        case "class": case "int": case "long": case "new": case "this": case "case": case "default": case "switch": case "package":
        case "import": case "interface": case "enum": case "byte": case "char": case "short": case "float": case "double": case "boolean":
        case "void": case "do": case "if": case "else": case "for": case "try": case "catch": case "finally": case "throw": case "return":
        case "abstract": case "final": case "static": case "public": case "private": case "protected": case "native": case "super":
        case "synchronized": case "transient": case "volatile": case "instanceof": case "assert": case "goto": case "const": case "while":
        case "break": case "continue": case "extends": case "implements": case "throws": case "strictfp": case "null": case "true": case "false":
            return true;
        default:
            return false;
        }
    }

    /** Is the name already used inside the loop, or declared in a scope that includes the loop? */
    private static boolean inUse(String name, J.ForLoop loop, Cursor cursor) {
        boolean[] used = {false};
        new JavaIsoVisitor<Integer>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier id, Integer p) {
                if (id.getSimpleName().equals(name)) used[0] = true;
                return id;
            }
        }.visit(loop, 0);
        if (used[0]) return true;

        Set<Object> onPath = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        cursor.getPathAsStream().forEach(onPath::add);
        for (java.util.Iterator<Object> it = cursor.getPathAsStream().iterator(); it.hasNext(); ) {
            Object o = it.next();
            if (o instanceof J.Block) {
                for (Statement s : ((J.Block) o).getStatements()) {
                    if (onPath.contains(s)) break;
                    if (declares(s, name)) return true;
                }
            } else if (o instanceof J.Case) {
                for (Statement s : ((J.Case) o).getStatements()) {
                    if (onPath.contains(s)) break;
                    if (declares(s, name)) return true;
                }
            } else if (o instanceof J.MethodDeclaration) {
                for (Statement s : ((J.MethodDeclaration) o).getParameters()) if (declares(s, name)) return true;
            } else if (o instanceof J.Lambda) {
                for (J p : ((J.Lambda) o).getParameters().getParameters()) {
                    if (declares(p, name)) return true;
                    if (p instanceof J.Identifier && ((J.Identifier) p).getSimpleName().equals(name)) return true;
                }
            } else if (o instanceof J.ForLoop) {
                for (Statement s : ((J.ForLoop) o).getControl().getInit()) if (declares(s, name)) return true;
            } else if (o instanceof J.ForEachLoop) {
                if (declares(((J.ForEachLoop) o).getControl().getVariable(), name)) return true;
            } else if (o instanceof J.Try) {
                J.Try t = (J.Try) o;
                if (t.getResources() != null) for (J.Try.Resource r : t.getResources()) if (declares(r.getVariableDeclarations(), name)) return true;
            } else if (o instanceof J.Try.Catch) {
                if (declares(((J.Try.Catch) o).getParameter().getTree(), name)) return true;
            }
        }
        return false;
    }

    private static boolean declares(J j, String name) {
        if (!(j instanceof J.VariableDeclarations)) return false;
        for (J.VariableDeclarations.NamedVariable v : ((J.VariableDeclarations) j).getVariables()) if (v.getSimpleName().equals(name)) return true;
        return false;
    }

    /** The declared type of the elements of the collection, taken from the source declaration so that it prints as written. */
    private static TypeTree elementType(Candidate c, Expression firstRead, J.ForLoop loop, Cursor cursor) {
        String last = lastSegment(text(c.collection));
        J.VariableDeclarations decl = c.collection instanceof J.Identifier ? findDeclaration(last, cursor) : null;
        if (decl != null && decl.getVariables().size() == 1 && decl.getVariables().get(0).getDimensionsAfterName().isEmpty()) {
            TypeTree t = decl.getTypeExpression();
            if (!c.isList && t instanceof J.ArrayType) {
                // (a declaration of a 2D array is not a simple case)
                TypeTree element = ((J.ArrayType) t).getElementType();
                if (!(element instanceof J.ArrayType)) return element;
            } else if (c.isList && t instanceof J.ParameterizedType && isPlainList(((J.ParameterizedType) t).getType())) {
                List<Expression> args = ((J.ParameterizedType) t).getTypeParameters();
                if (args != null && args.size() == 1 && args.get(0) instanceof TypeTree && !(args.get(0) instanceof J.Wildcard)
                        && !(args.get(0) instanceof J.Empty)) return (TypeTree) args.get(0);
            }
            return null;
        }
        // Not declared nearby: only primitives and java.lang classes can be written without worrying about imports
        JavaType type = firstRead.getType();
        if (type instanceof JavaType.Primitive && type != JavaType.Primitive.None && type != JavaType.Primitive.Null && type != JavaType.Primitive.String) {
            return new J.Primitive(randomId(), Space.EMPTY, Markers.EMPTY, (JavaType.Primitive) type);
        }
        if (type instanceof JavaType.FullyQualified) {
            String fqn = ((JavaType.FullyQualified) type).getFullyQualifiedName();
            if (fqn.startsWith("java.lang.") && fqn.indexOf('.', 10) < 0 && !(type instanceof JavaType.Parameterized)) {
                return new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, Collections.emptyList(), fqn.substring(10), type, null);
            }
        }
        return null;
    }

    /** Lists whose single type argument is known to be the type of the elements. */
    private static boolean isPlainList(JavaType t) {
        if (!(t instanceof JavaType.FullyQualified)) return false;
        switch (((JavaType.FullyQualified) t).getFullyQualifiedName()) {
        case "java.util.List": case "java.util.ArrayList": case "java.util.LinkedList": case "java.util.Vector":
            return true;
        default:
            return false;
        }
    }

    private static J.VariableDeclarations findDeclaration(String name, Cursor cursor) {
        Set<Object> onPath = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        cursor.getPathAsStream().forEach(onPath::add);
        for (java.util.Iterator<Object> it = cursor.getPathAsStream().iterator(); it.hasNext(); ) {
            Object o = it.next();
            if (o instanceof J.Block) {
                // locals are only in scope after their declaration, but fields can be declared anywhere in a class body
                boolean classBody = isClassBody(cursor, o);
                for (Statement s : ((J.Block) o).getStatements()) {
                    if (declares(s, name)) return (J.VariableDeclarations) s;
                    if (!classBody && onPath.contains(s)) break;
                }
            } else if (o instanceof J.MethodDeclaration) {
                for (Statement s : ((J.MethodDeclaration) o).getParameters()) if (declares(s, name)) return (J.VariableDeclarations) s;
            }
        }
        return null;
    }

    private static boolean isClassBody(Cursor cursor, Object block) {
        return cursor.getPathAsStream().anyMatch(o -> o instanceof J.ClassDeclaration && ((J.ClassDeclaration) o).getBody() == block);
    }
}
