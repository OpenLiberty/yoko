# Plan: Lazy Unmarshalling of Value Types in Any

## Overview

This plan addresses the need to implement lazy unmarshalling for value types stored in CORBA `Any` objects. Currently, value types (`tk_value`, `tk_value_box`, `tk_abstract_interface`) are eagerly unmarshalled when read into an Any, which can cause performance issues and unnecessary object creation.

## Current Implementation Analysis

### Existing Behavior

**Location**: `yoko-core/src/main/java/org/apache/yoko/orb/CORBA/AnyImpl.java`

#### Eager Unmarshalling (Lines 270-284)

```java
case _tk_value:
case _tk_value_box:
case _tk_abstract_interface: {
    try {
        YokoInputStream is = (YokoInputStream) in;
        is.read_value(this, typeCode);  // EAGER: Immediately deserializes
    } catch (ClassCastException ex) {
        try {
            org.omg.CORBA_2_3.portable.InputStream is = (org.omg.CORBA_2_3.portable.InputStream) in;
            value = is.read_value(typeCode.id());  // EAGER: Immediately deserializes
        } catch (BadKind e) {
            throw fail(e);
        }
    }
    break;
}
```

**Problem**: The value is immediately deserialized when reading into the Any, even if it's never extracted.

#### Lazy Pattern Already Used (Lines 257-268)

Struct, union, sequence, and array types already implement lazy unmarshalling:

```java
case _tk_struct:
case _tk_except:
case _tk_union:
case _tk_sequence:
case _tk_array: {
    try (YokoOutputStream out = new YokoOutputStream()) {
        out._OB_ORBInstance(orbInstance);
        out.write_InputStream(in, origTypeCode);
        value = out.create_input_stream();  // LAZY: Store stream, not deserialized object
    }
    break;
}
```

#### Extract Method Already Handles Lazy Case (Lines 961-979)

```java
public Serializable extract_Value() throws BAD_OPERATION {
    // ... validation ...
    
    if (value instanceof YokoInputStream) {
        YokoInputStream in = (YokoInputStream) value;
        in._OB_reset();
        if (kind == tk_abstract_interface) ensure(!in.read_boolean());
        return in.read_value();  // Deserialize on demand
    } else
        return (Serializable) value;  // Already deserialized
}
```

**Good news**: The extract method already supports both lazy (YokoInputStream) and eager (Serializable) cases.

## Problem Statement

### Performance Issues

1. **Unnecessary Deserialization**: Value types are deserialized even when never extracted from the Any
2. **Memory Overhead**: Deserialized objects consume more memory than their serialized form
3. **CPU Waste**: Deserialization is expensive and may be completely unnecessary
4. **Inconsistency**: Struct/union/array types use lazy unmarshalling, but value types don't

### Use Cases Affected

- **Request/Response Processing**: Anys containing value types in operation parameters or results
- **DII (Dynamic Invocation Interface)**: Dynamic requests with value type arguments
- **Any Forwarding**: Passing Anys through intermediaries without extracting values
- **Logging/Debugging**: Inspecting TypeCodes without needing actual values

## Proposed Solution

### Design Approach

Apply the same lazy unmarshalling pattern used for struct/union/array types to value types:

1. **Store the input stream** instead of deserializing immediately
2. **Deserialize on demand** when `extract_Value()` is called
3. **Maintain backward compatibility** by supporting both lazy and eager cases

### Key Changes Required

#### 1. Modify `readValue()` Method (Lines 270-284)

**Current (Eager)**:
```java
case _tk_value:
case _tk_value_box:
case _tk_abstract_interface: {
    try {
        YokoInputStream is = (YokoInputStream) in;
        is.read_value(this, typeCode);  // Eager deserialization
    } catch (ClassCastException ex) {
        // ... fallback ...
    }
    break;
}
```

