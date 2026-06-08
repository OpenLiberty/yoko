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
package org.apache.yoko.orb.CORBA.any;

import org.apache.yoko.orb.CORBA.YokoInputStream;
import org.apache.yoko.orb.CORBA.YokoOutputStream;
import org.apache.yoko.orb.CORBA.TypeCodeImpl;

import org.apache.yoko.orb.OB.ORBInstance;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.DATA_CONVERSION;
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.Streamable;
import org.omg.CORBA_2_3.portable.InputStream;
import org.omg.CORBA_2_3.portable.OutputStream;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Collections.unmodifiableMap;
import static org.apache.yoko.orb.OB.TypeCodeFactory.createPrimitiveTC;

/**
 * New Any implementation using YokoAnyData for type-safe value storage.
 * Delegates all operations to immutable YokoAnyData instances.
 */
public final class YokoAny extends Any {
    private final ORBInstance orbInstance;
    private YokoAnyData<?> data;

    // Factory map for creating YokoAnyData from InputStream
    private static final Map<Integer, BiFunction<InputStream, TypeCode, YokoAnyData<?>>> FROM_FACTORIES;

    static {
        Map<Integer, BiFunction<InputStream, TypeCode, YokoAnyData<?>>> map = new HashMap<>();
        map.put(TCKind._tk_null, NullAnyData::from);
        map.put(TCKind._tk_void, NullAnyData::from);
        map.put(TCKind._tk_short, ShortAnyData::from);
        map.put(TCKind._tk_long, LongAnyData::from);
        map.put(TCKind._tk_longlong, LongLongAnyData::from);
        map.put(TCKind._tk_ushort, UShortAnyData::from);
        map.put(TCKind._tk_ulong, ULongAnyData::from);
        map.put(TCKind._tk_ulonglong, ULongLongAnyData::from);
        map.put(TCKind._tk_float, FloatAnyData::from);
        map.put(TCKind._tk_double, DoubleAnyData::from);
        map.put(TCKind._tk_boolean, BooleanAnyData::from);
        map.put(TCKind._tk_char, CharAnyData::from);
        map.put(TCKind._tk_wchar, WCharAnyData::from);
        map.put(TCKind._tk_octet, OctetAnyData::from);
        map.put(TCKind._tk_string, StringAnyData::from);
        map.put(TCKind._tk_wstring, WStringAnyData::from);
        map.put(TCKind._tk_fixed, FixedAnyData::from);
        map.put(TCKind._tk_enum, EnumAnyData::from);
        map.put(TCKind._tk_TypeCode, TypeCodeAnyData::from);
        map.put(TCKind._tk_any, AnyAnyData::from);
        map.put(TCKind._tk_objref, ObjectAnyData::from);
        map.put(TCKind._tk_value, ValueAnyData::from);
        map.put(TCKind._tk_value_box, ValueAnyData::from);
        map.put(TCKind._tk_abstract_interface, AbstractInterfaceAnyData::from);
        // Complex types that wrap the stream directly
        map.put(TCKind._tk_struct, StreamWrapperAnyData::from);
        map.put(TCKind._tk_except, StreamWrapperAnyData::from);
        map.put(TCKind._tk_union, StreamWrapperAnyData::from);
        map.put(TCKind._tk_sequence, StreamWrapperAnyData::from);
        map.put(TCKind._tk_array, StreamWrapperAnyData::from);
        // Unsupported types that should throw exceptions
        map.put(TCKind._tk_Principal, (is, tc) -> { throw new BAD_OPERATION("TypeCode kind tk_Principal is deprecated and not supported"); });
        map.put(TCKind._tk_alias, (is, tc) -> { throw new BAD_OPERATION("TypeCode kind tk_alias should be resolved to its actual type before reading"); });
        map.put(TCKind._tk_longdouble, (is, tc) -> { throw new BAD_OPERATION("TypeCode kind tk_longdouble is not supported"); });
        map.put(TCKind._tk_native, (is, tc) -> { throw new BAD_OPERATION("TypeCode kind tk_native is not supported"); });
        FROM_FACTORIES = unmodifiableMap(map);
    }

    // Extraction map for legacy Any implementations
    private static final Map<Integer, Function<Any, Object>> LEGACY_EXTRACTORS;

