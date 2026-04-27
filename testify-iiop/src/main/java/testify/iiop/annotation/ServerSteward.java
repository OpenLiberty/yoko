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

import org.hamcrest.Matchers;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;
import testify.annotation.runner.AnnotationButler;
import testify.bus.Bus;
import testify.parts.PartRunner;

import javax.rmi.PortableRemoteObject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static javax.rmi.PortableRemoteObject.narrow;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;
import static testify.annotation.runner.PartRunners.requirePartRunner;
import static testify.bus.key.MemberKey.getMemberEvaluationType;
import static testify.iiop.annotation.ConfigureOrb.NameService.NONE;
import static testify.iiop.annotation.ConfigureServer.Separation.COLLOCATED;
import static testify.iiop.annotation.ConfigureServer.Separation.INTER_PROCESS;
import static testify.iiop.annotation.OrbSteward.getServerArgs;
import static testify.iiop.annotation.OrbSteward.getServerProps;
import static testify.util.Assertions.failf;
import static testify.util.Predicates.not;
import static testify.util.Reflect.setStaticField;

class ServerSteward {
    private static final Namespace NAMESPACE = Namespace.create(ServerSteward.class);
    private final List<Field> controlFields;
    private final List<Field> nameServiceFields;
    private final List<Field> nameServiceUrlFields;
    private final List<Field> corbanameUrlFields;
    private final List<Field> clientStubFields;
    private final List<Field> remoteStubFields;
    private final Map<Member, Remote> remoteImplMembers;
    private final List<Method> beforeMethods;
    private final List<Method> afterMethods;
    private final ConfigureServer config;
    private final ExtensionContext context;
    private final ServerComms serverComms;
    private final ServerController serverControl;
    private ServerSteward(ConfigureServer config, ExtensionContext context) {
        this.config = config;
        this.context = context;
        Class<?> testClass = context.getRequiredTestClass();
        this.controlFields = AnnotationButler.forClass(ConfigureServer.Control.class)
                .requireTestAnnotation(ConfigureServer.class)
                .assertPublic()
                .assertStatic()
                .assertFieldTypes(ServerControl.class)
                .filter(anno -> anno.serverName().equals(config.serverName()))
                .recruit()
                .findFields(testClass);
        this.nameServiceFields = AnnotationButler.forClass(ConfigureServer.NameServiceStub.class)
                .requireTestAnnotation(ConfigureServer.class,
                        "the test server must have its name service configured",
                        cfg -> cfg.serverOrb().nameService(),
                        anyOf(Matchers.is(ConfigureOrb.NameService.READ_ONLY), Matchers.is(ConfigureOrb.NameService.READ_WRITE)))
                .assertPublic()
                .assertStatic()
                .assertFieldTypes(NamingContext.class)
                .filter(anno -> anno.serverName().equals(config.serverName()))
                .recruit()
                .findFields(testClass);
        this.nameServiceUrlFields = AnnotationButler.forClass(ConfigureServer.NameServiceUrl.class)
                .requireTestAnnotation(ConfigureServer.class,
                        "the test server must have its name service configured",
                        cfg -> cfg.serverOrb().nameService(),
                        anyOf(Matchers.is(ConfigureOrb.NameService.READ_ONLY), Matchers.is(ConfigureOrb.NameService.READ_WRITE)))
                .assertPublic()
                .assertStatic()
                .assertFieldTypes(String.class)
                .filter(anno -> anno.serverName().equals(config.serverName()))
                .recruit()
                .findFields(testClass);
        this.corbanameUrlFields = AnnotationButler.forClass(ConfigureServer.CorbanameUrl.class)
                .requireTestAnnotation(ConfigureServer.class,
                        "the test server must have its name service configured",
                        cfg -> cfg.serverOrb().nameService(),
                        anyOf(Matchers.is(ConfigureOrb.NameService.READ_ONLY), Matchers.is(ConfigureOrb.NameService.READ_WRITE)))
                .assertPublic()
                .assertStatic()
                .assertFieldTypes(String.class)
                .filter(anno -> anno.serverName().equals(config.serverName()))
                .recruit()
                .findFields(testClass);
        this.clientStubFields = AnnotationButler.forClass(ConfigureServer.ClientStub.class)
                .requireTestAnnotation(ConfigureServer.class)
                .assertPublic()
                .assertStatic()
                .assertFieldTypes(Remote.class)
                .filter(anno -> anno.serverName().equals(config.serverName()))
                .recruit()
                .findFields(testClass);
        this.remoteStubFields = AnnotationButler.forClass(ConfigureServer.RemoteStub.class)
                .requireTestAnnotation(ConfigureServer.class)
                .assertPublic()
                .assertStatic()
                .assertFieldTypes(Remote.class)
                .filter(anno -> anno.serverName().equals(config.serverName()))
                .recruit()
                .findFields(testClass);
        // Create a map with null values and initialize after server startup
        this.remoteImplMembers = new HashMap<>();
        this.remoteImplMembers.putAll(AnnotationButler.forClass(ConfigureServer.RemoteImpl.class)
                .requireTestAnnotation(ConfigureServer.class)
                .assertPublic()
                .assertStatic()
                .assertFinal()
                .assertFieldTypes(Remote.class)
                .filter(anno1 -> anno1.serverName().equals(config.serverName()))
                .recruit()
                .findFieldsAsMap(testClass));
        this.remoteImplMembers.putAll(AnnotationButler.forClass(ConfigureServer.RemoteImpl.class)
                .requireTestAnnotation(ConfigureServer.class)
                .assertPublic()
                .assertStatic()
                .assertParameterTypes(ServerExtension.ParamType.SUPPORTED_TYPES)
                .filter(anno1 -> anno1.serverName().equals(config.serverName()))
                .recruit()
                .findMethodsAsMap(testClass));
        this.beforeMethods = AnnotationButler.forClass(ConfigureServer.BeforeServer.class)
                .requireTestAnnotation(ConfigureServer.class)
                .assertPublic()
                .assertStatic()
                .assertParameterTypes(ServerExtension.ParamType.SUPPORTED_TYPES)
                .filter(anno -> anno.value().equals(config.serverName()))
                .recruit()
                .findMethods(testClass);
        this.afterMethods = AnnotationButler.forClass(ConfigureServer.AfterServer.class)
                .requireTestAnnotation(ConfigureServer.class)
                .assertPublic()
                .assertStatic()
                .assertParameterTypes(ServerExtension.ParamType.SUPPORTED_TYPES)
                .filter(anno -> anno.value().equals(config.serverName()))
                .recruit()
                .findMethods(testClass);
        assertFalse(
                controlFields.isEmpty() &&
                        clientStubFields.isEmpty() &&
                        remoteImplMembers.isEmpty() &&
                        beforeMethods.isEmpty(),
                () -> ""
                + "The @" + ConfigureServer.class.getSimpleName() + " annotation on class " + testClass.getName() + " requires one of the following:"
                + "\n - EITHER the test must annotate a public static method with @" + ConfigureServer.BeforeServer.class.getSimpleName()
                + "\n - OR the test must annotate a public static field with @" + ConfigureServer.Control.class.getSimpleName()
                + "\n - OR the test must annotate a public static field with @" + ConfigureServer.ClientStub.class.getSimpleName()
                + "\n - OR the test must annotate a public static final field with @" + ConfigureServer.RemoteImpl.class.getSimpleName()
        );

        // blow up if jvm args specified unnecessarily
        if (config.separation() != INTER_PROCESS && config.jvmArgs().length > 0)
            throw new Error("The annotation @" + ConfigureServer.class.getSimpleName()
                    + " must not include JVM arguments unless it is configured as " + INTER_PROCESS);

        boolean colloc = isCollocated();
        ConfigureOrb cfg = colloc ? combineClientAndServerOrbConfig() : config.serverOrb();
        final Properties props = getServerProps(context.getRequiredTestClass(), cfg, colloc);
        final String[] args = getServerArgs(context.getRequiredTestClass(), cfg, colloc);
        this.serverComms = new ServerComms(this.config.serverName(), props, args);
        // Don't launch the server yet - defer until beforeAll() so other extensions can configure the PartRunner first

        this.serverControl = new ServerController();
    }