**Proposed (Lazy)**:
```java
case _tk_value:
case _tk_value_box:
case _tk_abstract_interface: {
    try (YokoOutputStream out = new YokoOutputStream()) {
        out._OB_ORBInstance(orbInstance);
        out.write_InputStream(in, origTypeCode);
        value = out.create_input_stream();  // Store stream, defer deserialization
    }
    break;
}
```

#### 2. Update `write_value()` Method (Lines 629-679)

**Current Handling**:
```java
case _tk_value: {
    YokoOutputStream o = (YokoOutputStream) out;
    if (value instanceof YokoInputStream) {
        YokoInputStream in = (YokoInputStream) value;
        in._OB_reset();
        o.write_InputStream(in, typeCode);
    } else
        o.write_value((Serializable) value);
    break;
}
```

**Status**: Already handles both lazy (YokoInputStream) and eager (Serializable) cases correctly. **No changes needed**.

#### 3. Update `copyFrom()` Method (Lines 390-398)

**Current Handling**:
```java
case _tk_value:
case _tk_value_box:
case _tk_abstract_interface:
    if (any.value instanceof YokoInputStream)
        readValue(any.create_input_stream());
    else
        value = any.value;
    break;
```

**Status**: Already handles both cases. **No changes needed**.

#### 4. Update `equal()` Method (Lines 489-497)

**Current Handling**:
```java
case _tk_value:
case _tk_value_box: {
    if (value instanceof YokoInputStream && any.value instanceof YokoInputStream) {
        return compareValuesAsInputStreams(this, any);
    } else
        return false;
}
```

**Issue**: Returns `false` if one Any has deserialized value and other has stream.

**Proposed Fix**:
```java
case _tk_value:
case _tk_value_box: {
    // Ensure both are in same form for comparison
    if (value instanceof YokoInputStream || any.value instanceof YokoInputStream) {
        return compareValuesAsInputStreams(this, any);
    } else {
        // Both are deserialized - compare objects
        return value.equals(any.value);
    }
}
```

#### 5. Constructor Handling (Lines 1099-1103)

**Current**:
```java
case _tk_objref:
case _tk_abstract_interface:
case _tk_local_interface:
    try {
        value = any.extract_Object();
        break;
    } catch (BAD_OPERATION ex) {
        // Any must hold an abstract interface representing
        // a valuetype, so fall through to default case
    }

default:
    readValue(any.create_input_stream());
    break;
```

**Status**: Falls through to `readValue()` which will be updated. **No additional changes needed**.

## Implementation Steps

### Phase 1: Core Lazy Unmarshalling

1. **Update `readValue()` method** to store input stream for value types
   - Apply same pattern as struct/union/array types
   - Remove eager `read_value()` calls
   - Store `YokoInputStream` in `value` field

2. **Verify `extract_Value()` method** handles lazy case
   - Already implemented correctly
   - No changes needed

3. **Verify `write_value()` method** handles lazy case
   - Already implemented correctly
   - No changes needed

### Phase 2: Equality and Comparison

4. **Fix `equal()` method** to handle mixed lazy/eager cases
   - Update comparison logic for value types
   - Ensure consistent behavior regardless of unmarshalling state

### Phase 3: Testing

5. **Create unit tests** for lazy unmarshalling
   - Test value types are not deserialized on read
   - Test deserialization occurs on extract
   - Test equality with mixed lazy/eager states
   - Test write after lazy read
   - Test copy operations

6. **Create performance tests**
   - Measure memory usage improvement
   - Measure CPU time savings
   - Compare with eager unmarshalling baseline

7. **Run existing test suite**
   - Ensure backward compatibility
   - Verify no regressions in existing functionality

### Phase 4: Documentation

8. **Update code comments** to explain lazy unmarshalling
9. **Document behavior change** in CHANGELOG
10. **Update any relevant user documentation**

## Backward Compatibility

### Compatibility Considerations

1. **API Compatibility**: No public API changes required
2. **Behavior Compatibility**: 
   - Externally observable behavior remains the same
   - Values are still extracted correctly
   - Equality comparisons work correctly
