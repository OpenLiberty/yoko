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
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fixes the indentation of the structure of a Java file without disturbing hand-made layout.
 * <p>
 * Lines that begin a statement, a member, a case label or a closing brace are indented according to nesting:
 * four spaces per level, and case labels level with their {@code switch}.
 * Every other line (a continuation of a wrapped statement, a line of an aligned table, a line of a comment
 * that belongs to one of those) is moved by exactly the same amount as the line that it belongs to.
 * That means the layout of any hand-aligned continuation is preserved, however unusual it is.
 * <p>
 * Tab characters in indentation are expanded to spaces (with tab stops every eight columns,
 * which is how they were displayed when they were written).
 * Nothing but leading whitespace is touched.
 * Blank lines are left alone.
 */
public class YokoIndent extends Recipe {
    static final int INDENT = 4;
    static final int CASE_INDENT = 0;
    static final int TAB_SIZE = 8;

    @Override
    public String getDisplayName() {
        return "Yoko indentation";
    }

    @Override
    public String getDescription() {
        return "Indent statements, members, case labels and closing braces by nesting depth (4 spaces, case labels "
                + "level with the switch) and carry the same adjustment to the continuation lines, so that "
                + "hand-aligned layouts survive. Tabs become spaces.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles("**/*.java"), new Indenter());
    }

    /** The anchor is the indentation that the lines inside a block are measured from. */
    private static final class Frame {
        final J.Block block;
        final UUID owner;
        final boolean braceOnNewLine;
        Integer anchor;

        Frame(J.Block block, UUID owner, boolean braceOnNewLine) {
            this.block = block;
            this.owner = owner;
            this.braceOnNewLine = braceOnNewLine;
        }
    }

    private static final class Indenter extends JavaIsoVisitor<ExecutionContext> {
        /** The adjustment applied to the line on which each element started. */
        private final Map<UUID, Integer> shifts = new HashMap<>();
        /** The indentation, after correction, of the line on which each element started. */
        private final Map<UUID, Integer> lineIndents = new HashMap<>();
        private final Deque<Frame> frames = new ArrayDeque<>();
        private int lastLineIndent;
        private int lastLineShift;

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            shifts.clear();
            lineIndents.clear();
            frames.clear();
            lastLineIndent = 0;
            lastLineShift = 0;
            return super.visitCompilationUnit(cu, ctx);
        }

        @Override
        public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
            Object parent = getCursor().getParentTreeCursor().getValue();
            UUID owner = parent instanceof J && isStructural(parent) ? ((J) parent).getId() : null;
            frames.push(new Frame(block, owner, hasNewline(block.getPrefix())));
            try {
                return super.visitBlock(block, ctx);
            } finally {
                frames.pop();
            }
        }

        /** Constructs whose body is indented relative to where the construct itself starts. */
        private static boolean isStructural(Object o) {
            return o instanceof J.MethodDeclaration || o instanceof J.ClassDeclaration || o instanceof J.If
                    || o instanceof J.ForLoop || o instanceof J.ForEachLoop || o instanceof J.WhileLoop
                    || o instanceof J.DoWhileLoop || o instanceof J.Try || o instanceof J.Synchronized
                    || o instanceof J.Switch || o instanceof J.Label || o instanceof J.NewClass
                    || o instanceof J.If.Else || o instanceof J.Try.Catch || o instanceof J.Lambda;
        }

        private int anchorFor(Frame frame) {
            Integer owner = frame.owner == null ? null : lineIndents.get(frame.owner);
            return owner != null ? owner : lastLineIndent;
        }

        @Override
        public Space visitSpace(Space space, Space.Location loc, ExecutionContext ctx) {
            Frame top = frames.peek();
            if (top != null && top.anchor == null && space != top.block.getPrefix()) top.anchor = anchorFor(top);

            Object value = getCursor().getValue();
            Integer expected = null; // the required indentation, if this is the start of a statement, etc.
            J element = null;
            if (loc == Space.Location.BLOCK_END) {
                Frame frame = frameOf((J.Block) value);
                expected = frame == null || frame.anchor == null ? lastLineIndent : frame.anchor;
            } else if (value instanceof J && loc != null && isPrefixLocation(loc)) {
                element = (J) value;
                expected = expectedIndent(element);
            }

            int shift = lastLineShift;
            Space result = space;
            if (hasNewline(space)) {
                int original = indentOfLastLine(space);
                if (original < 0) {
                    shift = 0; // the last line starts inside a comment, so we don't understand it
                } else if (expected != null) {
                    shift = expected - original;
                } else {
                    shift = continuationShift();
                }
                result = reindent(space, shift);
                if (original >= 0) {
                    lastLineIndent = Math.max(0, original + shift);
                    lastLineShift = shift;
                }
            }
            if (element != null) record(element, shift);
            return result;
        }

        /** Is this the prefix of the element being visited, as opposed to some other space within it? */
        private boolean isPrefixLocation(Space.Location loc) {
            return loc.name().endsWith("_PREFIX") || loc == Space.Location.ANY;
        }

        private void record(J element, int shift) {
            shifts.put(element.getId(), shift);
            lineIndents.put(element.getId(), lastLineIndent);
        }

        private Frame frameOf(J.Block block) {
            for (Frame f : frames) if (f.block == block) return f;
            return null;
        }

