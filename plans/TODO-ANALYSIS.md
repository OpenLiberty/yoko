# TODO Analysis - Potential GitHub Issues

Analysis date: 2026-06-11

## Summary
Found 88 TODO comments across 53 files in the codebase. Below is a categorized list of potential issues to raise.

---

## 1. Core ORB Implementation Issues

### 1.1 Portable Interceptor Improvements
**Files:** 
- `ORBInitInfo_impl.java:139`
- `ClientRequestInfo_impl.java`, `ServerRequestInfo_impl.java`, `RequestInfo_impl.java`

**TODOs:**
- Check state in ORBInitInfo operations
- Various interceptor-related improvements needed

**Potential Issue:** "Improve Portable Interceptor state validation and implementation"

---

### 1.2 Object Reference Template (ORT) Optimization
**Files:**
- `PersistentORT_impl.java:70-72`
- `TransientORT_impl.java:70-72`

**TODO:** Use CreatePersistentObjectKey/CreateTransientObjectKey instead of populating ObjectKey data to avoid copying

**Potential Issue:** "Optimize ORT object key creation to avoid unnecessary copying"

---

### 1.3 POA (Portable Object Adapter) Enhancements

#### 1.3.1 Retain Strategy Optimization
**File:** `RetainStrategy.java:51-53`

**TODO:** Optimize servant deactivation - if servant is still in active object map, then we still have remaining activations

**Potential Issue:** "Optimize POA RetainStrategy servant deactivation checks"

#### 1.3.2 Servant ID Table Initialization
**File:** `RetainStrategy.java:259`

**TODO:** Initialize servantIdTable anyway?

**Potential Issue:** "Review and improve servantIdTable initialization in RetainStrategy"

#### 1.3.3 POA Policies Refactoring
**File:** `POAPolicies.java:84`

**TODO:** Refactor to use builder pattern for conciseness

**Potential Issue:** "Refactor POAPolicies to use builder pattern"

#### 1.3.4 Synchronization Policy
**File:** `POAPolicies.java:154-156`

**TODO:** Fix synchronization policy implementation

**Potential Issue:** "Fix POA synchronization policy implementation"

#### 1.3.5 Thread Policy
**File:** `POAPolicies.java:196-198`

**TODO:** No ThreadPolicy policy appended - some ORBs don't support it

**Potential Issue:** "Review and implement ThreadPolicy support in POA"

#### 1.3.6 Object Key Creation
**File:** `POA_impl.java:319`

**TODO:** It would be nicer if this didn't use CreateObjectKey

**Potential Issue:** "Improve POA object key creation mechanism"

#### 1.3.7 Adapter Activator
**File:** `POA_impl.java:497`

**TODO:** Adapter activator requirement not met for find_POA

**Potential Issue:** "Implement proper adapter activator support in POA.find_POA()"

#### 1.3.8 POA Interceptor Policy
**File:** `ActiveObjectOnlyStrategy.java:221-223`

**TODO:** Check the POA interceptor policy

**Potential Issue:** "Implement POA interceptor policy checking"

#### 1.3.9 Temporary OID Variable
**File:** `ActiveObjectOnlyStrategy.java:58`

**TODO:** Remove temporary oid_ variable

**Potential Issue:** "Refactor ActiveObjectOnlyStrategy to remove temporary oid_ field"

---

### 1.4 POA Manager Factory Issues

#### 1.4.1 Connection Reaping
**File:** `POAManagerFactory_impl.java:90-92`

**TODO:** When we have connection reaping, set policy to prevent connection from being reaped

**Potential Issue:** "Implement connection reaping policy for POA Manager Factory"

#### 1.4.2 Unique Name Generation
**File:** `POAManagerFactory_impl.java:97-99`

**TODO:** Marry up getAndIncrement() with incrementAndGet() to avoid confusion

**Potential Issue:** "Fix POA Manager unique name generation inconsistency"

#### 1.4.3 Self Registration
**File:** `POAManagerFactory_impl.java:336-338`

**TODO:** Implement self registration for Java servers

**Potential Issue:** "Implement self-registration for Java servers in POA Manager Factory"

---

### 1.5 Default Servant Holder
**File:** `DefaultServantHolder.java:65-67`

**TODO:** Handle NoContext exception properly (currently throws internal error)

**Potential Issue:** "Improve error handling in DefaultServantHolder for NoContext exceptions"

---

## 2. CORBA Core Issues

### 2.1 Delegate Improvements

#### 2.1.1 Rebind Policy
**File:** `Delegate.java:134-136`

**TODO:** Check Rebind Policy - raise REBIND if policy is NO_RECONNECT

**Potential Issue:** "Implement proper Rebind Policy checking in Delegate"

#### 2.1.2 Is-A Cache
**File:** `Delegate.java:244`

**TODO:** Implement is-a cache for type checking

**Potential Issue:** "Implement is-a cache for improved type checking performance"