    Bus getBus(ExtensionContext ctx) {
        return requirePartRunner(ctx).bus(config.serverName());
    }

    void beforeAll(ExtensionContext ctx) {
        // Configure the PartRunner now that all extensions have had a chance to configure it
        PartRunner runner = requirePartRunner(ctx);
        // does this part run in a thread or a new process?
        if (this.config.separation() == INTER_PROCESS) runner.useNewJVMWhenForking(this.config.jvmArgs());
        else runner.useNewThreadWhenForking();

        // Launch the server now that the PartRunner is configured
        serverComms.launch(runner);

        populateControlFields(ctx);
        serverControl.start();
    }

    void afterAll(ExtensionContext ctx) {
        serverControl.ensureStopped();
    }

    private void populateControlFields(ExtensionContext ctx) {
        this.controlFields.forEach(f -> setStaticField(f, serverControl));
    }

    private void injectNameService() {
        final String url = serverComms.getNameServiceUrl();
        nameServiceUrlFields.forEach(f -> setStaticField(f, url));
        if (nameServiceFields.isEmpty()) return;
        final ORB clientOrb = getClientOrb();
        org.omg.CORBA.Object o = clientOrb.string_to_object(url);
        NamingContext ctx = NamingContextHelper.narrow(o);
        nameServiceFields.forEach(f -> setStaticField(f, ctx));
    }

