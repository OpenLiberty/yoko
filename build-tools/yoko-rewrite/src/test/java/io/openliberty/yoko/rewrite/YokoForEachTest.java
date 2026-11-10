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

class YokoForEachTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new YokoForEach()).parser(JavaParser.fromJavaVersion());
    }

    private static String in(String body) {
        return "import java.util.List;\n\nclass A {\n    int[] ids;\n\n" + body + "}\n";
    }

    @Test
    void arrayWithDeclaredElement() {
        rewriteRun(java(
                in("    void f(String[] names) {\n        for (int i = 0; i < names.length; i++) {\n            String n = names[i];\n            System.out.println(n);\n        }\n    }\n"),
                in("    void f(String[] names) {\n        for (String n : names) {\n            System.out.println(n);\n        }\n    }\n")));
    }

    @Test
    void arrayElementNamedBySingularising() {
        rewriteRun(java(
                in("    void f(String[] names) {\n        for (int i = 0; i < names.length; i++) {\n            System.out.println(names[i].length() + names[i]);\n        }\n    }\n"),
                in("    void f(String[] names) {\n        for (String name : names) {\n            System.out.println(name.length() + name);\n        }\n    }\n")));
    }

    @Test
    void primitiveArrayField() {
        rewriteRun(java(
                in("    long sum() {\n        long t = 0;\n        for (int i = 0; i < ids.length; i++) t += ids[i];\n        return t;\n    }\n"),
                in("    long sum() {\n        long t = 0;\n        for (int id : ids) t += id;\n        return t;\n    }\n")));
    }

    @Test
    void localList() {
        rewriteRun(java(
                in("    void f(List<String> names) {\n        for (int i = 0; i < names.size(); i++) {\n            System.out.println(names.get(i));\n        }\n    }\n"),
                in("    void f(List<String> names) {\n        for (String name : names) {\n            System.out.println(name);\n        }\n    }\n")));
    }

    @Test
    void indexUsedForOtherThingsIsLeftAlone() {
        rewriteRun(java(in("    void f(String[] names) {\n        for (int i = 0; i < names.length; i++) {\n            System.out.println(i + names[i]);\n        }\n    }\n")));
    }

    @Test
    void parallelArraysAreLeftAlone() {
        rewriteRun(java(in("    void f(String[] names, int[] ages) {\n        for (int i = 0; i < names.length; i++) {\n            System.out.println(names[i] + ages[i]);\n        }\n    }\n")));
    }

    @Test
    void assignedElementsAreLeftAlone() {
        rewriteRun(java(in("    void f(String[] names) {\n        for (int i = 0; i < names.length; i++) {\n            names[i] = names[i].trim();\n        }\n    }\n")));
    }

    @Test
    void listThatIsAFieldIsLeftAlone() {
        rewriteRun(java(in("    List<String> names;\n    void f() {\n        for (int i = 0; i < names.size(); i++) {\n            System.out.println(names.get(i));\n        }\n    }\n")));
    }

    @Test
    void listModifiedInTheLoopIsLeftAlone() {
        rewriteRun(java(in("    void f(List<String> names) {\n        for (int i = 0; i < names.size(); i++) {\n            if (names.get(i).isEmpty()) names.remove(i);\n        }\n    }\n")));
    }

    @Test
    void nameClashIsLeftAlone() {
        rewriteRun(java(in("    void f(String[] names) {\n        String name = \"x\";\n        for (int i = 0; i < names.length; i++) {\n            System.out.println(names[i] + name);\n        }\n    }\n")));
    }

    @Test
    void startsAtNonZeroIsLeftAlone() {
        rewriteRun(java(in("    void f(String[] names) {\n        for (int i = 1; i < names.length; i++) {\n            System.out.println(names[i]);\n        }\n    }\n")));
    }

    @Test
    void underscoreNamesAreCleanedUp() {
        rewriteRun(java(
                in("    void f(String[] _names) {\n        for (int i = 0; i < _names.length; i++) {\n            System.out.println(_names[i]);\n        }\n    }\n"),
                in("    void f(String[] _names) {\n        for (String name : _names) {\n            System.out.println(name);\n        }\n    }\n")));
    }

    @Test
    void nameThatWouldBeJustAnUnderscoreIsLeftAlone() {
        rewriteRun(java(in("    void f(String[] _s) {\n        for (int i = 0; i < _s.length; i++) {\n            System.out.println(_s[i]);\n        }\n    }\n")));
    }

    @Test
    void idlHelperShape() {
        rewriteRun(java(in("    static void write(java.io.PrintStream _out, java.lang.String[] _s) {\n        for (int i = 0; i < _s.length; i++) {\n            _out.print(_s[i]);\n        }\n    }\n")));
    }

    @Test
    void pluralsEndingInEsAreSingularisedProperly() {
        rewriteRun(java(
                in("    void f(Class<?>[] classes) {\n        for (int i = 0; i < classes.length; i++) {\n            System.out.println(classes[i]);\n        }\n    }\n"),
                in("    void f(Class<?>[] classes) {\n        for (Class<?> clazz : classes) {\n            System.out.println(clazz);\n        }\n    }\n")));
    }
}
