# Exception Behaviour Analysis: Reflection → MethodHandle Migration

## Background

Several methods in `ValueDescriptor` (and related classes) that previously invoked
user-defined methods via `Method.invoke()` / `Constructor.newInstance()` have been
migrated to use `MethodHandle.invoke()` instead. This document analyses whether that
migration changes any externally observable exception-handling behaviour, and records
the corrections made as a result.

## The Core Difference

| Invocation style | Exception thrown by target | What the caller sees |
|---|---|---|
| `Method.invoke()` / `Constructor.newInstance()` | anything (checked or unchecked) | `InvocationTargetException` wrapping the original |
| `MethodHandle.invoke()` | `RuntimeException` / `Error` | propagates unwrapped, as normal |
| `MethodHandle.invoke()` | checked exception | the original exception directly as `Throwable` |

`MethodHandle.invoke()` is declared `throws Throwable` and re-throws whatever the
target threw directly, with no wrapper. `InvocationTargetException` is a
reflection-only artefact — `MethodHandle` was specifically designed to avoid it.

---

## First Principles: What Exceptions Are "Expected" From These Methods?

The methods being invoked via `MethodHandle` are all Java serialization or IDL
mapping contract methods:

| Method | Declared throws |
|---|---|
| `writeObject(ObjectOutputStream)` | `IOException` only |
| `readObject(ObjectInputStream)` | `IOException`, `ClassNotFoundException` |
| `writeReplace()` | `ObjectStreamException` (subclass of `IOException`) |
| `readResolve()` | `ObjectStreamException` |
| `writeExternal(ObjectOutput)` | `IOException` only |
| `Helper.read(InputStream)` | CORBA system exceptions only |
| `Helper.write(OutputStream, T)` | CORBA system exceptions only |
| `Helper.type()` | nothing |

Any unchecked exception (`RuntimeException` or `Error`) thrown by a user
implementation of these methods is, by definition, unexpected — it is not an
outcome that the serialization or IDL mapping specs make any provision for.

Therefore the correct and principled behaviour is:

- `IOException` (and subclasses) → handle specifically, as the expected failure mode
- Everything else, including `RuntimeException` and `Error` → treat as unexpected;
  wrap and report as a CORBA system exception

The initial `MethodHandle` migration introduced catch ladders that special-cased
`Error | RuntimeException` for pass-through, which departed from this principle.
The sections below document where this caused problems and the corrections made.

---

## Sites Converted to MethodHandle: Analysis and Corrections

### 1. `writeReplace` / `readResolve` — `genWriteReplacer`, `genReadResolver`

**Contract:** `ObjectStreamException` only (subclass of `IOException`).

The old `Method.invoke()` code caught `InvocationTargetException` and converted
everything — including unchecked exceptions — to `UnknownException`:

```java
// OLD
} catch (InvocationTargetException ex) {
    throw as(UnknownException::new, ex.getTargetException(), ex.getTargetException());
}
```

The initial `MethodHandle` migration introduced:

```java
// INITIAL — INCORRECT
} catch (Error | RuntimeException e) { throw e; }   // pass-through
} catch (Throwable t)                 { throw as(UnknownException::new, t, t); }
```

**Why the pass-through was wrong:**

A bare `RuntimeException` escaping `writeReplace` reaches `RMIServant._invoke0`'s
`catch (RuntimeException)` branch, which passes it to `method.writeException()`.
That method tries to match it against the method's declared exception types, fails,
and throws a new `UnknownException(runtimeEx)` — which then propagates to the POA.

A bare `Error` is worse: it escapes `_invoke0` entirely (the POA only catches
`Exception`, not `Error`), propagates past the POA dispatch loop, and reaches the
thread pool or transport layer with **no reply sent to the client**.

Under the old code, both were wrapped in `UnknownException` at the lambda boundary,
caught by `_invoke0`'s `catch (SystemException)` branch, and correctly set as the
system exception on the upcall — resulting in an UNKNOWN reply with the original
exception serialised into the `UnknownExceptionInfo` service context.

**Correction applied:** Remove the `catch (Error | RuntimeException)` pass-through
so all non-`IOException` throwables are wrapped in `UnknownException`:

```java
// CORRECTED
} catch (Throwable t) { throw as(UnknownException::new, t, t); }
```

### 2. `readObject` — `buildReader`, `buildCustomMarshalReaderWithReadObject`

**Contract:** `IOException` and `ClassNotFoundException` only.

The initial migration introduced the same `catch (Error | RuntimeException e) { throw e; }`
pass-through ahead of the `IOException` and `Throwable` branches.

By the same reasoning as above, an `Error` or `RuntimeException` from a user's
`readObject` is unexpected and should be wrapped in `UnknownException`, not passed
through.

**Correction applied:** Remove the `catch (Error | RuntimeException)` branch,
leaving:

