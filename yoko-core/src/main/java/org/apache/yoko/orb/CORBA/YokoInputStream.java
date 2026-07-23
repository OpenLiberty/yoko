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
package org.apache.yoko.orb.CORBA;

import org.apache.yoko.codecs.CharCodec;
import org.apache.yoko.codecs.WcharCodec;
import org.apache.yoko.codecs.WcharCodec.WcharReader;
import org.apache.yoko.io.AlignmentBoundary;
import org.apache.yoko.io.Buffer;
import org.apache.yoko.io.ReadBuffer;
import org.apache.yoko.orb.OB.CodeBaseProxy;
import org.apache.yoko.orb.OB.CodecPair;
import org.apache.yoko.orb.OB.ORBInstance;
import org.apache.yoko.orb.OB.ObjectFactory;
import org.apache.yoko.orb.OB.TypeCodeCache;
import org.apache.yoko.orb.OB.ValueReader;
import org.apache.yoko.orb.OCI.GiopVersion;
import org.apache.yoko.rmi.impl.InputStreamWithOffsets;
import org.apache.yoko.util.Assert;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_TYPECODE;
import org.omg.CORBA.INITIALIZE;
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.Principal;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.BoxedValueHelper;
import org.omg.CORBA.portable.IDLEntity;
import org.omg.CORBA.portable.ObjectImpl;
import org.omg.CORBA_2_4.TCKind;
import org.omg.IOP.IOR;
import org.omg.IOP.IORHelper;
import org.omg.SendingContext.CodeBase;

import javax.rmi.CORBA.Util;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.security.PrivilegedActionException;
import java.util.Hashtable;

import static java.security.AccessController.doPrivileged;
import static java.util.logging.Level.FINE;
import static java.util.stream.IntStream.range;
import static org.apache.yoko.io.AlignmentBoundary.EIGHT_BYTE_BOUNDARY;
import static org.apache.yoko.io.AlignmentBoundary.FOUR_BYTE_BOUNDARY;
import static org.apache.yoko.io.AlignmentBoundary.TWO_BYTE_BOUNDARY;
import static org.apache.yoko.logging.VerboseLogging.GIOP_IN_LOG;
import static org.apache.yoko.orb.OB.TypeCodeFactory.createAbstractInterfaceTC;
import static org.apache.yoko.orb.OB.TypeCodeFactory.createAliasTC;
import static org.apache.yoko.orb.OB.TypeCodeFactory.createEnumTC;
import static org.apache.yoko.orb.OB.TypeCodeFactory.createFixedTC;
import static org.apache.yoko.orb.OB.TypeCodeFactory.createInterfaceTC;
import static org.apache.yoko.orb.OB.TypeCodeFactory.createLocalInterfaceTC;
import static org.apache.yoko.orb.OB.TypeCodeFactory.createNativeTC;
import static org.apache.yoko.orb.OB.TypeCodeFactory.createPrimitiveTC;
import static org.apache.yoko.orb.OB.TypeCodeFactory.createStringTC;
import static org.apache.yoko.orb.OB.TypeCodeFactory.createValueBoxTC;
import static org.apache.yoko.orb.OB.TypeCodeFactory.createWStringTC;
import static org.apache.yoko.orb.OCI.GiopVersion.GIOP1_0;
import static org.apache.yoko.util.Assert.ensure;
import static org.apache.yoko.util.MinorCodes.MinorInvalidUnionDiscriminator;
import static org.apache.yoko.util.MinorCodes.MinorLoadStub;
import static org.apache.yoko.util.MinorCodes.MinorReadBooleanArrayOverflow;
import static org.apache.yoko.util.MinorCodes.MinorReadBooleanOverflow;
import static org.apache.yoko.util.MinorCodes.MinorReadCharOverflow;
import static org.apache.yoko.util.MinorCodes.MinorReadDoubleArrayOverflow;
import static org.apache.yoko.util.MinorCodes.MinorReadDoubleOverflow;
import static org.apache.yoko.util.MinorCodes.MinorReadFixedInvalid;
import static org.apache.yoko.util.MinorCodes.MinorReadFloatArrayOverflow;
import static org.apache.yoko.util.MinorCodes.MinorReadFloatOverflow;
import static org.apache.yoko.util.MinorCodes.MinorReadInvTypeCodeIndirection;
import static org.apache.yoko.util.MinorCodes.MinorReadLongArrayOverflow;
import static org.apache.yoko.util.MinorCodes.MinorReadLongLongArrayOverflow;
import static org.apache.yoko.util.MinorCodes.MinorReadLongLongOverflow;
import static org.apache.yoko.util.MinorCodes.MinorReadLongOverflow;
import static org.apache.yoko.util.MinorCodes.MinorReadOctetArrayOverflow;
import static org.apache.yoko.util.MinorCodes.MinorReadOctetOverflow;
import static org.apache.yoko.util.MinorCodes.MinorReadOverflow;
import static org.apache.yoko.util.MinorCodes.MinorReadShortArrayOverflow;
import static org.apache.yoko.util.MinorCodes.MinorReadShortOverflow;
import static org.apache.yoko.util.MinorCodes.MinorReadStringNoTerminator;
import static org.apache.yoko.util.MinorCodes.MinorReadStringOverflow;
import static org.apache.yoko.util.MinorCodes.MinorReadStringZeroLength;
import static org.apache.yoko.util.MinorCodes.MinorReadULongArrayOverflow;
import static org.apache.yoko.util.MinorCodes.MinorReadULongLongArrayOverflow;
import static org.apache.yoko.util.MinorCodes.MinorReadULongLongOverflow;
import static org.apache.yoko.util.MinorCodes.MinorReadULongOverflow;
import static org.apache.yoko.util.MinorCodes.MinorReadUShortArrayOverflow;
import static org.apache.yoko.util.MinorCodes.MinorReadWCharOverflow;
import static org.apache.yoko.util.MinorCodes.MinorReadWStringNoTerminator;
import static org.apache.yoko.util.MinorCodes.MinorReadWStringOverflow;
import static org.apache.yoko.util.MinorCodes.MinorReadWStringZeroLength;
import static org.apache.yoko.util.MinorCodes.describeBadTypecode;
import static org.apache.yoko.util.MinorCodes.describeMarshal;
import static org.apache.yoko.util.PrivilegedActions.getClassLoader;
import static org.apache.yoko.util.PrivilegedActions.getMethod;
import static org.apache.yoko.util.PrivilegedActions.getNoArgConstructor;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;
import static org.omg.CORBA.TCKind._tk_Principal;
import static org.omg.CORBA.TCKind._tk_TypeCode;
import static org.omg.CORBA.TCKind._tk_abstract_interface;
import static org.omg.CORBA.TCKind._tk_alias;
import static org.omg.CORBA.TCKind._tk_any;
import static org.omg.CORBA.TCKind._tk_array;
import static org.omg.CORBA.TCKind._tk_boolean;
import static org.omg.CORBA.TCKind._tk_char;
import static org.omg.CORBA.TCKind._tk_double;
import static org.omg.CORBA.TCKind._tk_enum;
import static org.omg.CORBA.TCKind._tk_except;
import static org.omg.CORBA.TCKind._tk_fixed;
import static org.omg.CORBA.TCKind._tk_float;
import static org.omg.CORBA.TCKind._tk_long;
import static org.omg.CORBA.TCKind._tk_longdouble;
import static org.omg.CORBA.TCKind._tk_longlong;
import static org.omg.CORBA.TCKind._tk_native;
import static org.omg.CORBA.TCKind._tk_null;
import static org.omg.CORBA.TCKind._tk_objref;
import static org.omg.CORBA.TCKind._tk_octet;
import static org.omg.CORBA.TCKind._tk_sequence;
import static org.omg.CORBA.TCKind._tk_short;
import static org.omg.CORBA.TCKind._tk_string;
import static org.omg.CORBA.TCKind._tk_struct;
import static org.omg.CORBA.TCKind._tk_ulong;
import static org.omg.CORBA.TCKind._tk_ulonglong;
import static org.omg.CORBA.TCKind._tk_union;
import static org.omg.CORBA.TCKind._tk_ushort;
import static org.omg.CORBA.TCKind._tk_value;
import static org.omg.CORBA.TCKind._tk_value_box;
import static org.omg.CORBA.TCKind._tk_void;
import static org.omg.CORBA.TCKind._tk_wchar;
import static org.omg.CORBA.TCKind._tk_wstring;
import static org.omg.CORBA.TCKind.tk_union;
import static org.omg.CORBA_2_4.TCKind._tk_local_interface;

