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

import org.apache.yoko.io.ReadBuffer;
import org.apache.yoko.orb.CORBA.YokoInputStream;
import org.apache.yoko.orb.CORBA.ORB;
import org.apache.yoko.orb.CORBA.YokoOutputStream;
import org.apache.yoko.orb.OB.CodecPair;
import org.apache.yoko.orb.OB.ORBInstance;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.apache.yoko.orb.OB.CodeSetInfo.UTF_16;
import static org.apache.yoko.orb.OB.CodeSetInfo.UTF_8;
import static org.apache.yoko.orb.OCI.GiopVersion.GIOP1_2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static testify.hex.HexBuilder.buildHex;
import static testify.hex.HexParser.HEX_STRING;

@TestInstance(PER_CLASS)
class UtfStringsTest {
    static Stream<Arguments> unicodeTestStrings() {
        return Stream.of(
            // Basic ASCII
            Arguments.of(
                "Basic ASCII greeting",
                "Hello, World!",
                "0000000e 48656c6c 6f2c2057 6f726c64 2100",
                "0000001a 00480065 006c006c 006f002c 00200057 006f0072 006c0064 0021"
            ),
            Arguments.of(
                "ASCII digits",
                "0123456789",
                "0000000b 30313233 34353637 383900",
                "00000014 00300031 00320033 00340035 00360037 00380039"
            ),
            // Latin Extended
            Arguments.of(
                "Latin extended characters",
                "Caf\u00e9 r\u00e9sum\u00e9",
                "0000000f 436166c3 a92072c3 a973756d c3a900",
                "00000016 00430061 006600e9 00200072 00e90073 0075006d 00e9"
            ),
            Arguments.of(
                "German umlaut",
                "Z\u00fcrich",
                "00000008 5ac3bc72 69636800",
                "0000000c 005a00fc 00720069 00630068"
            ),
            // Cyrillic
            Arguments.of(
                "Cyrillic - Privet (Hello)",
                "\u041f\u0440\u0438\u0432\u0435\u0442",
                "0000000d d09fd180 d0b8d0b2 d0b5d182 00",
                "0000000c 041f0440 04380432 04350442"
            ),
            // Greek
            Arguments.of(
                "Greek - Geia (Hello)",
                "\u0393\u03b5\u03b9\u03b1",
                "00000009 ce93ceb5 ceb9ceb1 00",
                "00000008 039303b5 03b903b1"
            ),
            // CJK
            Arguments.of(
                "Chinese - Ni hao (Hello)",
                "\u4f60\u597d",
                "00000007 e4bda0e5 a5bd00",
                "00000004 4f60597d"
            ),
            Arguments.of(
                "Japanese - Konnichiwa (Hello)",
                "\u3053\u3093\u306b\u3061\u306f",
                "00000010 e38193e3 8293e381 abe381a1 e381af00",
                "0000000a 30533093 306b3061 306f"
            ),
            Arguments.of(
                "Korean - Annyeong (Hello)",
                "\uc548\ub155",
                "00000007 ec9588eb 859500",
                "00000004 c548b155"
            ),
            // Arabic (RTL)
            Arguments.of(
                "Arabic RTL - Marhaba (Hello)",
                "\u0645\u0631\u062d\u0628\u0627",
                "0000000b d985d8b1 d8add8a8 d8a700",
                "0000000a 06450631 062d0628 0627"
            ),
            // Hebrew (RTL)
            Arguments.of(
                "Hebrew RTL - Shalom (Hello)",
                "\u05e9\u05dc\u05d5\u05dd",
                "00000009 d7a9d79c d795d79d 00",
                "00000008 05e905dc 05d505dd"
            ),
            // Emoji (surrogate pairs)
            Arguments.of(
                "Emoji - waving hand",
                "\ud83d\udc4b",
                "00000007 eda0bded b18b00",
                "00000004 d83ddc4b"
            ),
            Arguments.of(
                "Emoji - globe",
                "\ud83c\udf0d",
                "00000007 eda0bced bc8d00",
                "00000004 d83cdf0d"
            ),
            // Mathematical symbols
            Arguments.of(
                "Math symbols - sum, infinity, approximately",
                "\u2211\u221e\u2248",
                "0000000a e28891e2 889ee289 8800",
                "00000006 2211221e 2248"
            ),
            // Edge cases
            Arguments.of(
                "Empty string",
                "",
                "00000001 00",
                "00000000"
            ),
            Arguments.of(
                "First non-ASCII character",
                "\u0080",
                "00000003 c28000",
                "00000002 0080"
            ),
            Arguments.of(
                "Last BMP character",
                "\uffff",
                "00000004 efbfbf00",
                "00000002 ffff"
            ),
            // Mixed content
            Arguments.of(
                "Mixed - Euro symbol",
                "Price: \u20ac50",
                "0000000d 50726963 653a20e2 82ac3530 00",
                "00000014 00500072 00690063 0065003a 002020ac 00350030"
            ),
            Arguments.of(
                "Mixed - degree symbol",
                "Temp: 25\u00b0C",
                "0000000c 54656d70 3a203235 c2b04300",
                "00000014 00540065 006d0070 003a0020 00320035 00b00043"
            ),
            // Normalization test
            Arguments.of(
                "Normalization - precomposed e-acute",
                "caf\u00e9",
                "00000006 636166c3 a900",
                "00000008 00630061 006600e9"
            ),
            Arguments.of(
                "Normalization - e + combining acute",
                "cafe\u0301",
                "00000007 63616665 cc8100",
                "0000000a 00630061 00660065 0301"
            ),
            // Zero-width
            Arguments.of(
                "Zero-width space",
                "Hello\u200bWorld",
                "0000000e 48656c6c 6fe2808b 576f726c 6400",
                "00000016 00480065 006c006c 006f200b 0057006f 0072006c 0064"
            )
        );
    }

