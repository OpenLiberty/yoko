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
import org.apache.yoko.orb.CORBA.typecode.YokoTypeCode;
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
import static org.apache.yoko.orb.CORBA.typecode.YokoTypeCode.getPrimitive;
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
import static org.omg.CORBA.TCKind.tk_Principal;
import static org.omg.CORBA.TCKind.tk_sequence;
import static org.omg.CORBA.TCKind.tk_short;
import static org.omg.CORBA.TCKind.tk_string;
import static org.omg.CORBA.TCKind.tk_struct;
import static org.omg.CORBA.TCKind.tk_TypeCode;
import static org.omg.CORBA.TCKind.tk_ulong;
import static org.omg.CORBA.TCKind.tk_ulonglong;
import static org.omg.CORBA.TCKind.tk_union;
import static org.omg.CORBA.TCKind.tk_ushort;
import static org.omg.CORBA.TCKind.tk_value;
import static org.omg.CORBA.TCKind.tk_value_box;
import static org.omg.CORBA.TCKind.tk_void;
import static org.omg.CORBA.TCKind.tk_wchar;
import static org.omg.CORBA.TCKind.tk_wstring;

/**
 * New Any implementation using YokoAnyData for type-safe value storage.
 * Delegates all operations to immutable YokoAnyData instances.
 */
public final class YokoAny extends Any {
    private final ORBInstance orbInstance;
    private YokoAnyData<?> data;

    // Factory map for creating YokoAnyData from InputStream
    private static final Map<TCKind, BiFunction<InputStream, TypeCode, YokoAnyData<?>>> FROM_FACTORIES;

    static {
        Map<TCKind, BiFunction<InputStream, TypeCode, YokoAnyData<?>>> map = new HashMap<>();
        map.put(tk_null, NullAnyData::from);
        map.put(tk_void, NullAnyData::from);
        map.put(tk_short, ShortAnyData::from);
        map.put(tk_long, LongAnyData::from);
        map.put(tk_longlong, LongLongAnyData::from);
        map.put(tk_ushort, UShortAnyData::from);
        map.put(tk_ulong, ULongAnyData::from);
        map.put(tk_ulonglong, ULongLongAnyData::from);
        map.put(tk_float, FloatAnyData::from);
        map.put(tk_double, DoubleAnyData::from);
        map.put(tk_boolean, BooleanAnyData::from);
        map.put(tk_char, CharAnyData::from);
        map.put(tk_wchar, WCharAnyData::from);
        map.put(tk_octet, OctetAnyData::from);
        map.put(tk_string, StringAnyData::from);
        map.put(tk_wstring, WStringAnyData::from);
        map.put(tk_fixed, FixedAnyData::from);
        map.put(tk_enum, EnumAnyData::from);
        map.put(tk_TypeCode, TypeCodeAnyData::from);
        map.put(tk_any, AnyAnyData::from);
        map.put(tk_objref, ObjectAnyData::from);
        map.put(tk_value, ValueAnyData::from);
        map.put(tk_value_box, ValueAnyData::from);
        map.put(tk_abstract_interface, AbstractInterfaceAnyData::from);
        // Complex types that wrap the stream directly
        map.put(tk_struct, StreamWrapperAnyData::from);
        map.put(tk_except, StreamWrapperAnyData::from);
        map.put(tk_union, StreamWrapperAnyData::from);
        map.put(tk_sequence, StreamWrapperAnyData::from);
        map.put(tk_array, StreamWrapperAnyData::from);
        // Unsupported types that should throw exceptions
        map.put(tk_Principal, (is, tc) -> { throw new BAD_OPERATION("TypeCode kind tk_Principal is deprecated and not supported"); });
        map.put(tk_alias, (is, tc) -> { throw new BAD_OPERATION("TypeCode kind tk_alias should be resolved to its actual type before reading"); });
        map.put(tk_longdouble, (is, tc) -> { throw new BAD_OPERATION("TypeCode kind tk_longdouble is not supported"); });
        map.put(tk_native, (is, tc) -> { throw new BAD_OPERATION("TypeCode kind tk_native is not supported"); });
        FROM_FACTORIES = unmodifiableMap(map);
    }

    // Extraction map for legacy Any implementations
    private static final Map<TCKind, Function<Any, Object>> LEGACY_EXTRACTORS;

    static {
        Map<TCKind, Function<Any, Object>> map = new HashMap<>();
        map.put(tk_null, a -> null);
        map.put(tk_void, a -> null);
        map.put(tk_short, Any::extract_short);
        map.put(tk_long, Any::extract_long);
        map.put(tk_longlong, Any::extract_longlong);
        map.put(tk_ushort, Any::extract_ushort);
        map.put(tk_ulong, Any::extract_ulong);
        map.put(tk_ulonglong, Any::extract_ulonglong);
        map.put(tk_float, Any::extract_float);
        map.put(tk_double, Any::extract_double);
        map.put(tk_boolean, Any::extract_boolean);
        map.put(tk_char, Any::extract_char);
        map.put(tk_wchar, Any::extract_wchar);
        map.put(tk_octet, Any::extract_octet);
        map.put(tk_string, Any::extract_string);
        map.put(tk_wstring, Any::extract_wstring);
        map.put(tk_fixed, Any::extract_fixed);
        map.put(tk_enum, Any::extract_ulong);
        map.put(tk_TypeCode, Any::extract_TypeCode);
        map.put(tk_any, Any::extract_any);
        map.put(tk_objref, Any::extract_Object);
        map.put(tk_value, Any::extract_Value);
        map.put(tk_value_box, Any::extract_Value);
        map.put(tk_abstract_interface, Any::extract_Value);
        // Complex types that use Streamable
        map.put(tk_struct, Any::extract_Streamable);
        map.put(tk_except, Any::extract_Streamable);
        map.put(tk_union, Any::extract_Streamable);
        map.put(tk_sequence, Any::extract_Streamable);
        map.put(tk_array, Any::extract_Streamable);
        // Unsupported types that should throw exceptions
        map.put(tk_Principal, a -> { throw new BAD_OPERATION("TypeCode kind tk_Principal is deprecated and not supported"); });
        map.put(tk_alias, a -> { throw new BAD_OPERATION("TypeCode kind tk_alias should be resolved to its actual type before extraction"); });
        map.put(tk_longdouble, a -> { throw new BAD_OPERATION("TypeCode kind tk_longdouble is not supported"); });
        map.put(tk_native, a -> { throw new BAD_OPERATION("TypeCode kind tk_native is not supported"); });
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
        TCKind kind = a.type().kind();
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
        TypeCode origType = YokoTypeCode.from(t).getOrigType();
        TCKind kind = origType.kind();
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
    @Override public void insert_fixed(BigDecimal value) { data = FixedAnyData.of(value, getPrimitive(TCKind.tk_fixed), this::create_output_stream); }
    @Override public void insert_fixed(BigDecimal value, TypeCode type) { data = FixedAnyData.of(value, type, this::create_output_stream); }
    @Override public void insert_Value(Serializable v) { data = ValueAnyData.of(v, getPrimitive(TCKind.tk_value), this::create_output_stream); }
    @Override public void insert_Value(Serializable v, TypeCode t) throws MARSHAL { data = ValueAnyData.of(v, t, this::create_output_stream); }
}
