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

class YokoSwitchTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new YokoSwitch()).parser(JavaParser.fromJavaVersion());
    }

    private static String in(String body) {
        return "class A {\n    enum E { X, Y, Z }\n    int n;\n\n" + body + "}\n";
    }

    @Test
    void stringChainWithDefault() {
        rewriteRun(java(
                in("    void f(String s) {\n        if (s.equals(\"a\")) {\n            n = 1;\n        } else if (s.equals(\"b\") || s.equals(\"c\")) {\n            n = 2;\n        } else if (s.equals(\"d\")) {\n            n = 3;\n        } else {\n            n = 4;\n        }\n    }\n"),
                in("    void f(String s) {\n        switch (s) {\n        case \"a\":\n            n = 1;\n            break;\n        case \"b\":\n        case \"c\":\n            n = 2;\n            break;\n        case \"d\":\n            n = 3;\n            break;\n        default:\n            n = 4;\n            break;\n        }\n    }\n")));
    }

    @Test
    void enumChainWithReturns() {
        rewriteRun(java(
                in("    int f(E e) {\n        if (e.equals(E.X)) {\n            return 1;\n        } else if (e.equals(E.Y)) {\n            return 2;\n        } else if (e.equals(E.Z)) {\n            return 3;\n        }\n        return 0;\n    }\n"),
                in("    int f(E e) {\n        switch (e) {\n        case X:\n            return 1;\n        case Y:\n            return 2;\n        case Z:\n            return 3;\n        }\n        return 0;\n    }\n")));
    }

    @Test
    void charChain() {
        rewriteRun(java(
                in("    void f(char ch) {\n        if (ch == '.') n = 1;\n        else if (ch == '/') n = 2;\n        else if (ch == '\\\\') n = 3;\n    }\n"),
                in("    void f(char ch) {\n        switch (ch) {\n        case '.':\n            n = 1;\n            break;\n        case '/':\n            n = 2;\n            break;\n        case '\\\\':\n            n = 3;\n            break;\n        }\n    }\n")));
    }

    @Test
    void shortChainsAreLeftAlone() {
        rewriteRun(java(in("    void f(String s) {\n        if (s.equals(\"a\")) n = 1;\n        else if (s.equals(\"b\")) n = 2;\n    }\n")));
    }

    @Test
    void enumIdentityComparisonIsLeftAlone() {
        // == is null safe and switch is not
        rewriteRun(java(in("    void f(E e) {\n        if (e == E.X) n = 1;\n        else if (e == E.Y) n = 2;\n        else if (e == E.Z) n = 3;\n    }\n")));
    }

    @Test
    void differentSubjectsAreLeftAlone() {
        rewriteRun(java(in("    void f(String s, String t) {\n        if (s.equals(\"a\")) n = 1;\n        else if (t.equals(\"b\")) n = 2;\n        else if (s.equals(\"c\")) n = 3;\n    }\n")));
    }

    @Test
    void methodCallSubjectIsLeftAlone() {
        rewriteRun(java(in("    String g() { return \"\"; }\n    void f() {\n        if (g().equals(\"a\")) n = 1;\n        else if (g().equals(\"b\")) n = 2;\n        else if (g().equals(\"c\")) n = 3;\n    }\n")));
    }

    @Test
    void breakInBodyIsLeftAlone() {
        rewriteRun(java(in("    void f(String[] ss) {\n        for (String s : ss) {\n            if (s.equals(\"a\")) { n = 1; }\n            else if (s.equals(\"b\")) { break; }\n            else if (s.equals(\"c\")) { n = 3; }\n        }\n    }\n")));
    }

    @Test
    void declarationsInBodyAreLeftAlone() {
        rewriteRun(java(in("    void f(String s) {\n        if (s.equals(\"a\")) { int k = 1; n = k; }\n        else if (s.equals(\"b\")) { int k = 2; n = k; }\n        else if (s.equals(\"c\")) { n = 3; }\n    }\n")));
    }

    @Test
    void duplicateLabelsAreLeftAlone() {
        rewriteRun(java(in("    void f(String s) {\n        if (s.equals(\"a\")) n = 1;\n        else if (s.equals(\"b\")) n = 2;\n        else if (s.equals(\"a\")) n = 3;\n    }\n")));
    }

    @Test
    void singleIfWithManyAlternativesIsLeftAlone() {
        rewriteRun(java(in("    void f(char ch) {\n        if (ch == '.' || ch == '/' || ch == '\\\\') n = 1;\n    }\n")));
    }
}
