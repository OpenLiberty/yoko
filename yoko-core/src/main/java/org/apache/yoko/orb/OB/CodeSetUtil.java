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

import org.apache.yoko.orb.CORBA.YokoInputStream;
import org.apache.yoko.orb.OCI.ProfileInfo;
import org.omg.CONV_FRAME.CodeSetComponent;
import org.omg.CONV_FRAME.CodeSetComponentInfo;
import org.omg.CONV_FRAME.CodeSetComponentInfoHelper;
import org.omg.CONV_FRAME.CodeSetContext;
import org.omg.CONV_FRAME.CodeSetContextHelper;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.TAG_CODE_SETS;

import java.util.stream.Stream;

import static org.apache.yoko.orb.OB.CodeSetDatabase.determineTCS;
import static org.apache.yoko.orb.OB.CodeSetInfo.ISO_LATIN_1;
import static org.apache.yoko.orb.OB.CodeSetInfo.UCS_2;
import static org.apache.yoko.orb.OB.CodeSetInfo.UTF_16;
import static org.apache.yoko.orb.OB.CodeSetInfo.UTF_8;

final public class CodeSetUtil {
    static CodeSetComponent createCodeSetComponent(final int native_codeset_id, final boolean wChar) {
        return wChar ?
                UTF_16.id == native_codeset_id ?
                        new CodeSetComponent(UTF_16.id) :
                        new CodeSetComponent(native_codeset_id, UTF_16.id) :
                UTF_8.id == native_codeset_id ?
                        new CodeSetComponent(UTF_8.id) :
                        new CodeSetComponent(native_codeset_id, UTF_8.id);
    }

    static CodeSetComponentInfo getCodeSetInfoFromComponents(ORBInstance orbInstance, ProfileInfo profileInfo) {
        // For IIOP 1.0 use proprietary mechanism (ISOLATIN1 and UCS2), if configured.
        if (profileInfo.major == 1 && profileInfo.minor == 0) {
            if (!orbInstance.extendedWchar()) return null;
            // proprietary codeset configured, so use ISO Latin 1 for char and UCS 2 for wchar
            return new CodeSetComponentInfo(new CodeSetComponent(ISO_LATIN_1.id), new CodeSetComponent(UCS_2.id));
        }

        // For IIOP 1.1 or newer extract codeset from profile
        return Stream.of(profileInfo.components)
                .filter(c -> TAG_CODE_SETS.value == c.tag)
                .map(c -> c.component_data)
                .map(YokoInputStream::new)
                .peek(YokoInputStream::_OB_readEndian)
                .map(CodeSetComponentInfoHelper::read)
                .findFirst().orElse(null);
    }

    //
    // Get code converters from ProfileInfo and/or IOR
    //
    static CodeConverters getCodeConverters(ORBInstance orbInstance, ProfileInfo profileInfo) {
        //
        // Set up code converters
        //
        //
        // Other transmission codesets than the defaults can only be
        // determined if a codeset profile was present in the IOR.
        // The fallbacks in this case according to the specification
        // are UTF-8 (not ISOLATIN1!) and UTF-16 (not UCS2!).
        //

        final CodeSetComponentInfo info = getCodeSetInfoFromComponents(orbInstance, profileInfo);

        if (info == null) return CodeConverters.create(orbInstance, ISO_LATIN_1.id, orbInstance.getDefaultWcs());

        CodeSetComponent client_cs = createCodeSetComponent(orbInstance.getNativeCs(), false);
        CodeSetComponent client_wcs = createCodeSetComponent(orbInstance.getNativeWcs(), true);
        final int tcs_c = determineTCS(client_cs, info.ForCharData, UTF_8.id);
        final int tcs_wc = determineTCS(client_wcs, info.ForWcharData, UTF_16.id);
        return CodeConverters.create(orbInstance, tcs_c, tcs_wc);
    }

    static CodeSetContext extractCodeSetContext(ServiceContext csSC) {
        YokoInputStream in = new YokoInputStream(csSC.context_data);
        in._OB_readEndian();
        return CodeSetContextHelper.read(in);
    }
}