    static {
        Map<Integer, Function<Any, Object>> map = new HashMap<>();
        map.put(TCKind._tk_null, a -> null);
        map.put(TCKind._tk_void, a -> null);
        map.put(TCKind._tk_short, Any::extract_short);
        map.put(TCKind._tk_long, Any::extract_long);
        map.put(TCKind._tk_longlong, Any::extract_longlong);
        map.put(TCKind._tk_ushort, Any::extract_ushort);
        map.put(TCKind._tk_ulong, Any::extract_ulong);
        map.put(TCKind._tk_ulonglong, Any::extract_ulonglong);
        map.put(TCKind._tk_float, Any::extract_float);
        map.put(TCKind._tk_double, Any::extract_double);
        map.put(TCKind._tk_boolean, Any::extract_boolean);
        map.put(TCKind._tk_char, Any::extract_char);
        map.put(TCKind._tk_wchar, Any::extract_wchar);
        map.put(TCKind._tk_octet, Any::extract_octet);
        map.put(TCKind._tk_string, Any::extract_string);
        map.put(TCKind._tk_wstring, Any::extract_wstring);
        map.put(TCKind._tk_fixed, Any::extract_fixed);
        map.put(TCKind._tk_enum, Any::extract_ulong);
        map.put(TCKind._tk_TypeCode, Any::extract_TypeCode);
        map.put(TCKind._tk_any, Any::extract_any);
        map.put(TCKind._tk_objref, Any::extract_Object);
        map.put(TCKind._tk_value, Any::extract_Value);
        map.put(TCKind._tk_value_box, Any::extract_Value);
        map.put(TCKind._tk_abstract_interface, Any::extract_Value);
        // Complex types that use Streamable
        map.put(TCKind._tk_struct, Any::extract_Streamable);
        map.put(TCKind._tk_except, Any::extract_Streamable);
        map.put(TCKind._tk_union, Any::extract_Streamable);
        map.put(TCKind._tk_sequence, Any::extract_Streamable);
        map.put(TCKind._tk_array, Any::extract_Streamable);
        // Unsupported types that should throw exceptions
        map.put(TCKind._tk_Principal, a -> { throw new BAD_OPERATION("TypeCode kind tk_Principal is deprecated and not supported"); });
        map.put(TCKind._tk_alias, a -> { throw new BAD_OPERATION("TypeCode kind tk_alias should be resolved to its actual type before extraction"); });
        map.put(TCKind._tk_longdouble, a -> { throw new BAD_OPERATION("TypeCode kind tk_longdouble is not supported"); });
        map.put(TCKind._tk_native, a -> { throw new BAD_OPERATION("TypeCode kind tk_native is not supported"); });
        LEGACY_EXTRACTORS = unmodifiableMap(map);
    }

    public YokoAny(ORBInstance orbInstance) {
        this.orbInstance = orbInstance;
        this.data = NullAnyData.NULL;
    }

    /**
     * Converts an arbitrary InputStream to a YokoInputStream with defensive copying.
     * If the input is already a YokoInputStream, creates a defensive copy.
     * Otherwise, wraps it by writing to an output stream and creating an input stream.
     */
    private YokoInputStream toYokoInputStream(InputStream is, TypeCode t) {
        if (is instanceof YokoInputStream) {
            return new YokoInputStream((YokoInputStream) is);
        }

        try (YokoOutputStream out = (YokoOutputStream) create_output_stream()) {
            out.write_InputStream(is, t);
            return out.create_input_stream();
        }
    }

    @Override
    public boolean equal(Any a) {
        if (a == null) return false;
        if (this == a) return true;
        if (!type().equal(a.type())) return false;

        // For StreamWrapperAnyData (complex types), compare stream buffers
        if (data instanceof StreamWrapperAnyData) {
            try {
                try (YokoInputStream thisStream = (YokoInputStream) create_input_stream();
                    YokoInputStream otherStream = (YokoInputStream) a.create_input_stream()) {
                    return thisStream.getBuffer().dataEquals(otherStream.getBuffer());
                }
            } catch (Exception e) {
                return false;
            }
        }

        // Compare values by extracting from both
        try {
            Object thisValue = data.extract();
            Object otherValue = (a instanceof YokoAny)
                ? ((YokoAny) a).data.extract()
                : extractValueFromLegacyAny(a);

            if (thisValue == null) return otherValue == null;
            return thisValue.equals(otherValue);
        } catch (Exception e) {
            return false;
        }
    }

    private Object extractValueFromLegacyAny(Any a) {
        int kind = a.type().kind().value();
        return Optional.ofNullable(LEGACY_EXTRACTORS.get(kind))
            .map(extractor -> extractor.apply(a))
            .orElseThrow(() -> new BAD_OPERATION("Unsupported TypeCode kind for legacy Any extraction: " + kind));
    }

    @Override
    public TypeCode type() {
        return data.type();
    }

    @Override
    public void type(TypeCode t) {
        data = NullAnyData.of(t);
    }

    @Override
    public void read_value(org.omg.CORBA.portable.InputStream is, TypeCode t) { read_value((InputStream) is, t); }
    private void read_value(InputStream is, TypeCode t) throws MARSHAL {
        // Unwrap alias TypeCodes to get the original kind (e.g., CharSeq is an alias for sequence)
        TypeCode origType = TypeCodeImpl._OB_getOrigType(t);
        int kind = origType.kind().value();
        YokoInputStream yokoInputStream = toYokoInputStream(is, t);

        data = Optional.ofNullable(FROM_FACTORIES.get(kind))
            .map(factory -> factory.apply(yokoInputStream, t))
            .orElseThrow(() -> new BAD_OPERATION("Unsupported TypeCode kind for read_value: " + kind));
    }

