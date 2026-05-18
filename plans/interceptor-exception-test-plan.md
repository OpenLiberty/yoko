# CORBA Portable Interceptor Exception Handling Test Plan

## Executive Summary

This test plan covers comprehensive testing of CORBA Portable Interceptors with focus on exception handling, completion status propagation, and object type mappings across client and server interceptor chains.

## 1. Overview

### 1.1 Scope
- Client-side interceptor exception handling (3 interceptors)
- Server-side interceptor exception handling (3 interceptors)
- Exception propagation through interceptor chains
- Completion status validation (COMPLETED_YES, COMPLETED_NO, COMPLETED_MAYBE)
- Object type mappings (IDL-to-Java, RMI-IIOP)

### 1.2 Test Environment
- Base Directory: yoko-verify/src/test/java-testify/org/omg/PortableInterceptor/
- Existing Framework: Uses testify-iiop annotations and test infrastructure
- Reference Implementation: PortableInterceptorTest.java

## 2. Exception Types

### 2.1 CORBA System Exceptions

| Exception | Minor Code | Default Completion Status | Description |
|-----------|------------|---------------------------|-------------|
| NO_PERMISSION | Various | COMPLETED_NO | Authorization failure |
| BAD_PARAM | Various | COMPLETED_NO | Invalid parameter |
| NO_IMPLEMENT | Various | COMPLETED_NO | Operation not implemented |
| MARSHAL | Various | COMPLETED_NO/MAYBE | Marshaling error |
| COMM_FAILURE | Various | COMPLETED_NO/MAYBE | Communication failure |
| TRANSIENT | Various | COMPLETED_NO/MAYBE | Temporary failure |
| OBJECT_NOT_EXIST | Various | COMPLETED_NO | Object does not exist |
| BAD_INV_ORDER | Various | COMPLETED_NO | Invalid operation order |
| INTERNAL | Various | COMPLETED_NO | Internal error |
| UNKNOWN | Various | COMPLETED_YES | Unknown exception |

### 2.2 User-Defined Exceptions
- IDL-defined exceptions (e.g., TestInterfacePackage.user)
- Mapped to Java checked exceptions
- Always have COMPLETED_YES status when reaching client

### 2.3 Java Exceptions
- Checked exceptions (mapped to CORBA user exceptions)
- Unchecked exceptions (RuntimeException to UNKNOWN system exception)
- RemoteException conversions (RMI-IIOP)

## 3. Completion Status Values

### 3.1 COMPLETED_YES
**Meaning**: Operation completed successfully before exception was raised

**When Used**:
- User exceptions (always)
- System exceptions raised after target method execution
- Exceptions in send_reply() or send_exception() on server
- Exceptions in receive_reply() or receive_exception() on client

### 3.2 COMPLETED_NO
**Meaning**: Operation did not complete; request not delivered to target

**When Used**:
- Exceptions in send_request() on client
- Exceptions in receive_request_service_contexts() or receive_request() on server
- Authorization failures before method invocation
- Parameter validation failures

### 3.3 COMPLETED_MAYBE
**Meaning**: Unknown whether operation completed

**When Used**:
- Network failures during transmission
- Timeout exceptions
- Connection failures where completion status is ambiguous
- Forced connection shutdown

## 4. Test Cases

### 4.1 Client-Side Exception Tests

#### TC-CLIENT-001: Exception in send_request()
**Objective**: Verify exception thrown in send_request() prevents request transmission

**Setup**:
- 3 client interceptors (CI1, CI2, CI3)
- CI2 throws NO_PERMISSION in send_request()

**Expected Results**:
- CI1.send_request() executes successfully
- CI2.send_request() throws NO_PERMISSION with COMPLETED_NO
- CI3.send_request() is NOT called
- CI1.receive_exception() called with NO_PERMISSION (only interceptors on Flow Stack)
- CI2.receive_exception() is NOT called (interceptor that raised exception)
- CI3.receive_exception() is NOT called (never pushed to Flow Stack)
- Exception reaches client application
- No network request sent

#### TC-CLIENT-002: Exception Translation in Interceptor Chain
**Objective**: Verify interceptor can translate exceptions

**Setup**:
- CI2 throws NO_PERMISSION in send_request()
- CI1 catches and translates to BAD_INV_ORDER in receive_exception()

**Expected Results**:
- CI1.send_request() executes successfully
- CI2.send_request() throws NO_PERMISSION
- CI3.send_request() is NOT called
- CI1.receive_exception() receives NO_PERMISSION, throws BAD_INV_ORDER
- CI2.receive_exception() is NOT called (interceptor that raised exception)
- CI3.receive_exception() is NOT called (never pushed to Flow Stack)
- Client receives BAD_INV_ORDER (not NO_PERMISSION)

