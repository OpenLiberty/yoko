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
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package testify.annotation.runner;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import testify.annotation.ConfigurePartRunner;
import testify.parts.PartRunner;

import java.util.Optional;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

public enum PartRunners {
    ;
    private static final Namespace NAMESPACE = Namespace.create(PartRunners.class);

    public static PartRunner requirePartRunner(ExtensionContext ctx) {
        return getPartRunner(ctx).orElseThrow(Error::new);
    }

    public static Optional<PartRunner> getPartRunner(ExtensionContext context) {
        return context.getElement()
                // check there is an annotation
                .flatMap(e -> findAnnotation(e, ConfigurePartRunner.class))
                .or(() -> findAnnotation(context.getRequiredTestClass(), ConfigurePartRunner.class))
                // PartRunners are one per test class (not shared across nested test classes)
                // Use the test class context, not the root context, so nested classes get their own PartRunner
                .map(annotation -> context.getStore(NAMESPACE))
                .map(store -> store.getOrComputeIfAbsent(PartRunners.class, PartRunners::create, PartRunner.class));
    }

    private static PartRunner create(Object ignored) { return PartRunner.create(); }
}
