/*
 * Copyright 2024 IBM Corporation and others.
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
package testify.iiop.annotation;

import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.PortableInterceptor.ORBInitializer;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Optional;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static testify.iiop.annotation.ConfigureOrb.NameService.NONE;

@ExtendWith(OrbExtension.class)
@Target({ANNOTATION_TYPE, TYPE})
@Retention(RUNTIME)
@Inherited
public @interface ConfigureOrb {
    enum NameService {
        NONE,
        READ_ONLY("org.apache.yoko.orb.spi.naming.NameServiceInitializer", "-YokoNameServiceRemoteAccess", "readOnly"),
        READ_WRITE("org.apache.yoko.orb.spi.naming.NameServiceInitializer", "-YokoNameServiceRemoteAccess", "readWrite");
        final String[] args;
        private final String initializerClassName;

        NameService() {
            this.args = new String[0];
            this.initializerClassName = null;
        }

        NameService(String initializerClassName, String...args) {
            this.args = args;
            this.initializerClassName = initializerClassName;
        }

        Optional<Class<? extends ORBInitializer>> getInitializerClass() {
            return Optional.ofNullable(initializerClassName).map(c -> {
				try {
					return Class.forName(c);
				} catch (ClassNotFoundException e) {
					Error e2 = new NoClassDefFoundError();
					e2.initCause(e);
					throw e2;
				}
			}).map(ORBInitializer.class.getClass()::cast);
        }
    }

    String value() default "orb";
    String[] args() default "";
    String[] props() default "";
    NameService nameService() default NONE;


    @Target({ANNOTATION_TYPE, TYPE})
    @Retention(RUNTIME)
    @interface UseWithOrb {
        // TODO: maybe set the initializer classes in the ORB config
        // TODO: use enums to identify ORBs
        // TODO: configure differently for @ConfigureServer
        String value() default ".*";
    }
}
