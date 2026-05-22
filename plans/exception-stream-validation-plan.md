# Exception stream validation plan

## Objective

Add a production and test plan to ensure any exception type declared in the incoming stream is validated before instantiation. The validation must:

- check that the resolved exception type is a subtype of `Throwable`
- run through the existing serial filter mechanism before any helper, factory, or reflective instantiation occurs
- apply consistently to both `UserException` and `SystemException` processing paths

## Findings from initial inspection

### Likely production touchpoints

- `yoko-core/src/main/java/org/apache/yoko/orb/OB/ValueReader.java`
  - central valuetype unmarshalling logic
  - contains repository ID based resolution and reflective/value-factory based instantiation paths
- additional exception unmarshalling paths are likely in `yoko-core/src/main/java/org/apache/yoko/orb/OB` and related GIOP/downcall handling classes
- serial filter support already exists in `yoko-rmi-impl`, notably:
  - `org/apache/yoko/rmi/util/SerialFilterHelper`
  - adapter implementations for JDK-specific filter APIs

### Behavioural target

For every exception type declared in the stream, validate the type before constructing the exception object:

1. resolve the declared exception class from the stream metadata
2. verify the class is assignable to `Throwable`
3. invoke the existing serial filter check for that class
4. only then continue with helper-based, factory-based, or reflective instantiation

If validation fails, abort unmarshalling with the appropriate unmarshalling error before any exception instance is partially created.

## Planned implementation steps

1. Identify the exact `UserException` and `SystemException` read paths where stream-defined exception types are resolved.
2. Introduce a shared pre-instantiation validation helper in the exception unmarshalling layer.
3. Make the helper:
   - accept the resolved class and stream context
   - reject classes that are not `Throwable`
   - delegate to the existing serial filter mechanism before instantiation
4. Wire the helper into all exception creation branches:
   - helper-based resolution
   - repository-id-based resolution
   - any reflective fallback path
5. Ensure the failure path raises a deterministic unmarshalling error and does not leave partially registered instances behind.
6. Keep logging aligned with existing unmarshalling and filter diagnostics.

## Verification plan

Add or extend tests to cover:

### Positive cases

- allowed `UserException` type passes `Throwable` validation and serial filter checks
- allowed `SystemException` type passes `Throwable` validation and serial filter checks

### Negative cases

- streamed exception type resolves to a class that is not a `Throwable`
- streamed exception type is rejected by the serial filter before instantiation
- no exception object is created or partially initialised when either validation fails

## Execution checklist

- [x] Inspect current valuetype and exception unmarshalling paths to locate where exception types are resolved and instantiated
- [x] Inspect existing serial filter support to identify the reusable check mechanism for class pre-instantiation validation
- [x] Confirm scope includes both production changes and verification tests
- [ ] Add a pre-instantiation validation step in the exception unmarshalling path for any exception type declared in the stream
- [ ] Ensure the validation checks that each streamed exception type is assignable to `Throwable` before any helper, factory, or reflective instantiation occurs
- [ ] Invoke the existing serial filter mechanism against each streamed exception class before instantiation, matching current project filtering behaviour
- [ ] Apply the same checks to both `UserException` and `SystemException` processing paths, including any helper-based and repository-id-based resolution branches
- [ ] Define failure behaviour so invalid or rejected exception classes fail with the appropriate unmarshalling error without partially instantiating the exception
- [ ] Add verification tests for allowed `UserException` and `SystemException` types that pass `Throwable` and serial filter checks
- [ ] Add verification tests for streamed exception types rejected because they are not `Throwable` subclasses
- [ ] Add verification tests for streamed exception types rejected by the serial filter before instantiation
- [ ] Run the targeted test set and any relevant copyright checks after implementation
- [x] Present the plan for approval, then switch to code mode to implement once approved

## Handover

Once this plan is approved for execution, implement the production changes and tests in the identified exception unmarshalling paths.
