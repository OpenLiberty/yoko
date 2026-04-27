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
package testify.iiop.annotation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.platform.commons.support.AnnotationSupport;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableInterceptor.ORBInitializer;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import testify.iiop.annotation.ConfigureOrb.UseWithOrb;
import testify.iiop.annotation.ConfigureOrb.UseWithOrb.InitializerScope;

import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;
import static org.junit.platform.commons.support.ModifierSupport.isPublic;
import static org.junit.platform.commons.support.ModifierSupport.isStatic;
import static testify.streams.Collectors.requireNoMoreThanOne;
import static testify.util.ArrayUtils.concat;
import static testify.util.Predicates.anyOf;

class OrbSteward implements ExtensionContext.Store.CloseableResource {
    private static final Class<?> CONNECTION_HELPER_CLASS;
    private static final Class<?> EXTENDED_CONNECTION_HELPER_CLASS;

    static {
        try {
            CONNECTION_HELPER_CLASS = Class.forName("org.apache.yoko.orb.OCI.IIOP.ConnectionHelper");
            EXTENDED_CONNECTION_HELPER_CLASS = Class.forName("org.apache.yoko.orb.OCI.IIOP.ExtendedConnectionHelper");
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }
    }

    @SuppressWarnings("unused")
    interface NullIiopConnectionHelper {
    }

    public static ORB getClientOrb(ExtensionContext ctx, ConfigureOrb config) { return getOrb(ctx, config, OrbScope.CLIENT); }
    public static ORB getCollocatedOrb(ExtensionContext ctx, ConfigureOrb config) { return getOrb(ctx, config, OrbScope.COLLOCATED); }

    public static String[] getServerArgs(Class<?> testClass, ConfigureOrb config, boolean collocated) {
        return new OrbSteward(config, collocated ? OrbScope.COLLOCATED : OrbScope.SERVER).args(testClass);
    }

    public static Properties getServerProps(Class<?> testClass, ConfigureOrb config, boolean collocated) {
        return new OrbSteward(config, collocated ? OrbScope.COLLOCATED : OrbScope.SERVER).props(testClass);
    }

    private final ConfigureOrb annotation;
    private final OrbScope orbScope;
    private ORB orb;

    private OrbSteward(ConfigureOrb annotation, OrbScope orbScope) {
        this.annotation = annotation;
        this.orbScope = orbScope;
    }

    private OrbSteward(ConfigAndScope key) {
        this(key.config, key.orbScope);
    }

    /**
     * Get the unique ORB for the supplied test context
     */
    private synchronized ORB getOrbInstance(ExtensionContext ctx) {
        if (orb != null) return orb;
        Class<?> testClass = ctx.getRequiredTestClass();
        this.orb = ORB.init(args(testClass), props(testClass));
        return this.orb;
    }

    @Override
    // A CloseableResource stored in a context store is closed automatically when the context goes out of scope.
    // Note this happens *before* the correlated extension callback points (e.g. AfterEachCallback/AfterAllCallback)
    public synchronized void close() {
        if (orb == null) return;
        orb.shutdown(true);
        orb.destroy();
        orb = null;
    }

    private boolean isOrbModifier(Class<?> c) {
        return AnnotationSupport.findAnnotation(c, UseWithOrb.class)
                .map(UseWithOrb::scope)
                .filter(orbScope::matches)
                .isPresent();
    }

    /**
     * Check that the supplied types are known types to be used as ORB extensions
     */
    private static void validateOrbModifierType(Class<?> type) {
        assertTrue(isStatic(type), "Class " + type.getName() + " should be static");
        assertTrue(isPublic(type), "Class " + type.getName() + " should be public");
        // we know about ORB initializers
        if (ORBInitializer.class.isAssignableFrom(type)) return;
        // we also know about ConnectionHelpers
        if (CONNECTION_HELPER_CLASS.isAssignableFrom(type)) return;
        if (EXTENDED_CONNECTION_HELPER_CLASS.isAssignableFrom(type)) return;
        // we don't know about anything else!
        fail("Type " + type + " cannot be used with an ORB");
    }

