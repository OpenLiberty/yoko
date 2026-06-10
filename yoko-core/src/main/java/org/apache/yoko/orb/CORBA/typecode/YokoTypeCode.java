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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.apache.yoko.orb.CORBA.typecode;

import org.apache.yoko.util.concurrent.LazyReference;
import org.omg.CORBA.Any;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.TypeCodePackage.BadKind;
import org.omg.CORBA.TypeCodePackage.Bounds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.yoko.util.Assert.ensure;
import static org.omg.CORBA.TCKind.tk_Principal;
import static org.omg.CORBA.TCKind.tk_TypeCode;
import static org.omg.CORBA.TCKind.tk_abstract_interface;
import static org.omg.CORBA.TCKind.tk_alias;
import static org.omg.CORBA.TCKind.tk_any;
import static org.omg.CORBA.TCKind.tk_array;
import static org.omg.CORBA.TCKind.tk_boolean;
import static org.omg.CORBA.TCKind.tk_char;
import static org.omg.CORBA.TCKind.tk_double;
import static org.omg.CORBA.TCKind.tk_enum;
import static org.omg.CORBA.TCKind.tk_except;
import static org.omg.CORBA.TCKind.tk_fixed;
import static org.omg.CORBA.TCKind.tk_float;
import static org.omg.CORBA.TCKind.tk_long;
import static org.omg.CORBA.TCKind.tk_longdouble;
import static org.omg.CORBA.TCKind.tk_longlong;
import static org.omg.CORBA.TCKind.tk_native;
import static org.omg.CORBA.TCKind.tk_null;
import static org.omg.CORBA.TCKind.tk_objref;
import static org.omg.CORBA.TCKind.tk_octet;
import static org.omg.CORBA.TCKind.tk_sequence;
import static org.omg.CORBA.TCKind.tk_short;
import static org.omg.CORBA.TCKind.tk_string;
import static org.omg.CORBA.TCKind.tk_struct;
import static org.omg.CORBA.TCKind.tk_ulong;
import static org.omg.CORBA.TCKind.tk_ulonglong;
import static org.omg.CORBA.TCKind.tk_union;
import static org.omg.CORBA.TCKind.tk_ushort;
import static org.omg.CORBA.TCKind.tk_value;
import static org.omg.CORBA.TCKind.tk_value_box;
import static org.omg.CORBA.TCKind.tk_void;
import static org.omg.CORBA.TCKind.tk_wchar;
import static org.omg.CORBA.TCKind.tk_wstring;
import static org.omg.CORBA_2_4.TCKind.tk_local_interface;


/**
 * Abstract base class for all TypeCode implementations.
 * Provides common functionality and defines the contract for type-specific implementations.
 */
public abstract class YokoTypeCode extends TypeCode {
    protected final TCKind kind;

    protected YokoTypeCode(TCKind kind) {
        this.kind = kind;
    }

    /**
     * Lazily initialized primitive TypeCode instances.
     * Each primitive kind has a single shared instance created on first access.
     * Non-primitive kinds throw IllegalArgumentException.
     * Map key is TCKind.value() for efficient lookup.
     */
    private static final Map<TCKind, LazyReference<YokoTypeCode>> PRIMITIVE_TYPECODES;

    static {
        // TCKind values for primitive types that can be created via getPrimitive()
        TCKind[] primitiveKinds = {
            tk_null, tk_void, tk_short, tk_long,
            tk_ushort, tk_ulong, tk_float, tk_double,
            tk_boolean, tk_char, tk_octet, tk_any,
            tk_TypeCode, tk_Principal, tk_longlong,
            tk_ulonglong, tk_longdouble, tk_wchar
        };

        // TCKind values for non-primitive types that require additional parameters
        TCKind[] nonPrimitiveKinds = {
            tk_objref, tk_struct, tk_union, tk_enum,
            tk_string, tk_sequence, tk_array, tk_alias,
            tk_except, tk_wstring, tk_fixed, tk_value,
            tk_value_box, tk_native, tk_abstract_interface, tk_local_interface
        };

        Map<TCKind, LazyReference<YokoTypeCode>> map = new HashMap<>();

        // Populate primitive types
        java.util.Arrays.stream(primitiveKinds).forEach(kind ->
            map.put(kind, new LazyReference<>(() -> new PrimitiveTypeCode(kind)))
        );

        // Populate non-primitive types that throw exceptions
        java.util.Arrays.stream(nonPrimitiveKinds).forEach(kind ->
            map.put(kind, new LazyReference<>(() -> {
                throw new IllegalArgumentException("TCKind " + kind + " is not a primitive type");
            }))
        );

        PRIMITIVE_TYPECODES = Collections.unmodifiableMap(map);
    }

