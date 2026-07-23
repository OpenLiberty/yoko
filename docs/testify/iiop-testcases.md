# Writing IIOP testcases with testify-iiop

`testify-iiop` is the quickest way to write Yoko IIOP tests when you need a client ORB talking to a server ORB, possibly in a separate process or even against an older Yoko release.

The main idea is simple:

- declare the server with annotations
- expose one or more remote implementations
- let the framework start the ORBs
- receive the remote stub directly in the test method

For most new tests, this is the right starting point.

## Start with `@ConfigureServer`

Use `@ConfigureServer` when your test needs a server-side object and a client talking to it.

```java
import org.junit.jupiter.api.Test;
import testify.iiop.annotation.ConfigureServer;

import java.rmi.Remote;
import java.rmi.RemoteException;

@ConfigureServer
class EchoTest {
    interface Echo extends Remote {
        String echo(String msg) throws RemoteException;
    }

    @ConfigureServer.RemoteImpl
    public static final Echo IMPL = msg -> msg;

    @Test
    void roundTrip(Echo stub) throws Exception {
        assertEquals("hello", stub.echo("hello"));
    }
}
```

This is the preferred pattern:

1. define a remote interface
2. define an implementation
3. mark the implementation with `@ConfigureServer.RemoteImpl`
4. accept the matching stub type as a test parameter

The framework finds the server-side implementation, exports it, and injects the corresponding client stub into the test method.

If this works for your testcase, stop there. Do not add lower-level plumbing unless the test really needs it.

## What gets injected

### Into test methods

The simplest and most useful injected parameter is the remote stub itself.

```java
@Test
void roundTrip(Echo stub) throws Exception {
    assertEquals("hello", stub.echo("hello"));
}
```

A test method may also request:

- `org.omg.CORBA.ORB` for direct ORB access
- `testify.bus.Bus` for inter-part communication
- `org.omg.PortableServer.POA` for lower-level POA work

In practice, ask for `ORB` only if the test is specifically about URL resolution, stringified objects, naming service lookup, or other lower-level CORBA mechanics.

### Into server-side setup methods

Server lifecycle methods can also receive supported runtime parameters such as:

- `Bus`
- `ORB`
- `POA`

That is useful when the server needs custom registration or setup logic at startup.

## Multiple ways to expose a remote object

### Preferred: `@ConfigureServer.RemoteImpl`

Annotate a static field or static method that supplies the server-side implementation.

Field form:

```java
@ConfigureServer.RemoteImpl
public static final Echo IMPL = msg -> msg;
```

Method form:

```java
@ConfigureServer.RemoteImpl
public static Echo createImpl(ORB orb, POA poa) {
    return msg -> "server:" + msg;
}
```

Use the method form when construction needs server-side runtime objects.

### Alternative: `@ConfigureServer.RemoteStub`

If you want the stub injected into a static field instead of as a test parameter, you can use `@ConfigureServer.RemoteStub`.

For new tests, prefer test method parameter injection because it is clearer and keeps the test local.

### Alternative: `@ConfigureServer.ClientStub`

Use `@ConfigureServer.ClientStub` when you want a static stub field for a specific implementation class.

Again, for most modern testcases, direct test parameter injection is simpler.

## Name service and corbaname tests

Some tests need to verify naming behavior rather than just invoking a remote method. In that case, configure the server ORB with a name service and ask for the generated URLs.

```java
import static testify.iiop.annotation.ConfigureOrb.NameService.READ_WRITE;

@ConfigureServer(serverOrb = @ConfigureOrb(nameService = READ_WRITE))
class NameServiceStyleTest {
    @ConfigureServer.NameServiceUrl
    public static String nameServiceUrl;

    @ConfigureServer.CorbanameUrl(EchoImpl.class)
    public static String stubUrl;

    @Test
    void resolveThroughOrb(ORB clientOrb) throws Exception {
        Object obj = clientOrb.string_to_object(stubUrl);
        Echo stub = Echo.class.cast(javax.rmi.PortableRemoteObject.narrow(obj, Echo.class));
        assertEquals("ok", stub.echo("ok"));
    }
}
```

Use this style only when the URL itself matters to the test.

If you only need to call the remote object, prefer:

```java
@Test
void invoke(Echo stub) throws Exception { ... }
```

instead of resolving it manually through `ORB.string_to_object(...)`.

## Server lifecycle hooks

Use these when the server needs setup or cleanup beyond constructing the implementation.

### `@ConfigureServer.BeforeServer`

Methods annotated with `@ConfigureServer.BeforeServer` run when the server starts.

Typical uses:

- bind or register objects
- initialize server state
- populate the naming service
- perform server-side assertions about startup conditions

Example:

```java
@ConfigureServer.BeforeServer
public static void setUpServer(ORB orb, POA poa) {
    // custom startup logic
}
```

### `@ConfigureServer.AfterServer`

Methods annotated with `@ConfigureServer.AfterServer` run after the tests are complete.

Use them for explicit cleanup when server shutdown alone is not enough.

## Restarting or stopping the server during a test

If the testcase needs to control server lifecycle explicitly, inject `ServerControl` into a static field.

```java
@ConfigureServer.Control
public static ServerControl serverControl;
```

That gives you:

- `serverControl.start()`
- `serverControl.stop()`
- `serverControl.restart()`

This is useful for tests about reconnect behavior, stale references, restart recovery, or server state transitions.

Keep in mind:

- stopping the server should make remote calls fail
- restarting the server should create a fresh server instance
- use this only when server lifecycle is part of the behavior under test

## Choosing the separation mode

`@ConfigureServer` supports different client/server layouts through `separation`.

### Default: `INTER_ORB`

This is the default and the best choice for most tests.

It gives you:

- separate client and server ORBs
- realistic distributed behavior
- less setup overhead than a separate process

Use the default unless the test specifically needs another mode.

### `COLLOCATED`

Use collocation when you explicitly want both sides in the same ORB.

That is less representative of a true distributed case, so do not choose it unless the test needs collocated behavior.

### `INTER_PROCESS`

Use a separate process when you need stronger isolation or versioned interop.

This is the mode automatically selected by `@InteropTest`.

## Use `@ConfigureOrb` only for ORB-only tests

`@ConfigureOrb` is the lower-level option for tests that need an ORB but not the server framework.

Use it when the test is about ORB configuration itself, interceptors, connection helpers, or other ORB behavior that does not need `@ConfigureServer`.

Do **not** combine `@ConfigureOrb` and `@ConfigureServer` on the same test class or method. The framework explicitly rejects that combination.

## Adding ORB customisation

`@ConfigureOrb` supports:

- command-line ORB args via `args`
- ORB properties via `props`
- optional built-in name service support via `nameService`

It also picks up nested public static classes annotated with `@ConfigureOrb.UseWithOrb`, such as ORB initializers or connection helpers.

That makes it possible to keep ORB extensions close to the testcase.

## Interop tests against older Yoko versions

Use `@InteropTest` when the server must run with a specific historical Yoko version.

```java
@InteropTest(InteropTest.YokoVersion.V1_6_1)
class VersionedInteropTest {
    // test body
}
```

`@InteropTest` automatically configures the server to run in a separate process.

Tests may be skipped if the requested version has not been cached locally. To cache a version:

```bash
./gradlew buildYokoVersion -PyokoVersion=X.Y.Z
```

Use `@InteropTest` only when version skew is the point of the testcase. If the test is not about cross-version compatibility, prefer plain `@ConfigureServer`.

## Practical advice for simplifying tests

When simplifying an older testcase, try this sequence:

1. remove manual ORB startup code
2. replace explicit servant export logic with `@ConfigureServer.RemoteImpl`
3. replace static stub setup with a test method parameter where possible
4. keep `ORB` injection only if the test genuinely needs direct CORBA object conversion
5. keep name service URLs only if the test is validating naming behavior
6. move custom startup logic into `@ConfigureServer.BeforeServer`

A good simplified testcase usually reads like a normal JUnit test with a few annotations, not like a framework bootstrap program.

## Pitfalls

### Prefer direct stub injection

If all you need is to call the server object, accept the stub directly:

```java
@Test
void callServer(Echo stub) throws Exception { ... }
```

This is simpler and clearer than manually doing:

- `ORB.string_to_object(...)`
- `PortableRemoteObject.narrow(...)`

Manual resolution is best reserved for naming and URL-specific tests.

### Do not mix `@ConfigureOrb` with `@ConfigureServer`

Choose one model:

- `@ConfigureServer` for client/server tests
- `@ConfigureOrb` for ORB-only tests

Do not apply both to the same test.

### Keep lifecycle control rare

`ServerControl` is powerful, but most tests do not need it. Use it only when start/stop/restart behavior is itself under test.

## Author checklist

For a new testcase, prefer this checklist:

- Is this a client/server IIOP test?
  - use `@ConfigureServer`
- Can the server object be exposed as a static field or static factory?
  - use `@ConfigureServer.RemoteImpl`
- Can the test just receive the stub?
  - inject the stub directly into the test method
- Does the test actually need an ORB?
  - only then request `ORB`
- Does the test care about naming service or corbaname URLs?
  - configure `@ConfigureOrb(nameService = ...)` and inject the URLs
- Is the test specifically about historical-version interop?
  - use `@InteropTest`

If you follow that pattern, your testcase will usually be shorter, clearer, and easier to maintain.