```java
// CORRECTED
} catch (IOException e)  { throw new UncheckedIOException(e); }
} catch (Throwable t)    { throw as(UnknownException::new, t, t); }
```

### 3. `writeObject` — `ObjectWriter.invokeWriteObject`

**Contract:** `IOException` only.

The initial migration had:

```java
// INITIAL — INCORRECT
} catch (Error | RuntimeException | IOException e) { throw e; }
} catch (Throwable t) { throw new IOException("Error invoking writeObject", t); }
```

This passed `Error` and `RuntimeException` through directly for the same wrong
reason, and also wrapped other `Throwable` in `IOException` rather than
`UnknownException`, obscuring the cause type at the POA boundary.

**Correction applied:**

```java
// CORRECTED
} catch (IOException e) { throw e; }
} catch (Throwable t)   { throw as(UnknownException::new, t, t); }
```

The `InvocationTargetException` import (a dead import left from before the
migration) was also removed, and `import static org.apache.yoko.util.Exceptions.as`
added.

### 4. IDL Helper methods — `IDLEntityDescriptor`: `genReader`, `genWriter`, `genTypeCode`

**Contract:** CORBA system exceptions only (generated code).

These invoke `Helper.read()`, `Helper.write()`, and `Helper.type()` — IDL-generated
helper methods. An `Error` or `RuntimeException` from them is equally unexpected.
The correct wrapping here is `MARSHAL` (not `UnknownException`, since these are
marshalling operations rather than RMI value operations).

The initial migration had `catch (Error | RuntimeException e) { throw e; }` ahead
of `catch (Throwable t) { throw as(MARSHAL::new, t); }` in all three lambdas.

**Correction applied:** Remove the pass-through branch in all three, leaving only:

```java
// CORRECTED
} catch (Throwable t) { throw as(MARSHAL::new, t); }
```

### 5. `readObject` (custom marshal path) — `buildCustomMarshalReaderWithReadObject`

Same contract and correction as §2 above. The `catch (Error | RuntimeException)`
pass-through was removed.

---

## Sites Requiring No Change

### Externalizable constructor — `toSupplier(MethodHandle)`

Invokes a no-arg constructor via `MethodHandle`. A constructor is not a serialization
contract method with a specific declared exception set. An `Error` here (e.g.
`OutOfMemoryError` while initialising fields) represents a genuine JVM-level failure
that should propagate directly. The pass-through of `RuntimeException | Error` is
correct here.

### Field getter/setter — `FieldDescriptor.setFieldContents`, `getFieldContents`

Already wraps all `Throwable` in `IOException`. No pass-through. Correct.

### Stub constructor — `RMIState.genStubSupplier`

Invokes a generated stub's no-arg constructor. Wraps all `Throwable` in
`RuntimeException("internal problem: cannot instantiate stub", ...)`. This is
internal ORB infrastructure, not user serialization code. Correct.

### IDL narrow — `PortableRemoteObjectImpl.narrowIDL`

Invokes a Helper `narrow()` method. Passes `Error` through directly (correct — a
JVM-level failure during narrowing should propagate), wraps everything else as
`ClassCastException` (correct — narrowing failure is a type mismatch). Correct.

---

## Remaining Use of `Constructor.newInstance()` and `InvocationTargetException`

`ReflectionFactory`-generated constructors (used for Serializable object
instantiation) cannot be converted to `MethodHandle`s — they are specifically
designed to work with `Constructor.newInstance()` and cannot be adapted.

`ValueDescriptor.toSupplier(Constructor<?>)` therefore still uses
`Constructor.newInstance()` and still catches `InvocationTargetException`,
unwrapping it to `UnknownException`. This path is unchanged and correct.

---

## Summary of Corrections

| File | Method | Invokes | Problem | Fix |
|---|---|---|---|---|
| `ValueDescriptor` | `genWriteReplacer` | `writeReplace()` | `RuntimeException`/`Error` passed through; `Error` escapes POA | Wrap all in `UnknownException` |
| `ValueDescriptor` | `genReadResolver` | `readResolve()` | Same | Same |
| `ValueDescriptor` | `buildReader` | `readObject()` | Same | Same |
| `ValueDescriptor` | `buildCustomMarshalReaderWithReadObject` | `readObject()` (custom marshal) | Same | Same |
| `ObjectWriter` | `invokeWriteObject` | `writeObject()` | `RuntimeException`/`Error` passed through; other `Throwable` wrapped in `IOException` instead of `UnknownException` | Wrap all non-`IOException` in `UnknownException` |
| `IDLEntityDescriptor` | `genReader` | `Helper.read()` | `RuntimeException`/`Error` passed through | Wrap all in `MARSHAL` |
| `IDLEntityDescriptor` | `genWriter` | `Helper.write()` | Same | Same |
| `IDLEntityDescriptor` | `genTypeCode` | `Helper.type()` | Same | Same |