    /**
     * Gets a primitive TypeCode for the specified kind.
     * Returns a shared singleton instance for each primitive type.
     *
     * @param kind the TCKind of the primitive type
     * @return the primitive TypeCode instance
     * @throws IllegalArgumentException if the kind is not a primitive type or unknown
     */
    public static YokoTypeCode getPrimitive(TCKind kind) {
        return java.util.Optional.ofNullable(PRIMITIVE_TYPECODES.get(kind))
            .map(LazyReference::get)
            .orElseThrow(() -> new IllegalArgumentException("Unknown TCKind: " + kind));
    }

    /**
     * Creates a string TypeCode with the specified maximum length.
     * Use length 0 for unbounded strings.
     *
     * @param length the maximum length of the string (0 for unbounded)
     * @return a new string TypeCode
     */
    public static YokoTypeCode createString(int length) {
        return new StringTypeCode(TCKind.tk_string, length);
    }

    /**
     * Creates a wide string TypeCode with the specified maximum length.
     * Use length 0 for unbounded wide strings.
     *
     * @param length the maximum length of the wide string (0 for unbounded)
     * @return a new wide string TypeCode
     */
    public static YokoTypeCode createWString(int length) {
        return new StringTypeCode(TCKind.tk_wstring, length);
    }

    /**
     * Creates a sequence TypeCode with the specified element type and maximum length.
     * Use length 0 for unbounded sequences.
     *
     * @param length the maximum length of the sequence (0 for unbounded)
     * @param contentType the TypeCode of the sequence elements
     * @return a new sequence TypeCode
     */
    public static YokoTypeCode createSequence(int length, TypeCode contentType) {
        return new SequenceTypeCode(length, contentType);
    }

    /**
     * Creates an array TypeCode with the specified element type and length.
     *
     * @param length the fixed length of the array
     * @param contentType the TypeCode of the array elements
     * @return a new array TypeCode
     */
    public static YokoTypeCode createArray(int length, TypeCode contentType) {
        return new ArrayTypeCode(length, contentType);
    }

    /**
     * Creates a struct TypeCode with the specified repository ID, name, and members.
     *
     * @param id the repository ID
     * @param name the struct name
     * @param memberNames array of member names
     * @param memberTypes array of member TypeCodes
     * @return a new struct TypeCode
     */
    public static YokoTypeCode createStruct(String id, String name, String[] memberNames, TypeCode[] memberTypes) {
        return new StructTypeCode(id, name, memberNames, memberTypes);
    }

    /**
     * Creates a union TypeCode with the specified repository ID, name, discriminator, and members.
     *
     * @param id the repository ID
     * @param name the union name
     * @param discriminatorType the TypeCode of the discriminator
     * @param memberNames array of member names
     * @param memberTypes array of member TypeCodes
     * @param labels array of discriminator labels for each member
     * @return a new union TypeCode
     */
    public static YokoTypeCode createUnion(String id, String name, TypeCode discriminatorType, String[] memberNames, TypeCode[] memberTypes, Any[] labels) {
        return new UnionTypeCode(id, name, discriminatorType, memberNames, memberTypes, labels);
    }

