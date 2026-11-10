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

import io.openliberty.yoko.rewrite.ImportLayout.Entry;
import io.openliberty.yoko.rewrite.ImportPriority.Candidate;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FindSourceFiles;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lays out the imports of a Java source file according to the Yoko conventions, and makes sure that
 * every class is referred to in the way the conventions require. See {@link ImportLayout} for the layout and
 * {@link ImportPriority} for how classes with the same simple name are resolved.
 * <p>
 * This recipe
 * <ul>
 * <li>expands star imports (including static ones) into single-type imports,</li>
 * <li>removes unused and redundant imports,</li>
 * <li>decides which of several classes sharing a simple name is imported and writes the others fully qualified
 *     (and shortens fully qualified references to the class that is imported), and</li>
 * <li>sorts and groups the remaining imports.</li>
 * </ul>
 * Fully qualified references are only shortened when there is a name clash to resolve.
 * Files are left untouched if they contain unresolved types or comments attached to imports,
 * because we cannot safely rewrite those.
 */
public class YokoImports extends Recipe {
    private static final Pattern WORD = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    @Override
    public String getDisplayName() {
        return "Yoko import layout and name resolution";
    }

    @Override
    public String getDescription() {
        return "Expand star imports, remove unused imports, resolve simple name clashes by package priority "
                + "(java, then org.omg, then others, then org.apache.yoko; supertypes first), and lay out imports "
                + "as: other, blank, javax then java, blank, statics.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles("**/*.java"), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                Analysis analysis = new Analysis(cu);
                if (!analysis.isSafeToRewrite()) return cu;
                J rewritten = new Rewriter(analysis).visit(cu, ctx, getCursor().getParentOrThrow());
                if (!(rewritten instanceof J.CompilationUnit)) return cu;
                List<J.Import> imports = analysis.buildImports();
                J.CompilationUnit result = (J.CompilationUnit) rewritten;
                return sameImports(result.getImports(), imports) ? result : result.withImports(imports);
            }
        });
    }

    private static boolean sameImports(List<J.Import> a, List<J.Import> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) if (a.get(i) != b.get(i)) return false;
        return true;
    }

    private static String simpleName(String fqn) {
        return fqn.substring(fqn.lastIndexOf('.') + 1);
    }

    /** The name of the type with '.' rather than '$' between nested classes. */
    private static String name(JavaType.FullyQualified t) {
        return t.getFullyQualifiedName().replace('$', '.');
    }

    private static JavaType.FullyQualified asClass(JavaType t) {
        if (t instanceof JavaType.Parameterized) t = ((JavaType.Parameterized) t).getType();
        return t instanceof JavaType.FullyQualified ? (JavaType.FullyQualified) t : null;
    }

    private static String text(Expression e) {
        if (e instanceof J.FieldAccess) return text(((J.FieldAccess) e).getTarget()) + '.' + ((J.FieldAccess) e).getSimpleName();
        if (e instanceof J.Identifier) return ((J.Identifier) e).getSimpleName();
        return "";
    }

    /** Is this identifier the right hand part of a qualified expression, such as the name in {@code a.name}? */
    private static boolean isQualifiedName(Object parent, J.Identifier id) {
        if (parent instanceof J.FieldAccess) return ((J.FieldAccess) parent).getName() == id;
        if (parent instanceof J.MethodInvocation) {
            J.MethodInvocation m = (J.MethodInvocation) parent;
            return m.getName() == id && m.getSelect() != null;
        }
        if (parent instanceof J.MemberReference) return ((J.MemberReference) parent).getReference() == id;
        return false;
    }

    /** Is this identifier the name being declared by a class declaration, rather than a reference to a type? */
    private static boolean isDeclarationName(Object parent, J.Identifier id) {
        return parent instanceof J.ClassDeclaration && ((J.ClassDeclaration) parent).getName() == id;
    }

    /** Finds out what is used in a file, and decides what ought to be imported. */
    private static final class Analysis extends JavaIsoVisitor<Integer> {
        private final J.CompilationUnit cu;
        private final String pkg;
        /** simple name -> fully qualified name -> candidate */
        private final Map<String, Map<String, Candidate>> byName = new TreeMap<>();
        private final Map<String, JavaType.FullyQualified> types = new HashMap<>();
        private final Set<String> simpleUses = new HashSet<>();
        private final Set<String> fqnUses = new HashSet<>();
        private final Set<String> declared = new HashSet<>();
        private final Set<String> unqualified = new HashSet<>();
        private final Set<String> words = new HashSet<>();
        private final Map<String, Candidate> winners = new HashMap<>();
        private final Set<String> alreadyImported = new HashSet<>();
        private boolean unresolved;

        Analysis(J.CompilationUnit cu) {
            this.cu = cu;
            for (J.Import i : cu.getImports()) if (!i.isStatic()) alreadyImported.add(text(i.getQualid()));
            this.pkg = cu.getPackageDeclaration() == null ? "" : text(cu.getPackageDeclaration().getExpression());
            visit(cu, 0);
            byName.forEach((n, candidates) -> {
                if (candidates.size() > 1 && !declared.contains(n)) winners.put(n, ImportPriority.best(candidates.values()));
            });
        }

        boolean isSafeToRewrite() {
            if (unresolved) return false;
            for (J.Import i : cu.getImports()) if (!i.getPrefix().getComments().isEmpty()) return false;
            return true;
        }

        // ---- collecting information ----

        @Override
        public J.Import visitImport(J.Import import_, Integer p) {
            return import_;
        }

        @Override
        public J.Package visitPackage(J.Package pkg, Integer p) {
            return pkg;
        }

        @Override
        public Space visitSpace(Space space, Space.Location loc, Integer p) {
            for (Comment c : space.getComments()) {
                Matcher m = WORD.matcher(c.printComment(getCursor()));
                while (m.find()) words.add(m.group());
            }
            return space;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration cd, Integer p) {
            declared.add(cd.getSimpleName());
            return super.visitClassDeclaration(cd, p);
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier id, Integer p) {
            if (id.getType() instanceof JavaType.Unknown) unresolved = true;
            if (!isQualifiedName(getCursor().getParentTreeCursor().getValue(), id)) unqualified.add(id.getSimpleName());
            JavaType.FullyQualified t = simpleUseType(id, getCursor());
            if (t != null) record(t, true);
            return id;
        }

        @Override
        public J.FieldAccess visitFieldAccess(J.FieldAccess fa, Integer p) {
            JavaType.FullyQualified t = fqnRefType(fa);
            if (t == null) return super.visitFieldAccess(fa, p);
            record(t, false);
            return fa;
        }

        /** The class this identifier refers to by simple name, or null if it is not a type reference by simple name. */
        JavaType.FullyQualified simpleUseType(J.Identifier id, Cursor cursor) {
            JavaType.FullyQualified t = asClass(id.getType());
            if (t == null || id.getFieldType() != null) return null;
            if (!id.getSimpleName().equals(simpleName(name(t)))) return null;
            Object parent = cursor.getParentTreeCursor().getValue();
            return isQualifiedName(parent, id) || isDeclarationName(parent, id) ? null : t;
        }

        /** The top level class this fully qualified expression refers to, or null if it is not one. */
        JavaType.FullyQualified fqnRefType(J.FieldAccess fa) {
            JavaType.FullyQualified t = asClass(fa.getType());
            if (t == null || fa.getName().getFieldType() != null || t.getOwningClass() != null) return null;
            return fa.getSimpleName().equals(simpleName(name(t))) ? t : null;
        }

        private void record(JavaType.FullyQualified t, boolean simple) {
            String fqn = name(t);
            types.putIfAbsent(fqn, t);
            byName.computeIfAbsent(simpleName(fqn), k -> new TreeMap<>())
                    .computeIfAbsent(fqn, k -> new Candidate(k, supertypes(t, new TreeSet<>()), alreadyImported.contains(k)));
            (simple ? simpleUses : fqnUses).add(fqn);
        }

        private static Set<String> supertypes(JavaType.FullyQualified t, Set<String> out) {
            JavaType.FullyQualified sup = t.getSupertype();
            if (sup != null && out.add(name(sup))) supertypes(sup, out);
            for (JavaType.FullyQualified i : t.getInterfaces()) if (out.add(name(i))) supertypes(i, out);
            return out;
        }

        Candidate winnerFor(String simpleName) {
            return winners.get(simpleName);
        }

        // ---- deciding what to import ----

        /** Is this class visible by its simple name without any import? */
        private boolean isImplicit(String fqn) {
            JavaType.FullyQualified t = types.get(fqn);
            if (t == null || t.getOwningClass() != null) return false;
            if (t.getPackageName().equals(pkg)) return true;
            if (!t.getPackageName().equals("java.lang")) return false;
            // a class in this package would hide the java.lang one
            return byName.get(simpleName(fqn)).keySet().stream().noneMatch(o -> packageOf(o).equals(pkg));
        }

        private static String packageOf(String fqn) {
            int i = fqn.lastIndexOf('.');
            return i < 0 ? "" : fqn.substring(0, i);
        }

        private static void collectStatics(JavaType.FullyQualified t, Set<String> out, Set<String> seen) {
            if (t == null || !seen.add(t.getFullyQualifiedName())) return;
            for (JavaType.Variable v : t.getMembers()) if (v.hasFlags(Flag.Static)) out.add(v.getName());
            for (JavaType.Method m : t.getMethods()) if (m.hasFlags(Flag.Static)) out.add(m.getName());
            collectStatics(t.getSupertype(), out, seen);
            for (JavaType.FullyQualified i : t.getInterfaces()) collectStatics(i, out, seen);
        }

        private static J.Import newImport(String name, boolean isStatic) {
            J.FieldAccess qualid = TypeTree.<J.FieldAccess>build(name).withPrefix(Space.SINGLE_SPACE);
            return new J.Import(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                    new JLeftPadded<>(isStatic ? Space.SINGLE_SPACE : Space.EMPTY, isStatic, Markers.EMPTY), qualid, null);
        }

        /** An import together with its position in the layout. */
        private static final class Item {
            final Entry entry;
            final J.Import imp;

            Item(Entry entry, J.Import imp) {
                this.entry = entry;
                this.imp = imp;
            }
        }

        List<J.Import> buildImports() {
            // what was there originally
            Map<String, J.Import> original = new LinkedHashMap<>();
            for (J.Import i : cu.getImports()) original.putIfAbsent((i.isStatic() ? "static " : "") + text(i.getQualid()), i);

            // what we want: every class used by simple name, apart from ones that are visible anyway
            Map<String, Item> items = new LinkedHashMap<>();
            Set<String> importedSimpleNames = new HashSet<>();
            byName.forEach((n, candidates) -> {
                if (declared.contains(n)) {
                    // A nested class only hides the name within its own scope, so an existing import may still be needed
                    for (Candidate c : candidates.values()) {
                        if (original.containsKey(c.fqn) && simpleUses.contains(c.fqn)) {
                            items.put(c.fqn, new Item(new Entry(c.fqn, false), original.get(c.fqn)));
                            importedSimpleNames.add(n);
                        }
                    }
                    return;
                }
                boolean conflict = winners.containsKey(n);
                Candidate w = conflict ? winners.get(n) : candidates.values().iterator().next();
                if (ImportPriority.isNeverImported(w.fqn)) return;
                if (!simpleUses.contains(w.fqn) && !(conflict && fqnUses.contains(w.fqn))) return;
                if (isImplicit(w.fqn) || !isImportable(w.fqn, original)) return;
                items.put(w.fqn, new Item(new Entry(w.fqn, false), original.getOrDefault(w.fqn, null)));
                importedSimpleNames.add(n);
            });

            // what was there that we should leave alone
            for (Map.Entry<String, J.Import> e : original.entrySet()) {
                J.Import imp = e.getValue();
                String q = text(imp.getQualid());
                String simple = imp.getQualid().getSimpleName();
                if (imp.isStatic()) {
                    String owner = text(imp.getQualid().getTarget());
                    if (!"*".equals(simple)) {
                        if (unqualified.contains(simple) || words.contains(simple)) items.putIfAbsent(e.getKey(), new Item(new Entry(q, true), imp));
                        continue;
                    }
                    JavaType.FullyQualified t = asClass(imp.getQualid().getTarget().getType());
                    if (t == null) { // cannot expand it, so leave it be
                        items.putIfAbsent(e.getKey(), new Item(new Entry(q, true), imp));
                        continue;
                    }
                    Set<String> statics = new TreeSet<>();
                    collectStatics(t, statics, new HashSet<>());
                    for (String s : statics) {
                        String key = "static " + owner + '.' + s;
                        boolean explicitlyImportedElsewhere = original.keySet().stream().anyMatch(k -> k.startsWith("static ") && k.endsWith('.' + s) && !k.endsWith(".*"));
                        if (unqualified.contains(s) && !explicitlyImportedElsewhere) {
                            items.putIfAbsent(key, new Item(new Entry(owner + '.' + s, true), original.get(key)));
                        }
                    }
                } else if (!"*".equals(simple) && !items.containsKey(q) && !ImportPriority.isNeverImported(q)) {
                    boolean typeUnknown = imp.getQualid().getType() == null || imp.getQualid().getType() instanceof JavaType.Unknown;
                    // an identifier with this name that we could not attribute a type to might be a use of this import
                    boolean untypedUse = unqualified.contains(simple) && !byName.containsKey(simple);
                    boolean mentioned = (words.contains(simple) || untypedUse) && !importedSimpleNames.contains(simple);
                    // importing one's own nested class is how it is used outside the class body, e.g. in an extends clause
                    JavaType.FullyQualified imported = asClass(imp.getQualid().getType());
                    boolean ownNested = imported != null && imported.getOwningClass() != null
                            && declared.contains(simpleName(name(imported.getOwningClass())))
                            && (unqualified.contains(simple) || words.contains(simple));
                    if (typeUnknown || mentioned || ownNested) {
                        items.put(q, new Item(new Entry(q, false), imp));
                        importedSimpleNames.add(simple);
                    }
                }
            }

            // lay out
            List<Item> sorted = new ArrayList<>(items.values());
            sorted.sort((a, b) -> ImportLayout.ORDER.compare(a.entry, b.entry));
            String firstPrefix = !cu.getImports().isEmpty() ? cu.getImports().get(0).getPrefix().getWhitespace()
                    : pkg.isEmpty() ? "" : "\n\n";
            List<J.Import> result = new ArrayList<>(sorted.size());
            Item previous = null;
            for (Item item : sorted) {
                String ws = previous == null ? firstPrefix
                        : ImportLayout.block(previous.entry) == ImportLayout.block(item.entry) ? "\n" : "\n\n";
                J.Import imp = item.imp != null ? item.imp : newImport(item.entry.name, item.entry.isStatic);
                if (!imp.getPrefix().getWhitespace().equals(ws) || !imp.getPrefix().getComments().isEmpty()) {
                    imp = imp.withPrefix(Space.format(ws));
                }
                result.add(imp);
                previous = item;
            }
            return Collections.unmodifiableList(result);
        }

        /** Nested classes are only imported if they were imported to begin with, as they may be inherited. */
        private boolean isImportable(String fqn, Map<String, J.Import> original) {
            JavaType.FullyQualified t = types.get(fqn);
            if (t == null || t.getOwningClass() == null) return true;
            String owner = name(t.getOwningClass());
            return original.containsKey(fqn) || original.containsKey(owner + ".*") || original.containsKey("static " + owner + ".*");
        }
    }

    /** Applies the decisions made by the analysis. */
    private static final class Rewriter extends JavaVisitor<ExecutionContext> {
        private final Analysis analysis;

        Rewriter(Analysis analysis) {
            this.analysis = analysis;
        }

        @Override
        public J visitImport(J.Import import_, ExecutionContext ctx) {
            return import_;
        }

        @Override
        public J visitPackage(J.Package pkg, ExecutionContext ctx) {
            return pkg;
        }

        @Override
        public Space visitSpace(Space space, Space.Location loc, ExecutionContext ctx) {
            return space; // leave comments alone
        }

        @Override
        public J visitIdentifier(J.Identifier id, ExecutionContext ctx) {
            JavaType.FullyQualified t = analysis.simpleUseType(id, getCursor());
            if (t == null || !id.getAnnotations().isEmpty()) return id;
            Candidate winner = analysis.winnerFor(id.getSimpleName());
            boolean banned = ImportPriority.isNeverImported(name(t));
            if (!banned && (winner == null || winner.fqn.equals(name(t)))) return id;
            J.FieldAccess qualified = TypeTree.build(name(t));
            return qualified.withPrefix(id.getPrefix()).withType(t);
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fa, ExecutionContext ctx) {
            JavaType.FullyQualified t = analysis.fqnRefType(fa);
            if (t == null) return super.visitFieldAccess(fa, ctx);
            Candidate winner = analysis.winnerFor(fa.getSimpleName());
            if (ImportPriority.isNeverImported(name(t))) return fa;
            if (winner == null || !winner.fqn.equals(name(t))) return fa;
            return new J.Identifier(Tree.randomId(), fa.getPrefix(), fa.getMarkers(), Collections.emptyList(),
                    fa.getSimpleName(), t, null);
        }
    }
}
