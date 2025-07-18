/*
 * Copyright 2025 IBM Corporation and others.
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
package org.apache.yoko.orb.OB;

import org.apache.yoko.orb.OB.Util.SysEx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.omg.CORBA.BAD_CONTEXT;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.TRANSACTION_MODE;
import org.omg.CORBA.UNKNOWN;
import org.omg.CORBA.portable.UnknownException;

import static org.junit.jupiter.api.Assertions.*;

class UtilTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "IDL:omg.org/CORBA/BAD_CONTEXT:1.0",
            "IDL:omg.org/CORBA/BAD_INV_ORDER:1.0",
            "IDL:omg.org/CORBA/BAD_OPERATION:1.0",
            "IDL:omg.org/CORBA/BAD_PARAM:1.0",
            "IDL:omg.org/CORBA/BAD_QOS:1.0",
            "IDL:omg.org/CORBA/BAD_TYPECODE:1.0",
            "IDL:omg.org/CORBA/CODESET_INCOMPATIBLE:1.0",
            "IDL:omg.org/CORBA/COMM_FAILURE:1.0",
            "IDL:omg.org/CORBA/DATA_CONVERSION:1.0",
            "IDL:omg.org/CORBA/FREE_MEM:1.0",
            "IDL:omg.org/CORBA/IMP_LIMIT:1.0",
            "IDL:omg.org/CORBA/INITIALIZE:1.0",
            "IDL:omg.org/CORBA/INTERNAL:1.0",
            "IDL:omg.org/CORBA/INTF_REPOS:1.0",
            "IDL:omg.org/CORBA/INV_FLAG:1.0",
            "IDL:omg.org/CORBA/INV_IDENT:1.0",
            "IDL:omg.org/CORBA/INV_OBJREF:1.0",
            "IDL:omg.org/CORBA/INV_POLICY:1.0",
            "IDL:omg.org/CORBA/INVALID_TRANSACTION:1.0",
            "IDL:omg.org/CORBA/MARSHAL:1.0",
            "IDL:omg.org/CORBA/NO_IMPLEMENT:1.0",
            "IDL:omg.org/CORBA/NO_MEMORY:1.0",
            "IDL:omg.org/CORBA/NO_PERMISSION:1.0",
            "IDL:omg.org/CORBA/NO_RESOURCES:1.0",
            "IDL:omg.org/CORBA/NO_RESPONSE:1.0",
            "IDL:omg.org/CORBA/OBJ_ADAPTER:1.0",
            "IDL:omg.org/CORBA/OBJECT_NOT_EXIST:1.0",
            "IDL:omg.org/CORBA/PERSIST_STORE:1.0",
            "IDL:omg.org/CORBA/REBIND:1.0",
            "IDL:omg.org/CORBA/TIMEOUT:1.0",
            "IDL:omg.org/CORBA/TRANSACTION_MODE:1.0",
            "IDL:omg.org/CORBA/TRANSACTION_REQUIRED:1.0",
            "IDL:omg.org/CORBA/TRANSACTION_ROLLEDBACK:1.0",
            "IDL:omg.org/CORBA/TRANSACTION_UNAVAILABLE:1.0",
            "IDL:omg.org/CORBA/TRANSIENT:1.0",
            "IDL:omg.org/CORBA/UNKNOWN:1.0" })
    public void testSysExId(String id) {
        assertTrue(Util.isSystemException(id));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "BAD ID",
            "idl:omg.org/CORBA/UNKNOWN:1.0",
            "RMI:java.lang.String",
            "IDL:omg.org/CORBA/MARSHAL:2.0",
            "org.omg.CORBA.TRANSIENT"})
    public void testInvalidSysExId(String id) {
        assertFalse(Util.isSystemException(id));
    }


    @Test
    public void testValueOfSysExObject() {
        assertEquals(SysEx.BAD_CONTEXT, SysEx.valueOf(new BAD_CONTEXT()));
        assertEquals(SysEx.TRANSACTION_MODE, SysEx.valueOf(new TRANSACTION_MODE()));
        assertEquals(SysEx.UNKNOWN, SysEx.valueOf(new UNKNOWN()));
    }

    @Test
    public void testValueOfOtherSysExObject() {
        SystemException systemException = new SystemException(null, 0, null) {};
        assertEquals(SysEx.UNKNOWN, SysEx.valueOf(systemException));
        assertEquals(SysEx.UNKNOWN, SysEx.valueOf(new UnknownException(null)));
    }
}