    /**
     * Creates an enum TypeCode with the specified repository ID, name, and member names.
     *
     * @param id the repository ID
     * @param name the enum name
     * @param memberNames array of enumerator names
     * @return a new enum TypeCode
     */
    public static YokoTypeCode createEnum(String id, String name, String[] memberNames) {
        return new EnumTypeCode(id, name, memberNames);
    }

    /**
     * Creates an alias (typedef) TypeCode with the specified repository ID, name, and content type.
     *
     * @param id the repository ID
     * @param name the alias name
     * @param contentType the TypeCode being aliased
     * @return a new alias TypeCode
     */
    public static YokoTypeCode createAlias(String id, String name, TypeCode contentType) {
        return new AliasTypeCode(id, name, contentType);
    }

    /**
     * Creates an exception TypeCode with the specified repository ID, name, and members.
     *
     * @param id the repository ID
     * @param name the exception name
     * @param memberNames array of member names
     * @param memberTypes array of member TypeCodes
     * @return a new exception TypeCode
     */
    public static YokoTypeCode createException(String id, String name, String[] memberNames, TypeCode[] memberTypes) {
        return new ExceptionTypeCode(id, name, memberNames, memberTypes);
    }

    /**
     * Creates an object reference TypeCode with the specified repository ID and name.
     *
     * @param id the repository ID
     * @param name the interface name
     * @return a new object reference TypeCode
     */
    public static YokoTypeCode createObjectRef(String id, String name) {
        return new ObjectRefTypeCode(TCKind.tk_objref, id, name);
    }

    /**
     * Creates an abstract interface TypeCode with the specified repository ID and name.
     *
     * @param id the repository ID
     * @param name the interface name
     * @return a new abstract interface TypeCode
     */
    public static YokoTypeCode createAbstractInterface(String id, String name) {
        return new ObjectRefTypeCode(TCKind.tk_abstract_interface, id, name);
    }

    /**
     * Creates a local interface TypeCode with the specified repository ID and name.
     *
     * @param id the repository ID
     * @param name the interface name
     * @return a new local interface TypeCode
     */
    public static YokoTypeCode createLocalInterface(String id, String name) {
        return new ObjectRefTypeCode(tk_local_interface, id, name);
    }

    /**
     * Creates a native TypeCode with the specified repository ID and name.
     *
     * @param id the repository ID
     * @param name the native type name
     * @return a new native TypeCode
     */
    public static YokoTypeCode createNative(String id, String name) {
        return new NativeTypeCode(id, name);
    }

    /**
     * Creates a fixed-point TypeCode with the specified digits and scale.
     *
     * @param digits the total number of decimal digits
     * @param scale the scale (position of decimal point)
     * @return a new fixed-point TypeCode
     */
    public static YokoTypeCode createFixed(short digits, short scale) {
        return new FixedTypeCode(digits, scale);
    }

    /**
     * Creates a value TypeCode with the specified parameters.
     *
     * @param id the repository ID
     * @param name the value type name
     * @param typeModifier the type modifier (VM_NONE, VM_CUSTOM, VM_ABSTRACT, VM_TRUNCATABLE)
     * @param concreteBase the concrete base type (or null)
     * @param memberNames array of member names
     * @param memberTypes array of member TypeCodes
     * @param memberVisibility array of member visibility flags
     * @return a new value TypeCode
     */
    public static YokoTypeCode createValue(String id, String name, short typeModifier, TypeCode concreteBase, String[] memberNames, TypeCode[] memberTypes, short[] memberVisibility) {
        return new ValueTypeCode(id, name, typeModifier, concreteBase, memberNames, memberTypes, memberVisibility);
    }

    /**
     * Creates a value box TypeCode with the specified repository ID, name, and boxed type.
     *
     * @param id the repository ID
     * @param name the value box name
     * @param boxedType the TypeCode being boxed
     * @return a new value box TypeCode
     */
    public static YokoTypeCode createValueBox(String id, String name, TypeCode boxedType) {
        return new ValueBoxTypeCode(id, name, boxedType);
    }


    @Override
    public TCKind kind() {
        return kind;
    }

    @Override
    public String toString() {
        return describe(new StringBuilder(), "", new HashSet<>()).toString();
    }