#### TC-CLIENT-003: Exception in receive_reply()
**Objective**: Verify exception in receive_reply() has COMPLETED_YES status

**Setup**:
- Normal request/reply succeeds
- CI2 throws NO_PERMISSION in receive_reply()

**Expected Results**:
- Request completes successfully on server
- receive_reply() called in reverse order: CI3, CI2, CI1
- CI3.receive_reply() executes successfully
- CI2.receive_reply() throws NO_PERMISSION with COMPLETED_YES
- CI1.receive_reply() is NOT called
- receive_exception() called on remaining Flow Stack: CI1
- CI1.receive_exception() called with NO_PERMISSION
- CI2.receive_exception() is NOT called (interceptor that raised exception)
- CI3.receive_exception() is NOT called (already completed successfully)
- Exception reaches client

#### TC-CLIENT-004: Exception in receive_exception()
**Objective**: Verify exception handling in receive_exception()

**Setup**:
- Server throws user exception
- CI2 throws system exception in receive_exception()

**Expected Results**:
- Original exception replaced by new exception
- CI1.receive_exception() receives new exception
- Client receives new exception

### 4.2 Server-Side Exception Tests

#### TC-SERVER-001: Exception in receive_request_service_contexts()
**Objective**: Verify early exception prevents target invocation

**Setup**:
- SI2 throws NO_PERMISSION in receive_request_service_contexts()

**Expected Results**:
- SI1.receive_request_service_contexts() executes successfully
- SI2.receive_request_service_contexts() throws NO_PERMISSION
- SI3.receive_request_service_contexts() NOT called
- SI1.send_exception() called with NO_PERMISSION (only interceptors on Flow Stack)
- SI2.send_exception() is NOT called (interceptor that raised exception)
- SI3.send_exception() is NOT called (never pushed to Flow Stack)
- Exception sent back to client with COMPLETED_NO
- Target object never invoked

#### TC-SERVER-002: Exception in receive_request()
**Objective**: Verify exception in receive_request() prevents target invocation

**Setup**:
- SI2 throws NO_PERMISSION in receive_request()

**Expected Results**:
- All receive_request_service_contexts() complete
- SI1.receive_request() completes
- SI2.receive_request() throws NO_PERMISSION
- SI3.receive_request() NOT called
- Target object NOT invoked
- send_exception() called: SI3 to SI2 to SI1 (reverse order from starting point)
- Exception has COMPLETED_NO status

#### TC-SERVER-003: Target Object Throws User Exception
**Objective**: Verify user exception propagation

**Setup**:
- Target object throws user-defined exception

**Expected Results**:
- All receive_request() interceptors complete
- Target throws user exception
- send_exception() called: SI3 to SI2 to SI1
- Exception sent to client with COMPLETED_YES
- Exception type and data preserved

#### TC-SERVER-004: Target Object Throws System Exception
**Objective**: Verify system exception propagation

**Setup**:
- Target object throws NO_IMPLEMENT system exception

**Expected Results**:
- All receive_request() interceptors complete
- Target throws NO_IMPLEMENT
- send_exception() called: SI3 to SI2 to SI1
- Exception sent to client
- Completion status appropriate for exception type
- Minor code preserved

#### TC-SERVER-005: Exception in send_reply()
**Objective**: Verify exception replaces successful reply

**Setup**:
- Target completes successfully
- SI2 throws NO_PERMISSION in send_reply()

**Expected Results**:
- Target completes successfully
- send_reply() called in reverse order: SI3, SI2, SI1
- SI3.send_reply() completes
- SI2.send_reply() throws NO_PERMISSION
- SI1.send_reply() is NOT called
- send_exception() called on remaining Flow Stack: SI1
- SI1.send_exception() called with NO_PERMISSION
- SI2.send_exception() is NOT called (interceptor that raised exception)
- SI3.send_exception() is NOT called (already completed successfully)
- Exception sent to client with COMPLETED_YES
- Original reply lost

#### TC-SERVER-006: Exception in send_exception()
**Objective**: Verify exception replacement in send_exception()

**Setup**:
- Target throws user exception
- SI2 throws different exception in send_exception()

**Expected Results**:
- Target throws original exception
- SI3.send_exception() completes
- SI2.send_exception() throws new exception
- SI1.send_exception() receives new exception
- New exception sent to client
- Original exception lost

### 4.3 Object Type Mapping Tests