final public class YokoInputStream extends InputStreamWithOffsets {
    private ORBInstance orbInstance;

    private final ReadBuffer readBuffer;

    boolean swapBytes;

    private GiopVersion giopVersion = GIOP1_0;

    private final int origPos;

    private final boolean origSwap;

    //
    // Handles all OBV marshaling
    //
    private ValueReader valueReader;

    private TypeCodeCache cache;

    private CodecPair codecs;

    private CodeBase sendingContextRuntime;

    private String codebase;

    // ------------------------------------------------------------------
    // Private and protected members
    // ------------------------------------------------------------------

    private TypeCodeImpl checkCache(String id, int startPos, int length) {
        TypeCodeImpl tc = null;

        if (!id.isEmpty()) {
            tc = cache.get(id);
            if (tc != null) {
                _OB_skip(length + startPos - readBuffer.getPosition());
            }
        }

        return tc;
    }

    private TypeCode readTypeCodeImpl(Hashtable<Integer, TypeCodeImpl> history, boolean isTopLevel) {
        int kind = read_ulong();
        int oldPos = readBuffer.getPosition() - 4;
        GIOP_IN_LOG.finest(() -> String.format("Reading a TypeCode of kind %d from position 0x%x", kind, oldPos));

        TypeCodeImpl tc = null;
        if (kind == -1) {
            int offs = read_long();
            int indirectionPos = readBuffer.getPosition() - 4 + offs;
            indirectionPos += (indirectionPos & 0x3); // adjust for alignment
            TypeCodeImpl p = history.get(indirectionPos);
            if (p == null) {
                throw newMarshalError(MinorReadInvTypeCodeIndirection);
            }
            history.put(oldPos, p);
            tc = p;
        } else {
            switch (kind) {
                case _tk_null :
                case _tk_void :
                case _tk_short :
                case _tk_long :
                case _tk_ushort :
                case _tk_ulong :
                case _tk_float :
                case _tk_double :
                case _tk_boolean :
                case _tk_char :
                case _tk_octet :
                case _tk_any :
                case _tk_TypeCode :
                case _tk_Principal :
                case _tk_longlong :
                case _tk_ulonglong :
                case _tk_longdouble :
                case _tk_wchar :
                    tc = (TypeCodeImpl) createPrimitiveTC(TCKind.from_int(kind));
                    history.put(oldPos, tc);
                    break;

                case _tk_fixed : {
                    short digits = read_ushort();
                    short scale = read_short();
                    tc = (TypeCodeImpl) createFixedTC(digits, scale);
                    history.put(oldPos, tc);
                    break;
                }

                case _tk_objref : {
                    int length = read_ulong(); // encapsulation length
                    // save this position after the read, since we might be on a chunk boundary.
                    // however, we do an explicit check for the chunk boundary before doing the
                    // read.
                    checkChunk();
                    int typePos = readBuffer.getPosition();
                    boolean swap = this.swapBytes;
                    _OB_readEndian();

                    String id = read_string();

                    if (isTopLevel && cache != null)
                        tc = checkCache(id, typePos, length); // may advance pos
                    if (tc == null) {
                        tc = (TypeCodeImpl) createInterfaceTC(id, read_string());

                        if (!id.isEmpty() && cache != null)
                            cache.put(id, tc);
                    }

                    history.put(oldPos, tc);
                    this.swapBytes = swap;
                    break;
                }

                case _tk_struct :
                case _tk_except : {
                    int length = read_ulong(); // encapsulation length
                    // save this position after the read, since we might be on a chunk boundary.
                    // however, we do an explicit check for the chunk boundary before doing the
                    // read.
                    checkChunk();
                    int typePos = readBuffer.getPosition();
                    boolean swap = this.swapBytes;
                    _OB_readEndian();

                    String id = read_string();

                    if (isTopLevel && cache != null)
                        tc = checkCache(id, typePos, length); // may advance pos
                    if (tc == null) {
                        //
                        // For potentially recursive types, we must
                        // construct the TypeCode manually in order to
                        // add it to the history
                        //
                        TypeCodeImpl p = new TypeCodeImpl();
                        history.put(oldPos, p);
                        p.kind_ = TCKind.from_int(kind);
                        p.id_ = id;
                        p.name_ = read_string();
                        int num = read_ulong();
                        p.memberNames_ = new String[num];
                        p.memberTypes_ = new TypeCodeImpl[num];
                        for (int i = 0; i < num; i++) {
                            p.memberNames_[i] = read_string();
                            p.memberTypes_[i] = (TypeCodeImpl) readTypeCodeImpl(history, false);
                        }

                        tc = p;

                        if (!id.isEmpty() && cache != null)
                            cache.put(id, tc);
                    }

                    this.swapBytes = swap;
                    break;
                }

                case _tk_union : {
                    int length = read_ulong(); // encapsulation length
                    // save this position after the read, since we might be on a chunk boundary.
                    // however, we do an explicit check for the chunk boundary before doing the
                    // read.
                    checkChunk();
                    int typePos = readBuffer.getPosition();
                    boolean swap = this.swapBytes;
                    _OB_readEndian();

                    String id = read_string();

                    if (isTopLevel && cache != null)
                        tc = checkCache(id, typePos, length); // may advance pos
                    if (tc == null) {
                        //
                        // For potentially recursive types, we must construct
                        // the TypeCode manually in order to add it to the
                        // history
                        //
                        TypeCodeImpl p = new TypeCodeImpl();
                        history.put(oldPos, p);
                        p.kind_ = tk_union;
                        p.id_ = id;
                        p.name_ = read_string();
                        p.discriminatorType_ = (TypeCodeImpl) readTypeCodeImpl(history, false);
                        int defaultIndex = read_long();
                        int num = read_ulong();
                        p.labels_ = new AnyImpl[num];
                        p.memberNames_ = new String[num];
                        p.memberTypes_ = new TypeCodeImpl[num];

                        //
                        // Check the discriminator type
                        //
                        TypeCodeImpl origTC = p.discriminatorType_._OB_getOrigType();

                        switch (origTC.kind().value()) {
                            case _tk_short :
                            case _tk_ushort :
                            case _tk_long :
                            case _tk_ulong :
                            case _tk_longlong :
                            case _tk_ulonglong :
                            case _tk_boolean :
                            case _tk_char :
                            case _tk_enum :
                                break;
                            default :
                                //
                                // Invalid discriminator type
                                //
                                throw new BAD_TYPECODE(describeBadTypecode(MinorInvalidUnionDiscriminator), MinorInvalidUnionDiscriminator, COMPLETED_NO);
                        }

                        for (int i = 0; i < num; i++) {
                            p.labels_[i] = new AnyImpl();
                            if (i == defaultIndex) {
                                //
                                // Unmarshal a dummy value of the
                                // appropriate size for the
                                // discriminator type
                                //
                                AnyImpl dummy = new AnyImpl();
                                dummy.read_value(this, p.discriminatorType_);

                                //
                                // Default label value is the zero octet
                                //
                                p.labels_[i].insert_octet((byte) 0);
                            } else {
                                p.labels_[i].read_value(this, p.discriminatorType_);
                            }
                            p.memberNames_[i] = read_string();
                            p.memberTypes_[i] = (TypeCodeImpl) readTypeCodeImpl(history, false);
                        }

                        tc = p;

                        if (!id.isEmpty() && cache != null)
                            cache.put(id, tc);
                    }

                    this.swapBytes = swap;
                    break;
                }

                case _tk_enum : {
                    int length = read_ulong(); // encapsulation length
                    // save this position after the read, since we might be on a chunk boundary.
                    // however, we do an explicit check for the chunk boundary before doing the
                    // read.
                    checkChunk();
                    int typePos = readBuffer.getPosition();
                    boolean swap = this.swapBytes;
                    _OB_readEndian();

                    String id = read_string();

                    if (isTopLevel && cache != null)
                        tc = checkCache(id, typePos, length); // may advance pos
                    if (tc == null) {
                        String name = read_string();
                        int num = read_ulong();
                        String[] members = new String[num];
                        for (int i = 0; i < num; i++)
                            members[i] = read_string();
                        tc = (TypeCodeImpl) createEnumTC(id, name, members);
                        history.put(oldPos, tc);

                        if (!id.isEmpty() && cache != null)
                            cache.put(id, tc);
                    }

                    this.swapBytes = swap;
                    break;
                }

                case _tk_string : {
                    tc = (TypeCodeImpl) createStringTC(read_ulong());
                    history.put(oldPos, tc);
                    break;
                }

                case _tk_wstring : {
                    tc = (TypeCodeImpl) createWStringTC(read_ulong());
                    history.put(oldPos, tc);
                    break;
                }

                case _tk_sequence :
                case _tk_array : {
                    read_ulong(); // encapsulation length
                    boolean swap = this.swapBytes;
                    _OB_readEndian();

                    //
                    // For potentially recursive types, we must construct
                    // the TypeCode manually in order to add it to the
                    // history
                    //
                    TypeCodeImpl p = new TypeCodeImpl();
                    history.put(oldPos, p);
                    p.kind_ = TCKind.from_int(kind);
                    p.contentType_ = (TypeCodeImpl) readTypeCodeImpl(history, false);
                    p.length_ = read_ulong();

                    tc = p;

                    this.swapBytes = swap;
                    break;
                }

                case _tk_alias : {
                    int length = read_ulong(); // encapsulation length
                    // save this position after the read, since we might be on a chunk boundary.
                    // however, we do an explicit check for the chunk boundary before doing the
                    // read.
                    checkChunk();
                    int typePos = readBuffer.getPosition();
                    boolean swap = this.swapBytes;
                    _OB_readEndian();

                    String id = read_string();

                    if (isTopLevel && cache != null)
                        tc = checkCache(id, typePos, length); // may advance pos
                    if (tc == null) {
                        tc = (TypeCodeImpl) createAliasTC(id, read_string(), readTypeCodeImpl(history, false));

                        history.put(oldPos, tc);

                        if (!id.isEmpty() && cache != null)
                            cache.put(id, tc);
                    }

                    this.swapBytes = swap;
                    break;
                }

                case _tk_value : {
                    int length = read_ulong(); // encapsulation length
                    // save this position after the read, since we might be on a chunk boundary.
                    // however, we do an explicit check for the chunk boundary before doing the
                    // read.
                    checkChunk();
                    int typePos = readBuffer.getPosition();
                    boolean swap = this.swapBytes;
                    _OB_readEndian();

                    String id = read_string();

                    if (isTopLevel && cache != null)
                        tc = checkCache(id, typePos, length); // may advance pos
                    if (tc == null) {
                        //
                        // For potentially recursive types, we must
                        // construct the TypeCode manually in order to
                        // add it to the history
                        //
                        TypeCodeImpl p = new TypeCodeImpl();
                        history.put(oldPos, p);
                        p.kind_ = TCKind.from_int(kind);
                        p.id_ = id;
                        p.name_ = read_string();
                        p.typeModifier_ = read_short();
                        p.concreteBaseType_ = (TypeCodeImpl) readTypeCodeImpl(history, false);
                        if (p.concreteBaseType_.kind().value() == _tk_null)
                            p.concreteBaseType_ = null;
                        int num = read_ulong();
                        p.memberNames_ = new String[num];
                        p.memberTypes_ = new TypeCodeImpl[num];
                        p.memberVisibility_ = new short[num];
                        for (int i = 0; i < num; i++) {
                            p.memberNames_[i] = read_string();
                            p.memberTypes_[i] = (TypeCodeImpl) readTypeCodeImpl(history, false);
                            p.memberVisibility_[i] = read_short();
                        }

                        tc = p;

                        if (!id.isEmpty() && cache != null)
                            cache.put(id, tc);
                    }

                    this.swapBytes = swap;
                    break;
                }

                case _tk_value_box : {
                    int length = read_ulong(); // encapsulation length
                    // save this position after the read, since we might be on a chunk boundary.
                    // however, we do an explicit check for the chunk boundary before doing the
                    // read.
                    checkChunk();
                    int typePos = readBuffer.getPosition();
                    boolean swap = this.swapBytes;
                    _OB_readEndian();

                    String id = read_string();

                    if (isTopLevel && cache != null)
                        tc = checkCache(id, typePos, length); // may advance pos
                    if (tc == null) {
                        tc = (TypeCodeImpl) createValueBoxTC(id, read_string(), readTypeCodeImpl(history, false));
                        history.put(oldPos, tc);

                        if (!id.isEmpty() && cache != null)
                            cache.put(id, tc);
                    }

                    this.swapBytes = swap;
                    break;
                }

                case _tk_abstract_interface : {
                    int length = read_ulong(); // encapsulation length
                    // save this position after the read, since we might be on a chunk boundary.
                    // however, we do an explicit check for the chunk boundary before doing the
                    // read.
                    checkChunk();
                    int typePos = readBuffer.getPosition();
                    boolean swap = this.swapBytes;
                    _OB_readEndian();

                    String id = read_string();

                    GIOP_IN_LOG.fine(() -> String.format("Abstract interface typecode encapsulation length=0x%x id=%s", length, id));

                    if (isTopLevel && cache != null)
                        tc = checkCache(id, typePos, length); // may advance pos
                    if (tc == null) {
                        tc = (TypeCodeImpl) createAbstractInterfaceTC(id, read_string());
                        history.put(oldPos, tc);

                        if (!id.isEmpty() && cache != null)
                            cache.put(id, tc);
                    }

                    this.swapBytes = swap;
                    break;
                }

                case _tk_native : {
                    int length = read_ulong(); // encapsulation length
                    // save this position after the read, since we might be on a chunk boundary.
                    // however, we do an explicit check for the chunk boundary before doing the
                    // read.
                    checkChunk();
                    int typePos = readBuffer.getPosition();
                    boolean swap = this.swapBytes;
                    _OB_readEndian();

                    String id = read_string();

                    if (isTopLevel && cache != null)
                        tc = checkCache(id, typePos, length); // may advance pos
                    if (tc == null) {
                        tc = (TypeCodeImpl) createNativeTC(id, read_string());

                        if (!id.isEmpty() && cache != null)
                            cache.put(id, tc);
                    }

                    history.put(oldPos, tc);
                    this.swapBytes = swap;
                    break;
                }

                case TCKind._tk_local_interface : {
                    int length = read_ulong(); // encapsulation length
                    // save this position after the read, since we might be on a chunk boundary.
                    // however, we do an explicit check for the chunk boundary before doing the
                    // read.
                    checkChunk();
                    int typePos = readBuffer.getPosition();
                    boolean swap = this.swapBytes;
                    _OB_readEndian();

                    String id = read_string();

                    if (isTopLevel && cache != null)
                        tc = checkCache(id, typePos, length); // may advance pos
                    if (tc == null) {
                        tc = (TypeCodeImpl) createLocalInterfaceTC(id, read_string());
                        history.put(oldPos, tc);

                        if (!id.isEmpty() && cache != null)
                            cache.put(id, tc);
                    }

                    this.swapBytes = swap;
                    break;
                }

                default :
                    throw new BAD_TYPECODE(String.format("Unknown TypeCode kind: 0x%08x (%1$d)", kind));
            }
        }

        return tc;
    }