    /**
     * Compute the ORB arguments
     *
     * @param testClass the test class on which the annotation is specified
     */
    String[] args(Class<?> testClass) {
        return concat(getNestedModifierTypes(testClass)
                        .filter(anyOf(CONNECTION_HELPER_CLASS::isAssignableFrom, EXTENDED_CONNECTION_HELPER_CLASS::isAssignableFrom))
                        .collect(requireNoMoreThanOne("Only one connection helper can be configured but two were supplied: %s, %s"))
                        .map(c -> concat(annotation.args(), "-IIOPconnectionHelper", c.getName()))
                        .orElseGet(annotation::args),
                annotation.nameService().args);
    }

    private Stream<Class<?>> getNestedModifierTypes(Class<?> testClass) {
        return Stream.of(testClass.getClasses())
                .filter(this::isOrbModifier)
                .peek(OrbSteward::validateOrbModifierType);
    }

    /**
     * Compute the orb properties.
     *
     * @param testClass the test class on which the annotation is specified
     */
    Properties props(Class<?> testClass) {
        Properties props = new Properties();
        props.put("org.omg.CORBA.ORBClass", "org.apache.yoko.orb.CORBA.ORB");
        props.put("org.omg.CORBA.ORBSingletonClass", "org.apache.yoko.orb.CORBA.ORBSingleton");
        for (String prop : annotation.props()) {
            if (prop.isEmpty()) continue;
            String[] arr = prop.split("=", 2);
            props.put(arr[0], arr.length < 2 ? "" : arr[1]);
        }
        // add initializer properties for each specified initializer class
        //noinspection unchecked
        getNestedModifierTypes(testClass)
                .filter(ORBInitializer.class::isAssignableFrom)
                .forEachOrdered(initializer -> addORBInitializerProp(props, (Class<? extends ORBInitializer>) initializer));
        // add initializer property for name service if configured
        annotation.nameService().getInitializerClass().ifPresent(c -> addORBInitializerProp(props, c));
        return props;
    }

    private static void addORBInitializerProp(Properties props, Class<? extends ORBInitializer> initializer) {
        String name = ORBInitializer.class.getName() + "Class." + initializer.getName();
        // blow up if this has been specified twice since this suggests a configuration error
        Assertions.assertFalse(props.contains(name), initializer.getName() + " should only be configured in one place");
        props.put(name, "true");
    }

    private static ORB getOrb(ExtensionContext ctx, OrbScope scope) {
        return ctx.getElement()
                .flatMap(e -> findAnnotation(e, ConfigureOrb.class))
                .or(() -> findAnnotation(ctx.getRequiredTestClass(), ConfigureOrb.class))
                .map(anno -> getOrb(ctx, anno, scope))
                .orElseThrow(Error::new); // error in framework, not calling code
    }

    private static ORB getOrb(ExtensionContext ctx, ConfigureOrb anno, OrbScope scope) {
        var key = new ConfigAndScope(anno, scope);
        return ctx.getStore(ExtensionContext.Namespace.create(key)).getOrComputeIfAbsent(key, OrbSteward::new, OrbSteward.class).getOrbInstance(ctx);
    }

    static POA getActivatedRootPoa(ORB orb) {
        try {
            POA rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootPOA.the_POAManager().activate();
            return rootPOA;
        } catch (InvalidName invalidName) {
            throw new ParameterResolutionException("Could not resolve initial reference \"RootPOA\"");
        } catch (AdapterInactive adapterInactive) {
            throw new ParameterResolutionException("Could not activate POA manager for root POA", adapterInactive);
        }
    }

    private final static class ConfigAndScope {
        final ConfigureOrb config;
        final OrbScope orbScope;

        ConfigAndScope(ConfigureOrb config, OrbScope orbScope) {
            this.config = config;
            this.orbScope = orbScope;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ConfigAndScope)) return false;
            ConfigAndScope that = (ConfigAndScope) o;
            return Objects.equals(config, that.config) && orbScope == that.orbScope;
        }

        @Override
        public int hashCode() {
            return Objects.hash(config, orbScope);
        }
    }

    private enum OrbScope {
        CLIENT, SERVER, COLLOCATED;

        boolean matches(InitializerScope scope) {
            switch (this) {
                case CLIENT:
                    return scope.includesClient();
                case SERVER:
                    return scope.includesServer();
                default:
                    return true;
            }
        }
    }
}