#### TC-MAPPING-001: IDL Exception to Java Exception
**Objective**: Verify IDL exception marshaling and field access

**IDL Definition**:
```
exception UserException {
    string message;
    long errorCode;
};
```

**Test Scenarios**:
- Throw from target, verify marshaling
- Catch in interceptor, verify field access
- Modify in interceptor, verify changes propagate

#### TC-MAPPING-002: RMI RemoteException to CORBA
**Objective**: Verify RemoteException mapping to CORBA exceptions

**Mappings to Test**:
- AccessException to NO_PERMISSION
- MarshalException to MARSHAL
- NoSuchObjectException to OBJECT_NOT_EXIST

**Expected Results**:
- Correct CORBA exception type
- Completion status assigned correctly
- Exception detail preserved

#### TC-MAPPING-003: Java RuntimeException to CORBA
**Objective**: Verify RuntimeException wrapping

**Test Scenarios**:
- Throw NullPointerException in target
- Verify UNKNOWN exception created
- Verify original exception accessible via detail
- Verify COMPLETED_YES status (if after target execution)

#### TC-MAPPING-004: Java Checked Exception to CORBA
**Objective**: Verify checked exception mapping

**Test Scenarios**:
- Throw declared IOException (maps to user exception)
- Throw undeclared SQLException (maps to UNKNOWN)
- Verify correct CORBA exception type

### 4.4 Completion Status Tests

#### TC-STATUS-001: Status Correction in Interceptors
**Objective**: Verify ORB corrects incorrect completion status

**Test Scenarios**:
- Interceptor throws exception with wrong status in send_request()
- ORB corrects to COMPLETED_NO
- Interceptor throws exception with wrong status in receive_reply()
- ORB corrects to COMPLETED_YES

**Expected Results**:
- ORB overrides incorrect status
- Warning logged about status correction
- Client receives correct status

#### TC-STATUS-002: Status Preservation Through Chain
**Objective**: Verify completion status preserved through interceptor chain

**Test Scenarios**:
- Exception propagates through multiple interceptors
- Each interceptor inspects status
- Final exception has original status

**Expected Results**:
- Status unchanged through chain
- Each interceptor sees correct status

#### TC-STATUS-003: COMPLETED_MAYBE Scenarios
**Objective**: Verify COMPLETED_MAYBE used for ambiguous scenarios

**Test Scenarios**:
- Network timeout during request
- Connection closed mid-execution
- Server crash during method execution

**Expected Results**:
- COMPLETED_MAYBE status set correctly
- Client cannot determine if operation completed
- Appropriate retry behavior (if applicable)

### 4.5 Complex Scenario Tests

#### TC-COMPLEX-001: Cascading Exceptions
**Objective**: Verify multiple exception replacements

**Scenario**:
1. Target throws user exception
2. SI3.send_exception() throws system exception
3. SI2.send_exception() throws different system exception
4. SI1.send_exception() completes normally

**Expected Results**:
- Final exception is from SI2
- Completion status reflects original target completion
- No memory leaks from lost exceptions

#### TC-COMPLEX-002: ForwardRequest Handling
**Objective**: Verify ForwardRequest exception handling

**Scenario**:
- Server interceptor throws ForwardRequest
- Client must retry to new location

**Expected Results**:
- Request forwarded to new location
- Interceptors called again for new request
- receive_other() called on client interceptors

#### TC-COMPLEX-003: Service Context with Exceptions
**Objective**: Verify service context availability during exception handling

**Scenario**:
- Client adds service context
- Server throws exception
- Verify service context available in exception path

**Expected Results**:
- Service contexts available in send_exception()
- Reply service contexts can be added during exception
- Contexts propagate back to client

#### TC-COMPLEX-004: Slot Data with Exceptions
**Objective**: Verify PICurrent slot data handling with exceptions

**Scenario**:
- PICurrent slot data set
- Exception thrown
- Verify slot data preserved/cleared appropriately

**Expected Results**:
- Request-scoped slots cleared after exception
- Thread-scoped slots preserved
- Slot data available in exception handlers

## 5. Test Implementation Guidelines

### 5.1 Test Structure Template
```java
@ConfigureServer(
    clientOrb = @ConfigureOrb(props = "yoko.orb.id=client orb"),
    serverOrb = @ConfigureOrb(props = "yoko.orb.id=server orb")
)
public class InterceptorExceptionTest {
    
    @UseWithOrb(scope = CLIENT)
    public static class ClientOrbInitializer implements TestORBInitializer {
        // Register 3 client interceptors
    }
    
    @UseWithOrb(scope = SERVER)
    public static class ServerOrbInitializer implements TestORBInitializer {
        // Register 3 server interceptors
    }
}
```