    private ValueReader valueReader() { return valueReader == null ? (valueReader = new ValueReader(this)) : valueReader; }

    public int available() {
        int available =  readBuffer.available();
        Assert.ensure(available >= 0);
        return available;
    }

    public int read() {
        checkChunk();
        return readBuffer.available() < 1 ? -1 : readBuffer.readByteAsChar();
    }

    public org.omg.CORBA.ORB orb() {
        return orbInstance == null ? null : orbInstance.getORB();
    }

    public boolean read_boolean() {
        checkChunk();

        try {
            int pos = readBuffer.getPosition();
            byte b = readBuffer.readByte();
            GIOP_IN_LOG.finest(() -> String.format("Boolean value is 0x%02x from position 0x%x", b, pos));
            return toBoolean(b);
        } catch (IndexOutOfBoundsException e) {
            throw newMarshalError((MinorReadBooleanOverflow), e);
        }
    }

    private static boolean toBoolean(byte b) {
        return b != (byte)0;
    }

    public char read_char() {
        checkChunk();
        try {
            return codecs.charCodec.readChar(readBuffer);
        } catch (IndexOutOfBoundsException e) {
            throw newMarshalError(MinorReadCharOverflow, e);
        }
    }

    public char read_wchar() {
        checkChunk();
        try {
            switch (giopVersion) {
                case GIOP1_0:
                case GIOP1_1:
                    readBuffer.align(TWO_BYTE_BOUNDARY);
                    return codecs.wcharCodec.readWchar_1_0(readBuffer, swapBytes);
                default:
                    return codecs.wcharCodec.readWchar_1_2(readBuffer);
            }
        } catch (IndexOutOfBoundsException e) {
            throw newMarshalError(MinorReadWCharOverflow, e);
        }
    }