#### 2.1.3 Codebase Support
**File:** `Delegate.java:535-537`

**TODO:** Implement get_codebase()

**Potential Issue:** "Implement get_codebase() method in Delegate"

#### 2.1.4 Client Policy
**File:** `Delegate.java:563-565`

**TODO:** Implement get_client_policy() properly

**Potential Issue:** "Implement proper client policy retrieval in Delegate"

#### 2.1.5 Connection Validation
**File:** `Delegate.java:568-570`

**TODO:** Validate policies in validate_connection()

**Potential Issue:** "Implement policy validation in Delegate.validate_connection()"

#### 2.1.6 Location Forward QoS
**File:** `Delegate.java:655-657`

**TODO:** NO_REBIND should raise exception if LocationForward changes client effective QoS policies

**Potential Issue:** "Implement QoS policy checking for LocationForward with NO_REBIND"

---

### 2.2 Request Memory Leak
**File:** `Request.java:742-744`

**TODO:** Memory leak - request sent but response never picked up. Should tell Downcall to discard response

**Potential Issue:** "Fix memory leak in Request when response is never retrieved"

---

### 2.3 Any Implementation

#### 2.3.1 Fixed Type Range Checking
**File:** `AnyImpl.java:661-663`

**TODO:** Check ranges for fixed types? Compare scale against TypeCode?

**Potential Issue:** "Implement range checking for fixed types in Any"

#### 2.3.2 Output Stream State
**File:** `AnyImpl.java:696-698`

**TODO:** Spec says calling create_output_stream and writing to any will update state

**Potential Issue:** "Implement proper state management for Any.create_output_stream()"

---

## 3. IOP and Codec Issues

### 3.1 Codec Factory Version Checking
**File:** `CodecFactory_impl.java:44-46`

**TODO:** Check major/minor version in codec creation

**Potential Issue:** "Implement version checking in CodecFactory"

---

## 4. DynamicAny Issues

### 4.1 Custom Valuetype Support
**File:** `DynAny_impl.java:473-475`

**TODO:** Custom valuetypes are not currently supported

**Potential Issue:** "Implement custom valuetype support in DynamicAny"

### 4.2 DynValue Equality
**File:** `DynAnyFactory_impl.java:321-323`

**TODO:** DynValue equalities that "span" members of sequence of DynAnys are not maintained

**Potential Issue:** "Fix DynValue equality handling across sequence members"

---

## 5. ORB Implementation Issues

### 5.1 ORB Initialization State
**File:** `ORB_impl.java:472-474`

**TODO:** Change state during post_init

**Potential Issue:** "Implement proper state management during ORB initialization"

### 5.2 Server Registration
**File:** `ORB_impl.java:1421-1423`

**TODO:** Should "register" do something different from "server"?

**Potential Issue:** "Review and clarify 'register' vs 'server' parameter handling in ORB"

---

## 6. Messaging Issues

### 6.1 User Exception Raise Proxy
**File:** `UserExceptionRaiseProxy.java:103-105`

**TODO:** Add try/catch block???

**Potential Issue:** "Review exception handling in UserExceptionRaiseProxy"

---

## 7. OCI/IIOP Transport Issues

### 7.1 Acceptor Factory Host Parsing
**File:** `AccFactory_impl.java:117-119`

**TODO:** Use library functions to split on commas and trim each element

**Potential Issue:** "Refactor AccFactory host parsing to use standard library functions"

### 7.2 COMM_FAILURE Exception Conversion
**File:** `Exceptions.java:51-53`

**TODO:** Complete conversion of all COMM_FAILURE creation

**Potential Issue:** "Complete COMM_FAILURE exception conversion in IIOP layer"

### 7.3 Acceptor Encapsulation
**File:** `Acceptor_impl.java:75-77`

**TODO:** Introduce encapsulation for acceptor data members

**Potential Issue:** "Improve encapsulation in IIOP Acceptor implementation"

### 7.4 Acceptor Finalizer
**File:** `Acceptor_impl.java:314-316`

**TODO:** Get rid of finalizer, use phantom refs in AccFactory_impl instead

**Potential Issue:** "Replace finalizer with phantom references in IIOP Acceptor"

### 7.5 IOR Profile Assertion
**File:** `Util.java:114-116`

**TODO:** Internal error? (regarding profile assertion)

**Potential Issue:** "Review and improve error handling for IOR profile validation"

---

## 8. OCI Current Issues

### 8.1 Empty Transport Stack
**File:** `OciCurrentImpl.java:62-64`

**TODO:** Should this raise an exception when transport stack is empty?

**Potential Issue:** "Review exception handling for empty transport stack in OCI Current"

---

## 9. GIOP Connection Issues

### 9.1 NEEDS_ADDRESSING_MODE
**File:** `GIOPConnection.java:429-431`, `597-599`

**TODO:** Implement NEEDS_ADDRESSING_MODE handling