### 5.2 Interceptor Test Helper Pattern
```java
class TestClientInterceptor implements ClientRequestInterceptor {
    private SystemException exceptionToThrow;
    private String throwAtPoint;
    private List<String> executionLog;
    
    void throwExceptionAt(String point, SystemException ex) {
        this.throwAtPoint = point;
        this.exceptionToThrow = ex;
    }
}
```

### 5.3 Assertion Helpers
```java
class InterceptorAssertions {
    static void assertCompletionStatus(
        SystemException ex, 
        CompletionStatus expected
    );
    
    static void assertInterceptorExecutionOrder(
        List<String> log,
        String... expectedOrder
    );
}
```

## 6. Success Criteria

### 6.1 Functional Requirements
- All exception types handled correctly
- Completion status set appropriately for each scenario
- Exception propagation through chains works correctly
- Object type mappings preserve exception data
- Service contexts and slots work with exceptions

### 6.2 Non-Functional Requirements
- No memory leaks from exception handling
- Performance acceptable (less than 10% overhead)
- Thread-safe exception handling
- Proper cleanup on exception paths

### 6.3 Coverage Requirements
- All interception points tested
- All exception types tested
- All completion status values tested
- All object type mappings tested
- Edge cases and error conditions covered

## 7. Test Execution Plan

### Phase 1: Basic Exception Handling
- Implement basic client interceptor exception tests
- Implement basic server interceptor exception tests
- Verify completion status for simple cases

### Phase 2: Exception Propagation
- Test exception propagation through chains
- Test exception translation in interceptors
- Test cascading exceptions

### Phase 3: Object Type Mappings
- Test IDL-to-Java exception mappings
- Test RMI-IIOP exception mappings
- Test struct/complex type handling in exceptions

### Phase 4: Edge Cases and Complex Scenarios
- Test completion status edge cases
- Test ForwardRequest handling
- Test service context and slot data with exceptions
- Performance and stress testing

## 8. References

### Existing Test Files
- PortableInterceptorTest.java - Main test suite
- CallInterceptor_impl.java - Client interceptor implementation
- ServerTestInterceptor_impl.java - Server interceptor implementation
- TranslateCallInterceptor_impl.java - Exception translation tests
- RemoteExceptionTest.java - RMI exception mapping tests

### Implementation Files
- ClientRequestInfo_impl.java - Client request info
- ServerRequestInfo_impl.java - Server request info
- Transients.java - Transient exception handling

## Appendix A: Completion Status Decision Matrix

| Interception Point | Exception Source | Completion Status |
|-------------------|------------------|-------------------|
| send_request() | Interceptor | COMPLETED_NO |
| receive_reply() | Interceptor | COMPLETED_YES |
| receive_exception() | Interceptor | Preserve original |
| receive_request_service_contexts() | Interceptor | COMPLETED_NO |
| receive_request() | Interceptor | COMPLETED_NO |
| Target method | Target object | COMPLETED_YES (user), varies (system) |
| send_reply() | Interceptor | COMPLETED_YES |
| send_exception() | Interceptor | Preserve original |
| Network failure | Transport | COMPLETED_MAYBE |
| Timeout | ORB | COMPLETED_MAYBE |

## Appendix B: Exception Type Compatibility Matrix

| Java Exception | CORBA Exception | Completion Status | Notes |
|---------------|-----------------|-------------------|-------|
| RemoteException | Various system | COMPLETED_NO | Subtype-specific mapping |
| AccessException | NO_PERMISSION | COMPLETED_NO | RMI-IIOP mapping |
| MarshalException | MARSHAL | COMPLETED_NO/MAYBE | RMI-IIOP mapping |
| NoSuchObjectException | OBJECT_NOT_EXIST | COMPLETED_NO | RMI-IIOP mapping |
| RuntimeException | UNKNOWN | COMPLETED_YES | Wrapped as detail |
| Checked Exception (declared) | User exception | COMPLETED_YES | IDL mapping |
| Checked Exception (undeclared) | UNKNOWN | COMPLETED_YES | Wrapped as detail |

## Appendix C: Test Naming Convention

Format: TC-{COMPONENT}-{SEQUENCE}

Examples:
- TC-CLIENT-001 - Client test case 1
- TC-SERVER-001 - Server test case 1
- TC-MAPPING-001 - Mapping test case 1
- TC-STATUS-001 - Status test case 1
- TC-COMPLEX-001 - Complex scenario test case 1