    public byte read_octet() {
        checkChunk();
        try {
            return readBuffer.readByte();
        } catch (IndexOutOfBoundsException e) {
            throw newMarshalError((MinorReadOctetOverflow), e);
        }
    }

    public short read_short() {
        checkChunk();
        readBuffer.align(TWO_BYTE_BOUNDARY);
        try {
            return swapBytes ? readBuffer.readShort_LE() : readBuffer.readShort();
        } catch (IndexOutOfBoundsException e) {
            throw newMarshalError((MinorReadShortOverflow), e);
        }
    }

    public short read_ushort() {
        return read_short();
    }

    public int read_long() {
        checkChunk();
        return _OB_readLongUnchecked();
    }

    @Override
    public long position() { return readBuffer.getPosition(); }

    public int read_ulong() {
        try {
            return read_long();
        } catch (IndexOutOfBoundsException|MARSHAL e) {
            throw newMarshalError((MinorReadULongOverflow), e);
        }
    }

    public long read_longlong() {
        checkChunk();
        readBuffer.align(EIGHT_BYTE_BOUNDARY);
        try {
            return swapBytes ? readBuffer.readLong_LE() : readBuffer.readLong();
        } catch (IndexOutOfBoundsException e) {
            throw newMarshalError((MinorReadLongLongOverflow), e);
        }
    }

