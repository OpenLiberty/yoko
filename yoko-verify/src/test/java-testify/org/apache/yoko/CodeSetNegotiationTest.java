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
package org.apache.yoko;

import acme.RemoteFunction;
import org.apache.yoko.io.Buffer;
import org.apache.yoko.io.ReadBuffer;
import org.apache.yoko.io.WriteBuffer;
import org.apache.yoko.orb.CORBA.YokoInputStream;
import org.apache.yoko.orb.OB.CodeSetInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.omg.CONV_FRAME.CodeSetComponent;
import org.omg.CONV_FRAME.CodeSetComponentInfo;
import org.omg.CONV_FRAME.CodeSetComponentInfoHelper;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.IOP.TAG_CODE_SETS;
import org.omg.IOP.TAG_INTERNET_IOP;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.RemoteImpl;
import testify.iiop.annotation.ConfigureServer.RemoteStub;

import javax.rmi.CORBA.Stub;
import java.io.Serializable;
import java.rmi.RemoteException;

import static java.util.Arrays.stream;
import static javax.rmi.PortableRemoteObject.narrow;
import static org.apache.yoko.io.AlignmentBoundary.FOUR_BYTE_BOUNDARY;
import static org.apache.yoko.io.AlignmentBoundary.TWO_BYTE_BOUNDARY;
import static org.apache.yoko.orb.OB.CodeSetInfo.UTF_8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWithIgnoringCase;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static testify.hex.HexParser.HEX_STRING;

/**
 * Test how the ORB behaves when negotiating codesets.
 */
@ConfigureServer
public abstract class CodeSetNegotiationTest {
    static final String RAINBOW_HEARTS = "\uD83D\uDC9C\uD83D\uDC99\uD83E\uDE75\uD83D\uDC9A\uD83D\uDC9B\uD83E\uDDE1\u2764\uFE0F";
    static final String PAYLOAD = "\u00C9cout\u00E9";
    static final NameComponent PAYLOAD_NC = new NameComponent(PAYLOAD, "");
    static final String YOKO_150_CSCI = "" +
            "00000001" + // component tag: TAG_CODE_SETS
            "0000001c" + // component data length: 28 bytes to follow
            "00bdbdbd" + // big-endian flag + 3 bytes padding
            "00010001" + // char native codeset: ISO_LATIN_1
            "00000002" + // 2 char conversion code sets
            "00010020" + // char conversion code set: ISO_646_IRV
            "05010001" + // char conversion code set: UTF-8
            "00010109" + // wchar native code set: UTF-16
            "00000000";  // no wchar conversion code sets
    static final String YOKO_152_CSCI = "" +
            "00000001" + // component tag: TAG_CODE_SETS
            "00000014" + // component data length: 20 bytes to follow
            "00bdbdbd" + // big-endian flag + 3 bytes padding
            "05010001" + // char native code set: UTF-8
            "00000000" + // no char conversion code sets
            "00010109" + // wchar native code set: UTF-16
            "00000000";  // no wchar conversion code sets
    public static final String PROFILE_LENGTH_PLACEHOLDER = "XXXXXXXX";

    static CodeSetComponent ccs(CodeSetInfo nativeCodeSet, CodeSetInfo... conversionCodeSets) {
        return new CodeSetComponent(nativeCodeSet.id, stream(conversionCodeSets).mapToInt(cs -> cs.id).toArray());
    }

    static CodeSetComponentInfo currentCodeSetComponentInfo;

    interface Echo extends RemoteFunction<Serializable, Serializable> {}

    @RemoteImpl
    public static final Echo IMPL = s -> {
        if (s instanceof String) assertThat(s, equalTo(PAYLOAD));
        else if (s instanceof NameComponent) assertThat(((NameComponent)s).id, equalTo(PAYLOAD));
        else throw new IllegalArgumentException("unexpected parameter type: " + s.getClass());
        return s;
    };

    @RemoteStub
    public static Echo stub;
    static Echo stub150, stub152;

    @BeforeAll
    public static void scrapeStub(ORB orb) {
        String ior = orb.object_to_string((Stub) stub);
        assertThat(ior, startsWithIgnoringCase("ior:"));  // check this looks like an ior
        byte[] bytes = HEX_STRING.parse(ior.substring(4)); // get the data from the hex representation
        WriteBuffer out = Buffer.createWriteBuffer(bytes.length + 128).writeBytes(bytes).trim();
        ReadBuffer in = out.readFromStart();
        final String iorTemplate;
        try {
            beginCDREncapsulation(in);
            skipTypeId(in);
            final int profileLengthIndex = assertExactlyOneProfileAndRememberLength(in);
            skipToTagComponentStart(in);
            currentCodeSetComponentInfo = removeCodeSetComponentInfo(in, out);
            // create a template IOR with a placeholder length and a missing codeset
            iorTemplate = getIorTemplate(out, profileLengthIndex);
        } catch (Throwable t) {
            System.out.printf("Dumping write buffer: %s%n%n%n%n", out.dumpAllData());
            System.out.printf("Dumping read buffer: %s%n%n%n%n", in.dumpAllDataWithPosition());
            throw t;
        }
        String ior150 = insertCorrectLength(iorTemplate + YOKO_150_CSCI);
        String ior152 = insertCorrectLength(iorTemplate + YOKO_152_CSCI);
        System.out.println("### original ior: " + ior);
        System.out.println("### Yoko 150 ior: " + ior150);
        System.out.println("### Yoko 152 ior: " + ior152);
        stub150 = (Echo) narrow(orb.string_to_object(ior150), Echo.class);
        stub152 = (Echo) narrow(orb.string_to_object(ior152), Echo.class);
    }