        /** The indentation that this element must have if it begins a line, or null if it is a continuation. */
        private Integer expectedIndent(J element) {
            Object parent = getCursor().getParentTreeCursor().getValue();
            if (parent instanceof J.CompilationUnit) return 0;
            if (element instanceof J.Block && !(parent instanceof J.Block) && parent instanceof J && isStructural(parent)) {
                // A brace on a line of its own lines up with the start of the construct that it opens
                Integer owner = lineIndents.get(((J) parent).getId());
                return owner; // null if we do not know, in which case it is treated as a continuation
            }
            if (parent instanceof J.Block) {
                Frame frame = frameOf((J.Block) parent);
                int anchor = frame == null || frame.anchor == null ? lastLineIndent : frame.anchor;
                return anchor + (element instanceof J.Case ? CASE_INDENT : INDENT);
            }
            if (parent instanceof J.Case && ((J.Case) parent).getStatements().stream().anyMatch(s -> s == element)) {
                Integer caseIndent = lineIndents.get(((J.Case) parent).getId());
                return (caseIndent == null ? lastLineIndent : caseIndent) + INDENT;
            }
            return null;
        }

        /** Continuation lines move with the statement, member, etc. that they belong to. */
        private int continuationShift() {
            for (Cursor c = getCursor(); c != null; c = c.getParent()) {
                Object v = c.getValue();
                if (v instanceof J) {
                    Integer s = shifts.get(((J) v).getId());
                    if (s != null) return s;
                }
            }
            return 0;
        }

        // ---- working with the text of a Space ----

        private static boolean hasNewline(Space space) {
            if (space.getWhitespace().indexOf('\n') >= 0) return true;
            for (Comment c : space.getComments()) {
                if (c.getSuffix().indexOf('\n') >= 0) return true;
                if (c.isMultiline() && c.printComment(new Cursor(null, Cursor.ROOT_VALUE)).indexOf('\n') >= 0) return true;
            }
            return false;
        }

        /** The width of the indentation of the last line start, or -1 if that is inside a comment. */
        private static int indentOfLastLine(Space space) {
            String last = null;
            boolean lastInComment = false;
            if (space.getWhitespace().indexOf('\n') >= 0) last = space.getWhitespace();
            for (Comment c : space.getComments()) {
                if (c.isMultiline() && c.printComment(new Cursor(null, Cursor.ROOT_VALUE)).indexOf('\n') >= 0) {
                    lastInComment = true;
                    last = null;
                }
                if (c.getSuffix().indexOf('\n') >= 0) {
                    lastInComment = false;
                    last = c.getSuffix();
                }
            }
            if (lastInComment || last == null) return -1;
            String line = last.substring(last.lastIndexOf('\n') + 1);
            int end = 0;
            while (end < line.length() && (line.charAt(end) == ' ' || line.charAt(end) == '\t')) end++;
            return width(line.substring(0, end));
        }

        private static int width(String whitespace) {
            int w = 0;
            for (int i = 0; i < whitespace.length(); i++) {
                w = whitespace.charAt(i) == '\t' ? (w / TAB_SIZE + 1) * TAB_SIZE : w + 1;
            }
            return w;
        }

        /** Adjusts the indentation of every line that starts within this text. The first line is not touched. */
        static String shiftLines(String s, int shift) {
            int nl = s.indexOf('\n');
            if (nl < 0) return s;
            StringBuilder sb = new StringBuilder(s.length() + 8).append(s, 0, nl + 1);
            int i = nl + 1;
            while (i <= s.length()) {
                int j = i;
                while (j < s.length() && (s.charAt(j) == ' ' || s.charAt(j) == '\t')) j++;
                boolean blank = j < s.length() && (s.charAt(j) == '\n' || (s.charAt(j) == '\r' && j + 1 < s.length() && s.charAt(j + 1) == '\n'));
                if (blank) {
                    sb.append(s, i, j);
                } else {
                    int w = Math.max(0, width(s.substring(i, j)) + shift);
                    for (int k = 0; k < w; k++) sb.append(' ');
                }
                int next = s.indexOf('\n', j);
                if (next < 0) {
                    sb.append(s, j, s.length());
                    break;
                }
                sb.append(s, j, next + 1);
                i = next + 1;
            }
            return sb.toString();
        }

        private Space reindent(Space space, int shift) {
            boolean changed = false;
            String whitespace = shiftLines(space.getWhitespace(), shift);
            changed |= !whitespace.equals(space.getWhitespace());
            boolean atLineStart = space.getWhitespace().indexOf('\n') >= 0;
            List<Comment> comments = new ArrayList<>(space.getComments().size());
            for (Comment c : space.getComments()) {
                Comment updated = c;
                if (c.isMultiline() && atLineStart) {
                    String printed = c.printComment(getCursor());
                    if (printed.length() >= 4 && printed.indexOf('\n') >= 0) {
                        String inner = printed.substring(2, printed.length() - 2);
                        String shifted = shiftLines(inner, shift);
                        if (!shifted.equals(inner)) {
                            updated = new TextComment(true, shifted, c.getSuffix(), c.getMarkers());
                        }
                    }
                }
                String suffix = shiftLines(updated.getSuffix(), shift);
                if (!suffix.equals(updated.getSuffix())) updated = updated.withSuffix(suffix);
                changed |= updated != c;
                if (c.getSuffix().indexOf('\n') >= 0) atLineStart = true;
                comments.add(updated);
            }
            return changed ? Space.build(whitespace, comments) : space;
        }
    }
}