    public long read_ulonglong() {
        try {
            return read_longlong();
        } catch (IndexOutOfBoundsException|MARSHAL e) {
            throw newMarshalError((MinorReadULongLongOverflow), e);
        }
    }

    public float read_float() {
        checkChunk();
        readBuffer.align(FOUR_BYTE_BOUNDARY);
        try {
            return swapBytes ? readBuffer.readFloat_LE() : readBuffer.readFloat();
        } catch (IndexOutOfBoundsException e) {
            throw newMarshalError((MinorReadFloatOverflow), e);
        }
    }

    public double read_double() {
        checkChunk();
        readBuffer.align(EIGHT_BYTE_BOUNDARY);
        try {
            return swapBytes ? readBuffer.readDouble_LE() : readBuffer.readDouble();
        } catch (IndexOutOfBoundsException e) {
            throw newMarshalError((MinorReadDoubleOverflow), e);
        }
    }

    public String read_string() {
        checkChunk();

        // Number of octets (i.e. bytes) in the string (including the null terminator).
        // This may not be the same as the number of characters if encoding was done.
        int byteCount = read_ulong();
        if (byteCount == 0) throw stringMarshallingError("string", byteCount, MinorReadStringZeroLength);
        if (byteCount < 0) throw stringMarshallingError("string", byteCount, MinorReadStringOverflow);
        if (readBuffer.available() < byteCount) throw stringMarshallingError("string", byteCount, MinorReadStringOverflow);

        // Java strings don't need null terminators, so our string length will be at most one less than the byte count
        StringBuilder sb = new StringBuilder(byteCount - 1);

        final CharCodec codec = codecs.charCodec;
        GIOP_IN_LOG.finest(() -> String.format("Reading string value of length=0x%x using codec %s", byteCount, codec));

        final int endPosition = readBuffer.getPosition() + byteCount - 1;

        try {
            while (readBuffer.getPosition() < endPosition) {
                sb.append(codec.readChar(readBuffer));
            }
        } catch (IndexOutOfBoundsException e) {
            throw newMarshalError(MinorReadStringOverflow, e);
        }

        // throw MARSHAL if the converter read too many bytes
        if (readBuffer.getPosition() > endPosition) throw newMarshalError(MinorReadStringOverflow);
        // All the supported char codecs would use a single zero byte for a null character.
        // The COLLOCATED codec writes two-byte Java chars (even for non-wide char encoding)
        // but this is only ever to the same ORB, so it is ok to read a single zero byte
        // as long as this is written explicitly in write_string() as well as here.
        // (i.e. do NOT use the codec to write the null terminator)
        if (readBuffer.readByte() != 0) throw newMarshalError(MinorReadStringNoTerminator);
        GIOP_IN_LOG.fine(() -> String.format("Read string \"%s\", using %s codec end pos=0x%x", sb, codec, readBuffer.getPosition()));
        return sb.toString();
    }

    private MARSHAL stringMarshallingError(String stringDesc, int length, int minor) {
        return stringMarshallingError(stringDesc, length, minor, null);
    }
    private MARSHAL stringMarshallingError(String stringDesc, int length, int minor, Exception e) {
        MARSHAL marshal = null == e ? newMarshalError(minor): newMarshalError(minor, e);
        GIOP_IN_LOG.severe(String.format("Error reading %s of length %d: %s%n%s",
                stringDesc, length, marshal.getMessage(), readBuffer.dumpAllDataWithPosition()));
        return marshal;
    }

    public String read_wstring() {
        checkChunk();
        switch (giopVersion) {
            case GIOP1_0:
            case GIOP1_1:
                return read_wstring_pre_1_2();
            default:
                return read_wstring_1_2();
        }
    }

    private String read_wstring_pre_1_2() {
        // read the length of the string (in characters for GIOP 1.0/1.1)
        int numChars = read_ulong();
        // it is not legal in GIOP 1.0/1.1 for a string to be 0 in length... it MUST have a null terminator
        if (numChars == 0) throw stringMarshallingError("GIOP 1.0 wstring", numChars, MinorReadWStringZeroLength);
        // in GIOP 1.0/1.1, every char must be encoded as EXACTLY two bytes
        if (numChars < 0) throw stringMarshallingError("GIOP 1.0 wstring", numChars, MinorReadWStringOverflow);
        if (readBuffer.available() < numChars * 2) throw stringMarshallingError("GIOP 1.0 wstring", numChars, MinorReadStringOverflow);

        final WcharCodec codec = codecs.wcharCodec;
        GIOP_IN_LOG.fine(() -> String.format("Reading GIOP 1.0 wstring of length %d chars using codec %s", numChars, codec));

        // in GIOP 1.0/1.1, there is no BOM - use the endianness from context
        char[] tmp = new char[numChars];

        try {
            for (int i = 0; i < numChars; i++) tmp[i] = codec.readWchar_1_0(readBuffer, swapBytes);
            // Check for terminating null wchar
            if (0 != tmp[numChars - 1]) throw stringMarshallingError("GIOP 1.0 wstring", numChars, MinorReadWStringNoTerminator);
            String result = new String(tmp, 0, numChars - 1);
            GIOP_IN_LOG.fine(() -> String.format("Read GIOP 1.0 wstring \"%s\", using %s codec end pos=0x%x", result, codec, readBuffer.getPosition()));
            return result;
        } catch (IndexOutOfBoundsException e) {
            throw stringMarshallingError("GIOP 1.0 wstring", numChars, MinorReadWStringOverflow, e);
        }
    }