    @Override
    public void write_value(org.omg.CORBA.portable.OutputStream os) { write_value((OutputStream) os); }
    private void write_value(OutputStream os) { data.write(os); }

    @Override
    public OutputStream create_output_stream() {
        YokoOutputStream out = new YokoOutputStream();
        out._OB_ORBInstance(orbInstance);
        return out;
    }

    @Override
    public InputStream create_input_stream() { return data.toInputStream(this::create_output_stream); }

    // Extract methods

    @Override public short extract_short() throws BAD_OPERATION { return data.extract_short(); }
    @Override public int extract_long() throws BAD_OPERATION { return data.extract_long(); }
    @Override public long extract_longlong() throws BAD_OPERATION { return data.extract_longlong(); }
    @Override public short extract_ushort() throws BAD_OPERATION { return data.extract_ushort(); }
    @Override public int extract_ulong() throws BAD_OPERATION { return data.extract_ulong(); }
    @Override public long extract_ulonglong() throws BAD_OPERATION { return data.extract_ulonglong(); }
    @Override public float extract_float() throws BAD_OPERATION { return data.extract_float(); }
    @Override public double extract_double() throws BAD_OPERATION { return data.extract_double(); }
    @Override public boolean extract_boolean() throws BAD_OPERATION { return data.extract_boolean(); }
    @Override public char extract_char() throws BAD_OPERATION { return data.extract_char(); }
    @Override public char extract_wchar() throws BAD_OPERATION { return data.extract_wchar(); }
    @Override public byte extract_octet() throws BAD_OPERATION { return data.extract_octet(); }
    @Override public Any extract_any() throws BAD_OPERATION { return data.extract_any(); }
    @Override public org.omg.CORBA.Object extract_Object() throws BAD_OPERATION { return data.extract_Object(); }
    @Override public String extract_string() throws BAD_OPERATION { return data.extract_string(); }
    @Override public String extract_wstring() throws BAD_OPERATION { return data.extract_wstring(); }
    @Override public TypeCode extract_TypeCode() throws BAD_OPERATION { return data.extract_TypeCode(); }
    @Override public Streamable extract_Streamable() {
        return data.extract_Streamable();
    }
    @Override public BigDecimal extract_fixed() {
        return data.extract_fixed();
    }
    @Override public Serializable extract_Value() throws BAD_OPERATION { return data.extract_Value(); }

    // Insert methods

    @Override public void insert_short(short s) {
        data = ShortAnyData.of(s);
    }
    @Override public void insert_long(int i) {
        data = LongAnyData.of(i);
    }
    @Override public void insert_longlong(long l) {
        data = LongLongAnyData.of(l);
    }
    @Override public void insert_ushort(short s) {
        data = UShortAnyData.of(s);
    }
    @Override public void insert_ulong(int i) {
        data = ULongAnyData.of(i);
    }
    @Override public void insert_ulonglong(long l) {
        data = ULongLongAnyData.of(l);
    }
    @Override public void insert_float(float f) {
        data = FloatAnyData.of(f);
    }
    @Override public void insert_double(double d) {
        data = DoubleAnyData.of(d);
    }
    @Override public void insert_boolean(boolean b) {
        data = BooleanAnyData.of(b);
    }
    @Override public void insert_char(char c) throws DATA_CONVERSION { data = CharAnyData.of(c); }
    @Override public void insert_wchar(char c) throws DATA_CONVERSION { data = WCharAnyData.of(c); }
    @Override public void insert_octet(byte b) {
        data = OctetAnyData.of(b);
    }
    @Override public void insert_any(Any a) {
        data = AnyAnyData.of(a, this::create_output_stream);
    }
    @Override public void insert_Object(org.omg.CORBA.Object o) { data = ObjectAnyData.of(o, this::create_output_stream); }
    @Override public void insert_Object(org.omg.CORBA.Object o, TypeCode t) { data = ObjectAnyData.of(o, t, this::create_output_stream); }
    @Override public void insert_string(String s) throws DATA_CONVERSION, MARSHAL { data = StringAnyData.of(s); }
    @Override public void insert_wstring(String s) throws MARSHAL { data = WStringAnyData.of(s); }
    @Override public void insert_TypeCode(TypeCode t) {
        data = TypeCodeAnyData.of(t, this::create_output_stream);
    }
    @Override public void insert_Streamable(Streamable s) { data = StreamableAnyData.of(s, s._type(), this::create_output_stream); }
    @Override public void insert_fixed(BigDecimal value) { data = FixedAnyData.of(value, createPrimitiveTC(TCKind.tk_fixed), this::create_output_stream); }
    @Override public void insert_fixed(BigDecimal value, TypeCode type) { data = FixedAnyData.of(value, type, this::create_output_stream); }
    @Override public void insert_Value(Serializable v) { data = ValueAnyData.of(v, createPrimitiveTC(TCKind.tk_value), this::create_output_stream); }
    @Override public void insert_Value(Serializable v, TypeCode t) throws MARSHAL { data = ValueAnyData.of(v, t, this::create_output_stream); }
}
