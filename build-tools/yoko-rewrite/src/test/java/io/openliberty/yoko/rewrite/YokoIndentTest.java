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

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

/** The examples are modelled on the real layouts found in the Yoko source. */
class YokoIndentTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new YokoIndent()).parser(JavaParser.fromJavaVersion());
    }

    @Test
    void correctSourceIsUntouched() {
        rewriteRun(java(
                "package p;\n\npublic class A {\n    int x;\n\n    void f(int a) {\n        if (a > 0) {\n            x = a;\n        }\n    }\n}\n"));
    }

    @Test
    void fixesBlockIndentation() {
        rewriteRun(java(
                "package p;\n\npublic class A {\n  int x;\n\n  void f(int a) {\n      if (a > 0) {\n   x = a;\n      }\n  }\n}\n",
                "package p;\n\npublic class A {\n    int x;\n\n    void f(int a) {\n        if (a > 0) {\n            x = a;\n        }\n    }\n}\n"));
    }

    @Test
    void expandsTabsUsingEightColumnTabStops() {
        rewriteRun(java(
                "package p;\n\npublic class A {\n\tint x;\n\n\tvoid f() {\n\t\tx = 1;\n\t}\n}\n",
                "package p;\n\npublic class A {\n    int x;\n\n    void f() {\n        x = 1;\n    }\n}\n"));
    }

    @Test
    void preservesAlignedCaseRows() {
        // from IMRIORInterceptor_impl
        rewriteRun(java(
                "package p;\n\nclass A {\n    int f(int s) {\n        switch (s) {\n        case 0:   return 10;\n        case 1:   return 11;\n        case 20:  return 12;\n        default:  return -1;\n        }\n    }\n}\n"));
    }

    @Test
    void movesAlignedRowsAsAWholeWhenTheirBlockIsMoved() {
        rewriteRun(java(
                "package p;\n\nclass A {\n    int f(int s) {\n            switch (s) {\n            case 0:   return 10;\n            case 20:  return 12;\n            default:  return -1;\n            }\n    }\n}\n",
                "package p;\n\nclass A {\n    int f(int s) {\n        switch (s) {\n        case 0:   return 10;\n        case 20:  return 12;\n        default:  return -1;\n        }\n    }\n}\n"));
    }

    @Test
    void caseLabelsAreLevelWithTheSwitchAndBodiesAreIndented() {
        rewriteRun(java(
                "package p;\n\nclass A {\n    int f(int s) {\n        switch (s) {\n            case 0:\n                return 1;\n            default:\n                return 2;\n        }\n    }\n}\n",
                "package p;\n\nclass A {\n    int f(int s) {\n        switch (s) {\n        case 0:\n            return 1;\n        default:\n            return 2;\n        }\n    }\n}\n"));
    }

    @Test
    void preservesOneLineMembersTable() {
        // from LatinCodec and TestLocalObject
        rewriteRun(java(
                "package p;\n\nclass A {\n    private interface Iso2 { String NAME = \"ISO-8859-2\"; }\n    private interface Iso3 { String NAME = \"ISO-8859-3\"; }\n    private interface Iso4 { String NAME = \"ISO-8859-4\"; }\n}\n"));
    }

    @Test
    void preservesSplitGenericDeclarations() {
        // from ServerComms: the type parameters sit on a line of their own
        rewriteRun(java(
                "package p;\n\nclass A {\n    private <T extends Number, U extends Runnable>\n    void f(T t, U u) {\n        t.intValue();\n    }\n}\n"));
    }

    @Test
    void preservesHandAlignedContinuationLines() {
        rewriteRun(java(
                "package p;\n\nclass A {\n    String f(String a,\n             String b) {\n        return a +\n               b;\n    }\n}\n"));
    }

    @Test
    void movesContinuationLinesWithTheirStatement() {
        rewriteRun(java(
                "package p;\n\nclass A {\n  String f(String a,\n           String b) {\n    return a +\n           b;\n  }\n}\n",
                "package p;\n\nclass A {\n    String f(String a,\n             String b) {\n        return a +\n               b;\n    }\n}\n"));
    }

    @Test
    void preservesAlignedTrailingCommentsAndColumns() {
        // from Utf8InvalidTest
        rewriteRun(java(
                "package p;\n\nclass A {\n    int[] bytes = {\n        0b1100_0000,  // 2-byte lead (min)\n        0b1101_1111,  // 2-byte lead (max)\n        0b1110_0000   // 3-byte lead (min)\n    };\n}\n"));
    }

    @Test
    void fixesMisindentedClosingBraceAndComments() {
        rewriteRun(java(
                "package p;\n\nclass A {\n      /* a comment\n         over lines */\n      int x; // trailing\n  }\n",
                "package p;\n\nclass A {\n    /* a comment\n       over lines */\n    int x; // trailing\n}\n"));
    }

    @Test
    void javadocFollowsItsMember() {
        rewriteRun(java(
                "package p;\n\nclass A {\n      /**\n       * Docs.\n       */\n      void f() {}\n}\n",
                "package p;\n\nclass A {\n    /**\n     * Docs.\n     */\n    void f() {}\n}\n"));
    }

    @Test
    void anonymousClassAndLambdaBodies() {
        rewriteRun(java(
                "package p;\n\nclass A {\n    Runnable r = new Runnable() {\n      public void run() {\n         Runnable q = () -> {\n           System.out.println();\n         };\n      }\n    };\n}\n",
                "package p;\n\nclass A {\n    Runnable r = new Runnable() {\n        public void run() {\n            Runnable q = () -> {\n                System.out.println();\n            };\n        }\n    };\n}\n"));
    }

    @Test
    void secondRunChangesNothing() {
        rewriteRun(java(
                "package p;\n\nclass A {\n    void f(int s) {\n        switch (s) {\n        case 0: return;\n        default: break;\n        }\n    }\n}\n"));
    }

    @Test
    void preservesIdlCompilerStyleBraceOnItsOwnLine() {
        // the layout of the generated-looking classes in yoko-core, such as org.apache.yoko.orb.IMR.ActiveStatePOA
        rewriteRun(java(
                "package p;\n\npublic abstract class A\n    extends Object\n    implements Runnable,\n               Cloneable\n{\n    static final String[] ids =\n    {\n        \"IDL:x:1.0\"\n    };\n\n    public String\n    f()\n    {\n        return null;\n    }\n}\n"));
    }

    @Test
    void allmanBracesAreMovedWithTheirOwner() {
        rewriteRun(java(
                "package p;\n\nclass A {\n      void f(int x)\n      {\n          if (x > 0)\n          {\n              x++;\n          }\n          else\n          {\n              x--;\n          }\n      }\n}\n",
                "package p;\n\nclass A {\n    void f(int x)\n    {\n        if (x > 0)\n        {\n            x++;\n        }\n        else\n        {\n            x--;\n        }\n    }\n}\n"));
    }
}