    private String read_wstring_1_2() {
        // read the length of the string (in octets for GIOP 1.2+)
        int numOctets = read_ulong();
        if (numOctets < 0) throw stringMarshallingError("GIOP 1.2 wstring", numOctets, MinorReadWStringOverflow);
        if (readBuffer.available() < numOctets) throw stringMarshallingError("GIOP 1.2 wstring", numOctets, MinorReadWStringOverflow);

        WcharCodec codec = codecs.wcharCodec;
        GIOP_IN_LOG.fine(() -> String.format("Reading GIOP 1.2 wstring of length %d octets using codec %s", numOctets, codec));

        // In GIOP 1.2 there is no terminating null char, but there might be a BOM
        StringBuilder sb = new StringBuilder(numOctets / 2);

        final int endPosition = readBuffer.getPosition() + numOctets;
        // this method checks for and consumes a BOM if present, returning the appropriately endian char reader
        WcharReader reader = codecs.wcharCodec.beginToReadWstring_1_2(readBuffer);
        try {
            while (readBuffer.getPosition() < endPosition) sb.append(reader.readWchar(readBuffer));
            GIOP_IN_LOG.fine(() -> String.format("Read GIOP 1.2 wstring \"%s\", using %s codec end pos=0x%x", sb, codec, readBuffer.getPosition()));
            return sb.toString();
        } catch (IndexOutOfBoundsException e) {
            throw stringMarshallingError("GIOP 1.2 wstring", numOctets, MinorReadWStringOverflow, e);
        }
    }

    public void read_boolean_array(boolean[] value, int offset, int length) {
        if (length <= 0) return;
        checkChunk();
        if (readBuffer.available() < length) throw newMarshalError((MinorReadBooleanArrayOverflow));
        for (int i = offset; i < offset + length; i++) value[i] = toBoolean(readBuffer.readByte());
    }

    public void read_char_array(char[] value, int offset, int length) {
        for (int i = offset; i < offset + length; i++) value[i] = read_char();
    }

    public void read_wchar_array(char[] value, int offset, int numChars) {
        for (int i = offset; i < offset + numChars; i++) value[i] = read_wchar();
    }

    public void read_octet_array(byte[] value, int offset, int length) {
        if (length <= 0) return;
        checkChunk();
        try {
            readBuffer.readBytes(value, offset, length);
        } catch (IndexOutOfBoundsException e) {
            throw newMarshalError((MinorReadOctetArrayOverflow), e);
        }
    }

    public void read_short_array(short[] value, int offset, int length) {
        if (length <= 0) return;
        checkChunk();
        readBuffer.align(TWO_BYTE_BOUNDARY);

        if (readBuffer.available() < length * 2) throw newMarshalError(MinorReadShortArrayOverflow);
        range(offset, offset + length).forEach(swapBytes ?
                i -> value[i] = readBuffer.readShort_LE() :
                i -> value[i] = readBuffer.readShort());
    }

    public void read_ushort_array(short[] value, int offset, int length) {
        try {
            read_short_array(value, offset, length);
        } catch (IndexOutOfBoundsException|MARSHAL e) {
            throw newMarshalError((MinorReadUShortArrayOverflow), e);
        }
    }

    public void read_long_array(int[] value, int offset, int length) {
        if (length <= 0) return;
        checkChunk();
        readBuffer.align(FOUR_BYTE_BOUNDARY);

        if (readBuffer.available() < length * 4)
            throw newMarshalError(MinorReadLongArrayOverflow);

        range(offset, offset + length).forEach( swapBytes ?
                i -> value[i] = readBuffer.readInt_LE() :
                i -> value[i] = readBuffer.readInt());
    }

    private static MARSHAL newMarshalError(int minor) {
        return new MARSHAL(describeMarshal(minor), minor, COMPLETED_NO);
    }

    private static MARSHAL newMarshalError(int minor, Throwable cause) {
        return (MARSHAL)(newMarshalError(minor).initCause(cause instanceof MARSHAL ? cause.getCause() : cause));
    }

    public void read_ulong_array(int[] value, int offset, int length) {
        try {
            read_long_array(value, offset, length);
        } catch (IndexOutOfBoundsException|MARSHAL e) {
            throw newMarshalError(MinorReadULongArrayOverflow, e);
        }
    }

    public void read_longlong_array(long[] value, int offset, int length) {
        if (length <= 0) return;
        checkChunk();
        readBuffer.align(EIGHT_BYTE_BOUNDARY);
        if (readBuffer.available() < length * 8) throw newMarshalError(MinorReadLongLongArrayOverflow);
        range(offset, offset + length).forEach( swapBytes ?
                i -> value[i] = readBuffer.readLong_LE() :
                i -> value[i] = readBuffer.readLong());
    }

    public void read_ulonglong_array(long[] value, int offset, int length) {
        try {
            read_longlong_array(value, offset, length);
        } catch (IndexOutOfBoundsException|MARSHAL e) {
            throw newMarshalError(MinorReadULongLongArrayOverflow, e);
        }
    }

    public void read_float_array(float[] value, int offset, int length) {
        if (length <= 0) return;
        checkChunk();
        readBuffer.align(FOUR_BYTE_BOUNDARY);
        if (readBuffer.available() < length * 4) throw newMarshalError(MinorReadFloatArrayOverflow);
        range(offset, offset + length).forEach( swapBytes ?
                i -> value[i] = readBuffer.readFloat_LE() :
                i -> value[i] = readBuffer.readFloat());
    }

    public void read_double_array(double[] value, int offset, int length) {
        if (length <= 0) return;
        checkChunk();
        readBuffer.align(EIGHT_BYTE_BOUNDARY);
        if (readBuffer.available() < length * 8) throw newMarshalError(MinorReadDoubleArrayOverflow);
        range(offset, offset + length).forEach( swapBytes ?
                i -> value[i] = readBuffer.readDouble_LE() :
                i -> value[i] = readBuffer.readDouble());
    }