    static YokoOutputStream newUtfSpecificOutputStream() {
        ORB orb = (ORB) ORB.init((String[]) null, null);
        ORBInstance orbInst = orb._OB_ORBInstance();
        CodecPair codecs = CodecPair.create(UTF_8.id, UTF_16.id);
        return new YokoOutputStream(codecs, GIOP1_2);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unicodeTestStrings")
    void writeUtf8String(String description, String text, String utf8Hex, String utf16Hex) {
        checkWrittenForm(o -> o.write_string(text), utf8Hex);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unicodeTestStrings")
    void readUtf8String(String description, String expectedText, String utf8Hex, String utf16Hex) {
        String actualText = writeHex(utf8Hex).read_string();
        assertEquals(expectedText, actualText);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unicodeTestStrings")
    void writeUtf16Wstring(String description, String text, String utf8Hex, String utf16Hex) {
        checkWrittenForm(o -> o.write_wstring(text), utf16Hex);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unicodeTestStrings")
    void readUtf16String(String description, String expectedText, String utf8Hex, String utf16Hex) {
        String actualText = writeHex(utf16Hex).read_wstring();
        assertEquals(expectedText, actualText);
    }

    static YokoInputStream writeHex(String hex) {
        byte[] bytes = HEX_STRING.parse(hex.replaceAll(" ", ""));
        try (var out = newUtfSpecificOutputStream()){
            out.write_octet_array(bytes, 0, bytes.length);
            System.out.println(out.getBufferReader().dumpAllData());
            return out.create_input_stream();
        }
    }

    static void checkWrittenForm(Consumer<YokoOutputStream> writeTextTo, String expectedHex) {
        // convert the hex to bytes
        byte[] expected = HEX_STRING.parse(expectedHex.replaceAll(" ", ""));
        // rewrite the hex in DUMP format
        expectedHex = buildHex().bytes(expected).dump();
        try (var out = newUtfSpecificOutputStream()) {
            writeTextTo.accept(out);
            System.out.println(out.getBufferReader().dumpAllData());
            ReadBuffer br = out.getBufferReader();
            byte[] actual = new byte[br.length()];
            br.readBytes(actual);
            String actualHex = buildHex().bytes(actual).dump();
            assertEquals(expectedHex, actualHex);
        }
    }
}