3. **Performance Compatibility**: 
   - Performance improves (lazy is faster)
   - No negative performance impact

### Risk Assessment

**Low Risk**: 
- Changes are internal implementation details
- Existing code already handles both lazy and eager cases
- Pattern is proven (already used for struct/union/array)

## Performance Benefits

### Expected Improvements

1. **Memory Savings**: 
   - Serialized form is typically smaller than deserialized objects
   - Especially significant for large value types or collections

2. **CPU Savings**:
   - Avoid deserialization when value is never extracted
   - Common in forwarding scenarios and logging

3. **Latency Reduction**:
   - Faster request processing when values aren't needed
   - Reduced GC pressure from fewer temporary objects

### Measurement Strategy

Create benchmarks to measure:
- Time to read Any with value type (should decrease)
- Memory usage of Any with value type (should decrease)
- Time to extract value (should remain same)
- End-to-end request processing time (should decrease for forwarding cases)

## Testing Strategy

### Unit Tests

1. **Lazy Read Test**
   ```java
   @Test
   void testValueTypeNotDeserializedOnRead() {
       // Create Any with value type
       // Verify value field is YokoInputStream, not deserialized object
   }
   ```

2. **Extract Test**
   ```java
   @Test
   void testValueTypeDeserializedOnExtract() {
       // Create Any with value type (lazy)
       // Extract value
       // Verify correct deserialized object returned
   }
   ```

3. **Equality Test**
   ```java
   @Test
   void testEqualityWithMixedLazyEager() {
       // Create two Anys with same value
       // One lazy, one eager
       // Verify they are equal
   }
   ```

4. **Write After Read Test**
   ```java
   @Test
   void testWriteAfterLazyRead() {
       // Read Any with value type (lazy)
       // Write Any to output stream
       // Verify correct serialization
   }
   ```

5. **Copy Test**
   ```java
   @Test
   void testCopyLazyAny() {
       // Create Any with value type (lazy)
       // Copy to new Any
       // Verify copy is also lazy
       // Extract from copy and verify correct value
   }
   ```

### Integration Tests

1. **DII Test**: Test dynamic invocation with value type parameters
2. **Forwarding Test**: Test Any forwarding without extraction
3. **Interoperability Test**: Test with foreign ORB implementations

### Performance Tests

1. **Memory Benchmark**: Measure heap usage with lazy vs eager
2. **CPU Benchmark**: Measure CPU time for read operations
3. **Throughput Benchmark**: Measure requests/second for forwarding scenario

## Success Criteria

- [ ] Value types are stored as input streams, not deserialized objects
- [ ] Deserialization occurs only when `extract_Value()` is called
- [ ] All existing tests pass without modification
- [ ] New tests verify lazy unmarshalling behavior
- [ ] Performance benchmarks show improvement
- [ ] No backward compatibility issues
- [ ] Code review approved
- [ ] Documentation updated

## Risks and Mitigations

### Risk 1: Hidden Dependencies on Eager Unmarshalling

**Mitigation**: Comprehensive test suite, especially integration tests

### Risk 2: Performance Regression in Extract Path

**Mitigation**: Performance benchmarks to verify extract performance unchanged

### Risk 3: Equality Comparison Edge Cases

**Mitigation**: Extensive equality tests with mixed lazy/eager states

## Timeline Estimate

- **Phase 1 (Core Changes)**: 2-3 hours
- **Phase 2 (Equality Fix)**: 1-2 hours  
- **Phase 3 (Testing)**: 4-6 hours
- **Phase 4 (Documentation)**: 1-2 hours

**Total**: 8-13 hours of development time

## Next Steps

1. Review and approve this plan
2. Switch to `code` mode to implement Phase 1
3. Implement and test incrementally
4. Review and merge changes

## References

- CORBA Specification: Section on Any type
- Yoko ORB Documentation
- Existing lazy unmarshalling implementation for struct/union/array types