    public org.omg.CORBA.Object read_Object() {
        checkChunk();
        IOR ior = IORHelper.read(this);
        if ((ior.type_id.isEmpty()) && (ior.profiles.length == 0)) return null;
        if (orbInstance == null) throw new INITIALIZE("InputStream must be created by a full ORB");
        ObjectFactory objectFactory = orbInstance.getObjectFactory();
        return objectFactory.createObject(ior);
    }

    @Override
    public org.omg.CORBA.Object read_Object(@SuppressWarnings("rawtypes") Class expectedType) {
        org.omg.CORBA.Object obj = read_Object();

        if (obj == null) return null;
        // OK, we have two possibilities here.  The usual possibility is we're asked to load
        // an object using a specified Stub class.  We just create an instance of the stub class,
        // attach the object as a delegate, and we're done.
        //
        // The second possibility is a request for an instance of an interface.  This will require
        // us to locate a stub class using the defined Stub search orders.  After that, the process
        // is largely the same.
        org.omg.CORBA.portable.ObjectImpl impl = (org.omg.CORBA.portable.ObjectImpl) obj;

        if (org.omg.CORBA.portable.ObjectImpl.class.isAssignableFrom(expectedType)) {
            return createStub(expectedType, impl._get_delegate());
        }

        final String codebase = ((org.omg.CORBA_2_3.portable.ObjectImpl) impl)._get_codebase();

        try {
            if (IDLEntity.class.isAssignableFrom(expectedType)) {
                final Class<?> helperClass = Util.loadClass(expectedType.getName() + "Helper", codebase, doPrivileged(getClassLoader(expectedType)));
                final Method helperNarrow = doPrivileged(getMethod(helperClass, "narrow", org.omg.CORBA.Object.class));
                return (org.omg.CORBA.Object) helperNarrow.invoke(null, impl);
            }
            return createStub(getRMIStubClass(codebase, expectedType), impl._get_delegate());
        } catch (IllegalAccessException | ClassNotFoundException | ClassCastException | PrivilegedActionException | InvocationTargetException ex) {
            GIOP_IN_LOG.log(FINE, ex, () -> "Exception creating object stub");
            throw newMarshalError(MinorLoadStub, ex);
        }
    }

    private org.omg.CORBA.Object createStub(Class<?> stubClass, org.omg.CORBA.portable.Delegate delegate) {
        ensure(ObjectImpl.class.isAssignableFrom(stubClass), "stub class " + stubClass.getName() + " must extend ObjectImpl");
        @SuppressWarnings("unchecked")
        Class<? extends ObjectImpl> clz = (Class<? extends ObjectImpl>) stubClass;
        try {
            org.omg.CORBA.portable.ObjectImpl stub = doPrivileged(getNoArgConstructor(clz)).newInstance();
            stub._set_delegate(delegate);
            return stub;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | PrivilegedActionException ex) {
            GIOP_IN_LOG.log(FINE, ex, () -> "Exception creating object stub");
            throw newMarshalError(MinorLoadStub, ex);
        }
    }

    /**
     * Convert a class type into a stub class name using the RMI stub name rules.
     * @param c The class we need to stub.
     * @return The target stub class name.
     */
    private String getRMIStubClassName(Class<?> c) {
        final String cname = c.getName();
        int idx = cname.lastIndexOf('.');
        return cname.substring(0, idx + 1) + "_" + cname.substring(idx + 1) + "_Stub";
    }

    /**
     * Load a statically-created Stub class for a type, attempting both the old
     * and new stub class rules.
     * @param codebase The search codebase to use.
     * @param type The type we need a stub for.
     * @return A loaded stub class.
     */
    @SuppressWarnings("unchecked")
    private Class<? extends org.omg.CORBA.portable.ObjectImpl> getRMIStubClass(String codebase, Class<?> type) throws ClassNotFoundException {
        String name = getRMIStubClassName(type);
        ClassLoader cl = doPrivileged(getClassLoader(type));
        try {
            return Util.loadClass(name, codebase, cl);
        } catch (ClassNotFoundException e1) {
            try {
                return Util.loadClass("org.omg.stub." + name, codebase, cl);
            } catch (ClassNotFoundException e2) {
                e2.addSuppressed(e1);
                throw e2;
            }
        }
    }

    public TypeCode read_TypeCode() {
        // NOTE:
        // No data with natural alignment of greater than four octets
        // is needed for TypeCode. Therefore, it is not necessary to do
        // encapsulation in a separate buffer.
        checkChunk();
        return readTypeCodeImpl(new Hashtable<>(), true);
    }

    public Any read_any() {
        Any any = new AnyImpl(orbInstance);
        any.read_value(this, read_TypeCode());
        return any;
    }

    public org.omg.CORBA.Context read_Context() {
        final int len = read_ulong();
        String[] values = new String[len];
        for (int i = 0; i < len; i++) values[i] = read_string();
        return new Context(orbInstance.getORB(), "", values);
    }

    public Principal read_Principal() {
        // Deprecated by CORBA 2.2
        throw new NO_IMPLEMENT();
    }

    public BigDecimal read_fixed() {
        StringBuilder vBuffer = new StringBuilder("0");
        StringBuilder sBuffer = new StringBuilder();

        boolean first = true;
        while (true) {
            final byte b = read_octet();

            int hi = (b >>> 4) & 0x0f;
            if (hi > 9) throw newMarshalError(MinorReadFixedInvalid);

            //
            // 0 as high nibble is only valid if it's not the first nibble
            //
            if (!first || hi > 0)
                vBuffer.append((char) (hi + '0'));

            final int lo = b & 0x0f;
            if (lo < 10)
                vBuffer.append((char) (lo + '0'));
            else if (lo == 0x0c || lo == 0x0d) {
                if (lo == 0x0d)
                    sBuffer.append("-");
                break;
            } else
                throw newMarshalError(MinorReadFixedInvalid);

            first = false;
        }

        sBuffer.append(vBuffer);

        try {
            return new BigDecimal(sBuffer.toString());
        } catch (NumberFormatException e) {
            throw newMarshalError((MinorReadFixedInvalid), e);
        }
    }

    public Serializable read_value() {
        return valueReader().readValue();
    }