    private static String getIorTemplate(WriteBuffer out, int profileLengthIndex) {
        final String iorTemplate;
        String hex = out.asHex();
        int hexProfileLengthIndex = profileLengthIndex * 2;
        iorTemplate = "ior:" +
                hex.substring(0, hexProfileLengthIndex) +
                PROFILE_LENGTH_PLACEHOLDER +
                hex.substring(hexProfileLengthIndex + 8);
        return iorTemplate;
    }

    private static String insertCorrectLength(String iorTemplate) {
        int lengthIndex = iorTemplate.indexOf(PROFILE_LENGTH_PLACEHOLDER);
        int profileLength = (iorTemplate.length() - (lengthIndex + PROFILE_LENGTH_PLACEHOLDER.length())) / 2;
        return iorTemplate.replace(PROFILE_LENGTH_PLACEHOLDER, String.format("%08x", profileLength));
    }

    private static void skipTypeId(ReadBuffer in) {
        readLengthAndSkip("type id", in);
    }

    private static int assertExactlyOneProfileAndRememberLength(ReadBuffer in) {
        int profileLengthIndex;
        // check the profile count is 1 and the tag is 0, and the length is correct
        assertThat(in.align(FOUR_BYTE_BOUNDARY).readInt(), is(1));
        // check the tag is TAG_INTERNET_IOP
        assertThat(in.readInt(), is(TAG_INTERNET_IOP.value));
        // check this profile extends to the end of the IOR
        profileLengthIndex = in.getPosition();
        int len = in.readInt();
        assertThat(len, equalTo(in.available()));
        return profileLengthIndex;
    }

    private static void skipToTagComponentStart(ReadBuffer in) {
        // read in the profile body up to but not including the tagged components
        beginCDREncapsulation(in);
        assertThat("GIOP major version is 1", in.read(), is(1));
        assertThat("GIOP minor version is 2", in.read(), is(2));
        readLengthAndSkip("hostname", in);
        // skip the port number
        in.align(TWO_BYTE_BOUNDARY).skipBytes(2);
        // skip the object key
        readLengthAndSkip("object key", in);
    }

    private static CodeSetComponentInfo removeCodeSetComponentInfo(ReadBuffer in, WriteBuffer out) {
        byte[] codeSetComponentInfoData;
        // now cycle through the components
        int componentCount = in.align(FOUR_BYTE_BOUNDARY).readInt();
        while (componentCount-- > 0) {
            int componentTag = in.align(FOUR_BYTE_BOUNDARY).readInt();
            if (TAG_CODE_SETS.value == componentTag) break;
            readLengthAndSkip(String.format("Component with tag: %08X", componentTag), in);
        }
        // check we found a codesets component
        assertThat(componentCount, greaterThanOrEqualTo(0));
        // capture the position in the stream before the tag.
        out.setPosition(in.getPosition() - 4);
        // read in the data for the CodeSetComponentInfo
        int csciLen = in.readInt();
        codeSetComponentInfoData = new byte[csciLen];
        in.readBytes(codeSetComponentInfoData);
        // copy any remaining components to the write buffer
        if (!in.isComplete()) {
            in.align(FOUR_BYTE_BOUNDARY);
            // write buffer is already on a four byte boundary
            assert out.getPosition() % 4 == 0;
            while (!in.isComplete()) out.writeByte(in.readByte());
        }

        // align output stream to a four byte boundary before returning since we are now one tag component short
        out.align(FOUR_BYTE_BOUNDARY);
        out.trim();

        // Read in the CodeSetComponentInfo and return it
        YokoInputStream is = new YokoInputStream(codeSetComponentInfoData);
        assertThat(is.read(), is(0)); // read in the endian byte, next read will align automatically and skip padding
        return CodeSetComponentInfoHelper.read(is);
    }

    private static void beginCDREncapsulation(ReadBuffer in) {
        assertThat("CDR encapsulation should start with endianness byte of 0x00", in.read(), is(0));
    }

    private static void readLengthAndSkip(String description, ReadBuffer in) {
        in.align(FOUR_BYTE_BOUNDARY);
        System.out.println("Reading length for " + description);
        int len = in.readInt();
        System.out.println("Got length " + len);
        in.skipBytes(len);
        System.out.println("Skipped " + len + " bytes");
    }

}


/**
 * Test interaction with a Yoko-1.5.0-style IOR in this class to avoid connections caching codeset negotiation from other tests.
 */
@ConfigureServer
class Yoko_1_5_0_Test extends CodeSetNegotiationTest {
    @Test
    void testTransmitChars(ORB orb) throws RemoteException {
        stub150.apply(PAYLOAD_NC);
    }

    @Test
    void testTransmitWchars(ORB orb) throws RemoteException {
        stub150.apply(PAYLOAD);
    }
}

/**
 * Test interaction with a Yoko-1.5.2-style IOR in this class to avoid connections caching codeset negotiation from other tests.
 */
@ConfigureServer
class Yoko_1_5_2_Test extends CodeSetNegotiationTest {
    @Test
    void testTransmitChars(ORB orb) throws RemoteException {
        stub152.apply(PAYLOAD_NC);
    }

    @Test
    void testTransmitWchars(ORB orb) throws RemoteException {
        stub152.apply(PAYLOAD);
    }
}

/**
 * Test that the codeset tag component is created as expected
 */
@ConfigureServer
class CodesetTagComponentCreationTest extends CodeSetNegotiationTest {
    @Test
    void testCodesetsTagComponent() throws Exception {
        assertThat(currentCodeSetComponentInfo.ForCharData.native_code_set, is(UTF_8.id));
        assertThat(currentCodeSetComponentInfo.ForWcharData.native_code_set, is(CodeSetInfo.UTF_16.id));
    }
}