**Potential Issue:** "Implement NEEDS_ADDRESSING_MODE support in GIOP connection"

### 9.2 Location Reply Status
**File:** `GIOPConnection.java:495-497`

**TODO:** Implement LOC_SYSTEM_EXCEPTION, LOC_NEEDS_ADDRESSING_MODE

**Potential Issue:** "Implement missing location reply status types in GIOP"

---

## 10. Test Framework Issues (Testify)

### 10.1 JUnit Hook Points
**File:** `ForkedPart.java:29`

**TODO:** Add support for JUnit hook points (@Before, @After)

**Potential Issue:** "Add JUnit lifecycle hook support to Testify ForkedPart"

### 10.2 ORB Steward Workaround
**File:** `OrbSteward.java:113`

**TODO:** Remove workaround once issue #783 is properly fixed in Yoko core

**Potential Issue:** "Remove OrbSteward workaround after fixing issue #783"

### 10.3 Server Instance Error Handling
**File:** `ServerInstance.java:188`

**TODO:** Find out how certain error condition happens

**Potential Issue:** "Investigate and document error condition in ServerInstance"

---

## 11. RMI-IIOP Issues

### 11.1 Field Descriptor Spec Compliance
**File:** `FieldDescriptor.java:202`

**TODO:** Make field descriptor spec-compliant

**Potential Issue:** "Make RMI-IIOP FieldDescriptor spec-compliant"

### 11.2 PortableRemoteObject.exportObject
**File:** `ServantFactory.java:26`

**TODO:** Fix the horribly broken PRO.exportObject

**Potential Issue:** "Fix PortableRemoteObject.exportObject() implementation"

---

## 12. Test Code Issues

### 12.1 Retry Test
**File:** `Client.java:83` (test/retry)

**TODO:** Fix retry functionality and reinstate test

**Potential Issue:** "Fix and reinstate retry functionality test"

### 12.2 IIOPAddress Implementation
**File:** `IIOPAddress_impl.java:102`

**TODO:** Implement missing functionality

**Potential Issue:** "Implement missing IIOPAddress functionality in test"

### 12.3 Multiple ORBs Test
**File:** `TestMultipleOrbsThreadedClient.java:66`

**TODO:** Fix shutdown issue in multiple ORBs test

**Potential Issue:** "Fix shutdown handling in multiple ORBs threaded test"

### 12.4 RMI Test Framework Support
**File:** `RMITest.java:60`

**TODO:** Add framework support for CORBA objects (as opposed to RMI-IIOP objects)

**Potential Issue:** "Add CORBA object support to RMI test framework"

### 12.5 Endpoint Handling Test
**File:** `EndpointHandlingTest.java:118-119`

**TODO:** Fix PortableRemoteObject.exportObject() so it doesn't create an ORB

**Potential Issue:** "Prevent ORB creation in PortableRemoteObject.exportObject() for tests"

### 12.6 Servant Activator Test
**File:** `ServantActivatorServerTest.java:58`

**TODO:** Find a way to test after ORB shutdown

**Potential Issue:** "Implement post-shutdown testing for ServantActivator"

### 12.7 INS Test Integration
**File:** `InsTest.java:61`

**TODO:** Integrate more tightly with newer features of testify framework

**Potential Issue:** "Modernize INS test to use newer Testify features"

### 12.8 Portable Interceptor Tests
**Multiple files in test/pi/**

**TODOs:**
- Test context in various interceptor methods
- Test sync scope
- Test get_effective_components
- Test operation_context

**Potential Issue:** "Complete Portable Interceptor test coverage"

---

## Priority Recommendations

### High Priority (Correctness/Spec Compliance)
1. Fix memory leak in Request (2.2)
2. Implement proper Rebind Policy checking (2.1.1)
3. Make RMI-IIOP FieldDescriptor spec-compliant (11.1)
4. Fix PortableRemoteObject.exportObject() (11.2)
5. Implement NEEDS_ADDRESSING_MODE support (9.1)

### Medium Priority (Performance/Optimization)
1. Optimize ORT object key creation (1.2)
2. Implement is-a cache (2.1.2)
3. Optimize POA RetainStrategy (1.3.1)
4. Replace finalizer with phantom references (7.4)

### Low Priority (Code Quality/Refactoring)
1. Refactor POAPolicies to use builder pattern (1.3.3)
2. Improve encapsulation in IIOP Acceptor (7.3)
3. Refactor host parsing to use standard library (7.1)
4. Remove temporary variables (1.3.9)

### Test/Framework Improvements
1. Add JUnit hook support to Testify (10.1)
2. Complete Portable Interceptor test coverage (12.8)
3. Modernize tests to use newer Testify features (12.7)

---

## Notes

- Many TODOs are related to incomplete implementations of CORBA specifications
- Several performance optimization opportunities identified
- Test framework (Testify) has several enhancement opportunities
- Some TODOs reference specific issues (e.g., #783) that should be cross-referenced