    protected abstract StringBuilder describe(StringBuilder sb, String prefix, Set<String> describedIds);

    protected static final String NL = System.lineSeparator();

    // Default implementations that throw BadKind - subclasses override as needed

    @Override public String id() throws BadKind { throw new BadKind(); }
    @Override public String name() throws BadKind { throw new BadKind(); }
    @Override public int member_count() throws BadKind { throw new BadKind(); }
    @Override public String member_name(int index) throws BadKind, Bounds { throw new BadKind(); }
    @Override public TypeCode member_type(int index) throws BadKind, Bounds { throw new BadKind(); }
    @Override public Any member_label(int index) throws BadKind, Bounds { throw new BadKind(); }
    @Override public TypeCode discriminator_type() throws BadKind { throw new BadKind(); }
    @Override public int default_index() throws BadKind { throw new BadKind(); }
    @Override public int length() throws BadKind { throw new BadKind(); }
    @Override public TypeCode content_type() throws BadKind { throw new BadKind(); }
    @Override public short fixed_digits() throws BadKind { throw new BadKind(); }
    @Override public short fixed_scale() throws BadKind { throw new BadKind(); }
    @Override public short member_visibility(int index) throws BadKind, Bounds { throw new BadKind(); }
    @Override public short type_modifier() throws BadKind { throw new BadKind(); }
    @Override public TypeCode concrete_base_type() throws BadKind { throw new BadKind(); }

    // Equivalence checking with recursion handling

    @Override
    public boolean equivalent(TypeCode t) {
        List<TypeCode> history = new ArrayList<>();
        List<TypeCode> otherHistory = new ArrayList<>();

        boolean result = equivalentRecHelper(t, history, otherHistory);

        ensure(history.isEmpty());
        ensure(otherHistory.isEmpty());

        return result;
    }

    protected boolean equivalentRecHelper(TypeCode t, List<TypeCode> history, List<TypeCode> otherHistory) {
        if (t == null) return false;
        if (t == this) return true;

        // Avoid infinite loops
        {
            final boolean foundLoop = history.stream().anyMatch(typeCode -> this == typeCode);
            final boolean foundOtherLoop = otherHistory.stream().anyMatch(typeCode -> t == typeCode);
            if (foundLoop && foundOtherLoop) return true;
        }

        history.add(this);
        otherHistory.add(t);

        boolean result = equivalentRec(t, history, otherHistory);

        history.remove(history.size() - 1);
        otherHistory.remove(otherHistory.size() - 1);

        return result;
    }

    protected abstract boolean equivalentRec(TypeCode t, List<TypeCode> history, List<TypeCode> otherHistory);

    // Compact TypeCode generation with recursion handling

    @Override
    public TypeCode get_compact_typecode() {
        List<TypeCode> history = new ArrayList<>();
        List<TypeCode> compacted = new ArrayList<>();

        return getCompactTypeCodeRec(history, compacted);
    }

    protected abstract YokoTypeCode getCompactTypeCodeRec(List<TypeCode> history, List<TypeCode> compacted);

    // Equality checking

    @Override
    public abstract boolean equal(TypeCode other);

    // Factory method to convert TypeCode to YokoTypeCode

    /**
     * Converts a TypeCode to a YokoTypeCode. If the TypeCode is already a YokoTypeCode,
     * returns it as-is. Otherwise, creates a new YokoTypeCode instance by copying
     * the structure of the foreign TypeCode.
     *
     * @param tc the TypeCode to convert
     * @return a YokoTypeCode instance
     * @throws org.omg.CORBA.BAD_TYPECODE if the TypeCode is invalid
     */
    public static YokoTypeCode from(TypeCode tc) {
        if (tc instanceof YokoTypeCode) return (YokoTypeCode) tc;

        // TODO: Implement full conversion from foreign TypeCodes
        // For now, this will be implemented when TypeCodeFactory is migrated
        throw new UnsupportedOperationException(
            "Conversion from foreign TypeCode not yet implemented");
    }
}
