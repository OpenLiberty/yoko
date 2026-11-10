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

class YokoImportsTest implements RewriteTest {
    private static final String CORBA_ORB = "package org.omg.CORBA; public abstract class ORB {}";
    private static final String CORBA_2_3_ORB = "package org.omg.CORBA_2_3; public abstract class ORB extends org.omg.CORBA.ORB {}";
    private static final String CORBA_2_4_ORB = "package org.omg.CORBA_2_4; public abstract class ORB extends org.omg.CORBA_2_3.ORB {}";
    private static final String CORBA_OBJECT = "package org.omg.CORBA; public interface Object {}";
    private static final String CORBA_ANY = "package org.omg.CORBA; public abstract class Any {}";
    private static final String CORBA_STATUS = "package org.omg.CORBA; public final class CompletionStatus { public static final int COMPLETED_NO = 1; public static final int COMPLETED_YES = 0; }";
    private static final String YOKO_ORB = "package org.apache.yoko.orb.CORBA; public class ORB {}";
    private static final String OTHER_DATE = "package com.example; public class Date {}";
    private static final String OTHER_LIST = "package com.example; public class List {}";

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new YokoImports()).parser(JavaParser.fromJavaVersion().dependsOn(
                CORBA_ORB, CORBA_2_3_ORB, CORBA_2_4_ORB, CORBA_OBJECT, CORBA_ANY, CORBA_STATUS, YOKO_ORB, OTHER_DATE, OTHER_LIST));
    }

    @Test
    void layoutsAndSortsImports() {
        rewriteRun(java(
                "package p;\n\nimport static java.util.Collections.emptyList;\nimport java.util.List;\nimport org.omg.CORBA.Any;\nimport javax.swing.JPanel;\nimport java.io.File;\nimport com.example.Date;\n\n"
                        + "class A {\n    Date d; Any a; JPanel j; File f; List<String> l = emptyList();\n}\n",
                "package p;\n\nimport com.example.Date;\nimport org.omg.CORBA.Any;\n\nimport javax.swing.JPanel;\nimport java.io.File;\nimport java.util.List;\n\nimport static java.util.Collections.emptyList;\n\n"
                        + "class A {\n    Date d; Any a; JPanel j; File f; List<String> l = emptyList();\n}\n"));
    }

    @Test
    void alreadyCorrectIsUntouched() {
        rewriteRun(java(
                "package p;\n\nimport org.omg.CORBA.Any;\n\nimport java.util.List;\n\nimport static java.util.Collections.emptyList;\n\n"
                        + "class A {\n    Any a; List<String> l = emptyList();\n}\n"));
    }

    @Test
    void removesUnusedImports() {
        rewriteRun(java(
                "package p;\n\nimport java.util.List;\nimport java.util.Map;\n\nclass A {\n    Map<String, String> m;\n}\n",
                "package p;\n\nimport java.util.Map;\n\nclass A {\n    Map<String, String> m;\n}\n"));
    }

    @Test
    void keepsImportsMentionedOnlyInJavadoc() {
        rewriteRun(java(
                "package p;\n\nimport java.util.List;\n\n/** See {@link List}. */\nclass A {\n}\n"));
    }

    @Test
    void expandsStarImports() {
        rewriteRun(java(
                "package p;\n\nimport org.omg.CORBA.*;\n\nclass A {\n    Any a;\n}\n",
                "package p;\n\nimport org.omg.CORBA.Any;\n\nclass A {\n    Any a;\n}\n"));
    }

    @Test
    void expandsStaticStarImports() {
        rewriteRun(java(
                "package p;\n\nimport static org.omg.CORBA.CompletionStatus.*;\n\nclass A {\n    int a = COMPLETED_NO;\n}\n",
                "package p;\n\nimport static org.omg.CORBA.CompletionStatus.COMPLETED_NO;\n\nclass A {\n    int a = COMPLETED_NO;\n}\n"));
    }

    @Test
    void javaBeatsOtherOnClash() {
        rewriteRun(java(
                "package p;\n\nimport com.example.Date;\n\nclass A {\n    Date a; java.util.Date b;\n}\n",
                "package p;\n\nimport java.util.Date;\n\nclass A {\n    com.example.Date a; Date b;\n}\n"));
    }

    @Test
    void javaBeatsOmgBeatsOtherBeatsYoko() {
        rewriteRun(java(
                "package p;\n\nimport org.apache.yoko.orb.CORBA.ORB;\n\nclass A {\n    ORB a; org.omg.CORBA.ORB b;\n}\n",
                "package p;\n\nimport org.omg.CORBA.ORB;\n\nclass A {\n    org.apache.yoko.orb.CORBA.ORB a; ORB b;\n}\n"));
    }

    @Test
    void javaLangObjectBeatsCorbaObject() {
        rewriteRun(java(
                "package p;\n\nimport org.omg.CORBA.Object;\n\nclass A {\n    Object a; java.lang.Object b;\n}\n",
                "package p;\n\nclass A {\n    org.omg.CORBA.Object a; Object b;\n}\n"));
    }

    @Test
    void supertypeWinsWithinTier() {
        rewriteRun(java(
                "package p;\n\nimport org.omg.CORBA_2_4.ORB;\n\nclass A {\n    ORB a; org.omg.CORBA.ORB b;\n}\n",
                "package p;\n\nimport org.omg.CORBA.ORB;\n\nclass A {\n    org.omg.CORBA_2_4.ORB a; ORB b;\n}\n"));
    }

    @Test
    void sameClassQualifiedAndImportedIsShortenedOnlyWhenClashing() {
        // no clash: fully qualified references are left alone
        rewriteRun(java(
                "package p;\n\nimport org.omg.CORBA.Any;\n\nclass A {\n    Any a; org.omg.CORBA.Any b;\n}\n"));
    }

    @Test
    void qualifiedReferenceToWinnerIsShortened() {
        rewriteRun(java(
                "package p;\n\nimport org.omg.CORBA_2_4.ORB;\n\nclass A {\n    org.omg.CORBA.ORB a; ORB b;\n}\n",
                "package p;\n\nimport org.omg.CORBA.ORB;\n\nclass A {\n    ORB a; org.omg.CORBA_2_4.ORB b;\n}\n"));
    }

    @Test
    void declaredClassNeverImported() {
        rewriteRun(java(
                "package p;\n\nimport java.util.List;\n\nclass A {\n    static class List {}\n    List l;\n    java.util.List<String> m;\n}\n",
                "package p;\n\nclass A {\n    static class List {}\n    List l;\n    java.util.List<String> m;\n}\n"));
    }

    @Test
    void doesNotTouchOtherCode() {
        rewriteRun(java(
                "package p;\n\nimport java.util.List;\nimport java.io.File;\n\nclass A {\n\tList<String> l;   // odd   spacing\n  File f;\n}\n",
                "package p;\n\nimport java.io.File;\nimport java.util.List;\n\nclass A {\n\tList<String> l;   // odd   spacing\n  File f;\n}\n"));
    }

    @Test
    void tieKeepsTheExistingImport() {
        rewriteRun(java(
                "package p;\n\nimport com.example.List;\n\nclass A {\n    List a; org.apache.yoko.orb.CORBA.ORB b;\n}\n"));
    }

    @Test
    void keepsImportOfOwnNestedClassUsedInExtendsClause() {
        rewriteRun(java(
                "package p;\n\nimport p.A.Info;\n\nclass A extends java.util.ArrayList<Info> {\n    static class Info {}\n}\n"));
    }

    @Test
    void keepsImportOfRawType() {
        rewriteRun(java(
                "package p;\n\nimport java.lang.reflect.Constructor;\n\nclass A {\n    Object f(Class c) throws Exception {\n        Class[] types = { String.class };\n        Constructor constr = c.getConstructor(types);\n        return constr.newInstance(\"x\");\n    }\n}\n"));
    }

    @Test
    void javaLangObjectNeedsExplicitImportToBeatSamePackageClass() {
        // in package org.omg.CORBA, "Object" means org.omg.CORBA.Object unless java.lang.Object is imported explicitly
        rewriteRun(java(
                "package org.omg.CORBA;\n\ninterface I {\n    void a(java.lang.Object o);\n    void b(Object o);\n}\n",
                "package org.omg.CORBA;\n\nimport java.lang.Object;\n\ninterface I {\n    void a(Object o);\n    void b(org.omg.CORBA.Object o);\n}\n"));
    }

    @Test
    void nestedClassOnlyHidesImportWithinItsOwnScope() {
        rewriteRun(java(
                "package p;\n\nimport java.lang.reflect.Constructor;\n\nclass A {\n    private static class Inner {\n        private interface Constructor {}\n        Constructor c;\n    }\n    Constructor<?> k;\n}\n"));
    }

    @Test
    void corbaObjectIsNeverImportedEvenWithoutAClash() {
        rewriteRun(java(
                "package p;\n\nimport org.omg.CORBA.Object;\n\ninterface I extends Object {\n    Object get();\n}\n",
                "package p;\n\ninterface I extends org.omg.CORBA.Object {\n    org.omg.CORBA.Object get();\n}\n"));
    }

    @Test
    void declaringACorbaObjectIsFine() {
        rewriteRun(
                spec -> spec.recipe(new YokoImports()).parser(JavaParser.fromJavaVersion()),
                java("package org.omg.CORBA;\n\npublic interface Object extends java.io.Serializable {\n}\n"));
    }
}
