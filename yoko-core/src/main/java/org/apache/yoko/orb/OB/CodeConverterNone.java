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

import org.apache.yoko.orb.CORBA.YokoOutputStream;
import org.apache.yoko.io.ReadBuffer;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.INV_OBJREF;

import static org.apache.yoko.util.MinorCodes.MinorNoWcharCodeSet;
import static org.apache.yoko.util.MinorCodes.MinorWcharCodeSetRequired;
import static org.apache.yoko.util.MinorCodes.describeBadParam;
import static org.apache.yoko.util.MinorCodes.describeInvObjref;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;

/**
 * This class represents the absence of wchar codeset in an IOR or a CodeSet service context.
 * It throws exceptions when an attempt is made to convert wchar information.
 * The exceptions throw are as described for the client and the server in "Code Set Negotiation"
 * (see CORBA 3.0.3, section 13.10.2.6).
 * Note: there is an assumption that when wchars are being written, this is on the client,
 * and when they are being read, this is on the server. This assumption could be wrong.
 */
final class CodeConverterNone extends CodeConverterBase {
    CodeConverterNone(CodeSetInfo fromSet, CodeSetInfo toSet) {
        super(fromSet, toSet);
    }

    public boolean conversionRequired() {
        // conversion *is* required, but it will fail
        return true;
    }

    public char read_wchar(ReadBuffer readBuffer, int len) {
        throw new BAD_PARAM(describeBadParam(MinorNoWcharCodeSet), MinorNoWcharCodeSet, COMPLETED_NO);
    }

    public void write_wchar(YokoOutputStream out, char v) {
        throw new INV_OBJREF(describeInvObjref(MinorWcharCodeSetRequired), MinorWcharCodeSetRequired, COMPLETED_NO);
    }

    public int read_count_wchar(char value) {
        throw new BAD_PARAM(describeBadParam(MinorNoWcharCodeSet), MinorNoWcharCodeSet, COMPLETED_NO);
    }

    public int write_count_wchar(char v) {
        throw new INV_OBJREF(describeInvObjref(MinorWcharCodeSetRequired), MinorWcharCodeSetRequired, COMPLETED_NO);
    }

    public char convert(char v) {
        throw new INV_OBJREF(describeInvObjref(MinorWcharCodeSetRequired), MinorWcharCodeSetRequired, COMPLETED_NO);
    }
}