    public Serializable read_value(String id) {
        return valueReader().readValue(id);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Serializable read_value(@SuppressWarnings("rawtypes") Class clz) {
        return valueReader().readValue(clz);
    }

    public Serializable read_value(BoxedValueHelper helper) {
        return valueReader().readValueBox(helper);
    }

    public Serializable read_value(Serializable value) {
        // This version of read_value is intended for use by factories
        valueReader().initializeValue(value);
        return value;
    }

    public Object read_abstract_interface() {
        return valueReader().readAbstractInterface();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object read_abstract_interface(@SuppressWarnings("rawtypes") Class clz) {
        return valueReader().readAbstractInterface(clz);
    }

    public void read_value(Any any, TypeCode tc) {
        valueReader().readValueAny(any, tc);
    }

    private YokoInputStream(ReadBuffer readBuffer, int offs, boolean swapBytes, CodecPair codecs, GiopVersion giopVersion) {
        this.readBuffer = readBuffer.setPosition(offs);
        this.swapBytes = swapBytes;
        this.origPos = offs;
        this.origSwap = swapBytes;
        setCodecsAndGiopVersion(codecs, giopVersion);
    }

    /**
     * Create a new input stream that starts from where <code>that</code> input stream started.
     */
    @SuppressWarnings("CopyConstructorMissesField")
    public YokoInputStream(YokoInputStream that) {
        this(that.readBuffer.clone(), that.origPos, that.origSwap, that.codecs, that.giopVersion);
        this.orbInstance = that.orbInstance;
    }

    public YokoInputStream(ReadBuffer readBuffer, boolean swapBytes, CodecPair codecs, GiopVersion giopVersion) {
        this(readBuffer, 0, swapBytes, codecs, giopVersion);
    }

    public YokoInputStream(byte[] data, boolean swapBytes, CodecPair codecs, GiopVersion giopVersion) {
        this(Buffer.createReadBuffer(data), swapBytes, codecs, giopVersion);
    }

    public YokoInputStream(ReadBuffer readBuffer, int offs, boolean swapBytes) {
        this(readBuffer, offs, swapBytes, null, null);
    }

    public YokoInputStream(ReadBuffer readBuffer, boolean swapBytes) {
        this(readBuffer, swapBytes, null, null);
    }

    public YokoInputStream(ReadBuffer readBuffer) {
        this(readBuffer, false, null, null);
    }

    public YokoInputStream(byte[] data) {
        this(Buffer.createReadBuffer(data));
    }

    public void setCodecsAndGiopVersion(CodecPair codecs, GiopVersion giopVersion) {
        if (giopVersion != null) this.giopVersion = giopVersion;
        this.codecs = CodecPair.createCopy(codecs);
    }

    public CodecPair getCodecs() {
        return codecs;
    }

    public ReadBuffer getBuffer() {
        return readBuffer;
    }

    public int getPosition() {
        return readBuffer.getPosition();
    }

    public void setPosition(int pos) {
        readBuffer.setPosition(pos);
    }

    public void _OB_swap(boolean swap) {
        this.swapBytes = swap;
    }

    public void _OB_reset() {
        swapBytes = origSwap;
        readBuffer.setPosition(origPos);
    }

    public void _OB_skip(int n) {
        try {
            readBuffer.skipBytes(n);
        } catch (IndexOutOfBoundsException e) {
            throw newMarshalError((MinorReadOverflow), e);
        }
    }

    public void skipAlign(AlignmentBoundary boundary) {
        readBuffer.align(boundary);
    }

    public void _OB_readEndian() {
        swapBytes = read_boolean(); // false means big endian
    }

    public void _OB_ORBInstance(ORBInstance orbInstance) {
        this.orbInstance = orbInstance;

        if (this.orbInstance != null && this.orbInstance.useTypeCodeCache()) {
            cache = TypeCodeCache.instance();
        }
    }

    public ORBInstance _OB_ORBInstance() { return orbInstance; }

    public int _OB_readLongUnchecked() {
        // The chunking code needs to read a long value without entering an infinite loop
        readBuffer.align(FOUR_BYTE_BOUNDARY);
        try {
            return swapBytes ? readBuffer.readInt_LE() : readBuffer.readInt();
        } catch (IndexOutOfBoundsException e) {
            throw newMarshalError((MinorReadLongOverflow), e);
        }
    }

    public void _OB_beginValue() { valueReader().beginValue(); }

    public void _OB_endValue() { valueReader().endValue(); }

    public void _OB_remarshalValue(TypeCode tc, YokoOutputStream out) {
        valueReader().remarshalValue(tc, out);
    }

    public void __setSendingContextRuntime(CodeBase runtime) {
        /*
         * Sun's ValueHandler implementation narrows the remote CodeBase
         * to a com.sun.org.omg.SendingContext.CodeBase. Narrowing CodeBaseProxy
         * is not possible, we need a stub.
         */
        sendingContextRuntime = (runtime instanceof CodeBaseProxy) ? ((CodeBaseProxy) runtime).getCodeBase() : runtime;
    }

    public CodeBase __getSendingContextRuntime() {
        return sendingContextRuntime;
    }

    public void __setCodeBase(String codebase) {
        this.codebase = codebase;
    }

    public String __getCodeBase() {
        return codebase;
    }

    /**
     * Return the cursor position in the buffer as a formatted string suitable for logging.
     */
    public String dumpPosition() {
        return readBuffer.dumpPosition();
    }

    /**
     * Return the unread data in the buffer as a formatted string suitable for logging.
     */
    public String dumpRemainingData() {
        return readBuffer.dumpRemainingData();
    }

    /** Return all the data in the buffer as a formatted string suitable for logging. */
    public String dumpAllData() { return readBuffer.dumpAllData(); }

    /** Append all the data in the buffer as a formatted string suitable for logging. */
    @SuppressWarnings("UnusedReturnValue")
    public StringBuilder dumpAllData(StringBuilder sb) { return readBuffer.dumpAllData(sb); }

    /** Return all the data in the buffer, with the position marked, as a formatted string suitable for logging. */
    public String dumpAllDataWithPosition() { return readBuffer.dumpAllDataWithPosition(); }

    /** Append all the data in the buffer, with the position marked, as a formatted string suitable for logging. */
    @SuppressWarnings("UnusedReturnValue")
    public StringBuilder dumpAllDataWithPosition(StringBuilder sb, String label) { return readBuffer.dumpAllDataWithPosition(sb, label); }

    private void checkChunk() {
        if (valueReader != null) {
            valueReader.checkChunk();
        }
    }

    @Override
    public void end_value() {
        valueReader().endValue();
    }

    @Override
    public void start_value() {
        valueReader().beginValue();
    }
}