    private void instantiateServerObjects() {
        ORB clientOrb = getClientOrb();
        corbanameUrlFields.forEach(f -> {
            String url = serverComms.instantiate(f);
            setStaticField(f, url);
        });
        clientStubFields.forEach(f -> {
            // instantiate the remote field on the server
            String ior = serverComms.instantiate(f);
            // instantiate the stub on the client
            setStaticField(f, narrow(clientOrb.string_to_object(ior), f.getType()));
        });
        remoteImplMembers.entrySet().forEach(e -> {
            Member m = e.getKey();
            String ior = serverComms.exportObject(m);
            Object object = clientOrb.string_to_object(ior);
            Remote stub = (Remote)PortableRemoteObject.narrow(object, getMemberEvaluationType(m));
            e.setValue(stub);
        });
        remoteStubFields.forEach(f -> setStaticField(f, resolveParameter(getMemberEvaluationType(f))));
    }

    private void beforeServer(ExtensionContext ctx) {
        // drive the before methods
        beforeMethods.forEach(serverComms::invoke);
    }

    private void afterServer(ExtensionContext ctx) {
        // drive the after methods
        afterMethods.forEach(serverComms::invoke);
    }

    /**
     * Combines the server and client ORB configurations into a single merged configuration.
     * This method merges properties and arguments from both the server ORB and client ORB
     * configurations, with server configuration taking precedence in case of conflicts.
     */
    ConfigureOrb combineClientAndServerOrbConfig() {
        return new ConfigureOrb() {
            public Class<? extends Annotation> annotationType() { return ConfigureOrb.class; }
            public String[] args() { return configs(ConfigureOrb::args).flatMap(Arrays::stream).toArray(String[]::new); }
            public String[] props() { return configs(ConfigureOrb::props).flatMap(Arrays::stream).toArray(String[]::new); }
            public NameService nameService() { return configs(ConfigureOrb::nameService).filter(not(NONE::equals)).findFirst().orElse(NONE); }
            private <T> Stream<T> configs(Function<ConfigureOrb,T> mapper) { return Stream.of(config.serverOrb(), config.clientOrb()).map(mapper); }
        };
    }

    static ServerSteward getInstance(ExtensionContext context) {
        // only one server per test class (each nested test class gets its own ServerSteward and PartRunner)
        var store = context.getStore(NAMESPACE);
        return context.getElement()
                .flatMap(e -> findAnnotation(e, ConfigureServer.class))
                .or(() -> findAnnotation(context.getRequiredTestClass(), ConfigureServer.class))
                .map(annotation -> store.getOrComputeIfAbsent(annotation, cfg -> new ServerSteward(cfg, context), ServerSteward.class))
                .orElseThrow(Error::new); // if no ServerSteward can be found, this is an error in the framework
    }

    public ORB getClientOrb() {
        if (isCollocated()) return serverComms.getServerOrb().orElseThrow(Error::new);
        // For non-collocated servers, use OrbSteward to create and cache the client ORB
        return OrbSteward.getClientOrb(context, config.clientOrb());
    }

    private boolean isCollocated() { return config.separation() == COLLOCATED; }

    public void beforeEach(ExtensionContext ctx) {
        serverControl.ensureStarted();
    }

    public void beforeTestExecution(ExtensionContext ctx) {
        // TODO       if (config.separation() == INTER_PROCESS) serverComms.beginLogging(TestLogger.getLogStarter(ctx));
    }

    public void afterTestExecution(ExtensionContext ctx) {
        // TODO        if (config.separation() == INTER_PROCESS) serverComms.endLogging(TestLogger.getLogFinisher(ctx));
    }

    public boolean supportsParameter(Class<?> type) {
        if (!Remote.class.isAssignableFrom(type)) return false;
        final List<Member> matches = findMatchingRemoteImplMembers(type);
        switch (matches.size()) {
            case 0:
                throw failf("Cannot find any members of type %s annotated with @%s", type, ConfigureServer.RemoteImpl.class.getSimpleName());
            case 1: return true;
            default:
                throw failf("Found multiple members matching %s annotated with @%s: %s", type, ConfigureServer.RemoteImpl.class.getSimpleName(),
                        matches.stream().map(Member::toString).collect(joining(", ")));
        }
    }

    public Remote resolveParameter(Class<?> type) {
        Member m = findMatchingRemoteImplMembers(type)
                .stream()
                .findFirst()
                .orElseThrow(() -> failf("Could not find any fields matching type %s annotated with @%s", type, ConfigureServer.RemoteImpl.class.getSimpleName()));

        return Optional.of(m)
                .map(remoteImplMembers::get)
                .orElseThrow(() -> failf("Could not find stub object for field of type %s", type));
    }

    private List<Member> findMatchingRemoteImplMembers(Class<?> type) {
        final List<Member> exactMatches = new ArrayList<>();
        final List<Member> inexactMatches = new ArrayList<>();

        remoteImplMembers.keySet().stream()
                .filter(f -> type.isAssignableFrom(getMemberEvaluationType(f)))
                .peek(inexactMatches::add)
                .filter(f -> type.equals(getMemberEvaluationType(f)))
                .forEach(exactMatches::add);
        return exactMatches.isEmpty() ? inexactMatches : exactMatches;
    }

    private class ServerController implements ServerControl  {
        boolean started;

        public synchronized void start() {
            assertFalse(started, "Server should be stopped when ServerControl.start() is invoked");
            serverComms.control(ServerComms.ServerOp.START_SERVER);
            started = true;
            injectNameService();
            instantiateServerObjects();
            beforeServer(context);
        }

        public synchronized void stop() {
            assertTrue(started, "Server should be started when ServerControl.stop() is invoked");
            serverComms.control(ServerComms.ServerOp.STOP_SERVER);
            started = false;
            afterServer(context);
        }

        synchronized void ensureStarted() {
            if (!started) start();
        }

        synchronized void ensureStopped() {
            if (started) stop();
        }
    }
}
