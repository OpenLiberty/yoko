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
package org.apache.yoko;

import org.apache.yoko.io.ReadBuffer;
import org.apache.yoko.io.SimplyCloseable;
import org.apache.yoko.orb.CORBA.YokoInputStream;
import org.apache.yoko.orb.CORBA.OutputStream;
import org.apache.yoko.orb.OCI.GiopVersion;
import org.apache.yoko.rmi.impl.ValueHandlerImpl;
import org.apache.yoko.util.cmsf.CmsfThreadLocal;
import org.apache.yoko.util.rofl.RoflThreadLocal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.CosNaming.NameComponent;

import javax.rmi.CORBA.Util;
import javax.rmi.CORBA.ValueHandler;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.apache.yoko.util.rofl.Rofl.RemoteOrb.IBM;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.CoreMatchers.theInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static testify.hex.HexBuilder.buildHex;
import static testify.hex.HexParser.HEX_DUMP;

/**
 * Test writing Java values directly to and reading them back from CDR streams.
 */
class JavaValueTest {
    OutputStream out;
    YokoInputStream in;

    enum NameComponents {;

        static NameComponent stringToName(String idAndKind) {
            String id = idAndKind.replaceFirst("\\..*", "");
            String kind = idAndKind.replaceFirst( "[^.]*\\.?", "");
            if ("null".equals(id)) id = null;
            if ("null".equals(kind)) kind = null;
            //System.out.printf("%s -> '%s'.'%s'%n", idAndKind, id, kind);
            return new NameComponent(id, kind);
        }

        static NameComponent[] stringToPath(String components) {
            components = components.replace('/',' ').trim();
            List<NameComponent> ncs = new ArrayList<>();
            for (String idAndKind : components.split(" ")) ncs.add(stringToName(idAndKind));
            return ncs.toArray(new NameComponent[ncs.size()]);
        }

        static String nameToString(NameComponent nc) {
            return String.format("%s.%s", nc.id, nc.kind);
        }

        static String pathToString(NameComponent[] path) {
            if (path.length == 0) return "";
            String result = "";
            for (NameComponent nc : path) result += "/" + nameToString(nc);
            return result.substring(1);
        }

        static void assertEquals(NameComponent actual, NameComponent expected) {
            assertNotNull(expected);
            assertNotSame(actual, expected);
            Assertions.assertEquals(nameToString(actual), nameToString(expected));
        }

        static void assertEquals(NameComponent[] actual, NameComponent[] expected) {
            assertNotNull(expected);
            assertNotSame(actual, expected);
            Assertions.assertEquals(pathToString(actual), pathToString(expected));
        }
    }

    private void finishWriting() {
        System.out.println(out.getBufferReader().dumpAllData());
        in = out.create_input_stream();
        out = null;
    }

    private void writeHex(String hex) {
        byte[] bytes = HEX_DUMP.parse(hex);
        out.write_octet_array(bytes, 0, bytes.length);
        finishWriting();
    }

    private void assertHex(String hex) {
        byte[] expected = HEX_DUMP.parse(hex);
        String expectedHex = buildHex().bytes(expected).dump();
        ReadBuffer br = out.getBufferReader();
        byte[] actual = new byte[br.length()];
        br.readBytes(actual);
        String actualHex = buildHex().bytes(actual).dump();
        assertEquals(expectedHex, actualHex);
    }

    @BeforeEach
    void setupStreams() {
        out = new OutputStream(null, GiopVersion.GIOP1_2);
        ValueHandler vh = Util.createValueHandler();
        assertInstanceOf(ValueHandlerImpl.class, vh);
    }

    @Test
    void marshalTwoDistinctLongs() {
        Long l1 = 2019L;
        Long l2 = Long.parseLong("2019");
        assumeFalse(l1 == l2, "If this Java runtime makes the two longs equal to start with, this test cannot work");
        out.write_value(l1, Long.class);
        out.write_value(l2, Long.class);
        finishWriting();
        Long l3 = (Long) in.read_value(Long.class);
        Long l4 = (Long) in.read_value(Long.class);
        assertThat(l3, equalTo(l1));
        assertThat(l4, equalTo(l2));
        assertThat(l3, not(sameInstance(l1)));
        assertThat(l4, not(sameInstance(l2)));
        assertThat(l4, not(sameInstance(l3)));
    }

    @Test
    void marshalTheSameLongTwiceToTestValueIndirection() {
        Long l1 = Long.valueOf(2000L);
        Long l2 = l1;
        out.write_value(l1, Long.class);
        out.write_value(l2, Long.class);
        finishWriting();
        Long l3 = (Long) in.read_value(Long.class);
        Long l4 = (Long) in.read_value(Long.class);
        assertThat(l3, equalTo(l1));
        assertThat(l4, equalTo(l2));
        assertThat(l3, not(sameInstance(l1)));
        assertThat(l4, not(sameInstance(l2)));
        assertThat(l4, is(sameInstance(l3)));
    }

    @Test
    void marshalNameComponentAsValue() {
        NameComponent actual = NameComponents.stringToName("hello.world");
        out.write_value(actual, NameComponent.class);
        finishWriting();
        NameComponent expected = (NameComponent) in.read_value(NameComponent.class);
        NameComponents.assertEquals(actual, expected);
    }

    @Test
    void marshalNameComponentArrayAsValue() {
        NameComponent[] actual = NameComponents.stringToPath("hello.dir/world.dir/text.object");
        out.write_value(actual, NameComponent[].class);
        finishWriting();
        NameComponent[] expected = (NameComponent[]) in.read_value(NameComponent[].class);
        NameComponents.assertEquals(actual, expected);
    }

    @Test
    void marshalWstringWithNulls() {
        String expected = "Hello\0world";
        out.write_wstring(expected);
        finishWriting();
        String actual = in.read_wstring();
        assertNotNull(actual);
        assertEquals(expected, actual);
        assertNotSame(expected, actual);
    }

    @Test
    void marshalWstringWithSurrogates() {
        String expected = new String(Character.toChars(Character.MIN_SURROGATE)) + new String(Character.toChars(Character.MAX_CODE_POINT));
        out.write_wstring(expected);
        finishWriting();
        String actual = in.read_wstring();
        assertNotNull(actual);
        assertEquals(expected, actual);
        assertNotSame(expected, actual);
    }

    @Test
    void unmarshalTwoLongValues() {
        writeHex("" +
                "    7fffff02 00000035 524d493a 6a617661  \".......5RMI:java\"\n" +
                "    2e6c616e 672e4c6f 6e673a32 30354636  \".lang.Long:205F6\"\n" +
                "    43434630 30324536 4539303a 33423842  \"CCF002E6E90:3B8B\"\n" +
                "    45343930 43433846 32334446 00bdbdbd  \"E490CC8F23DF....\"\n" +
                "    00000000 00000007 7fffff02 ffffffff  \"................\"\n" +
                "    ffffffb4 bdbdbdbd 00000000 0000000b  \"................\"");
        Long actual1 = (Long)in.read_value(Long.class);
        Long actual2 = (Long)in.read_value(Long.class);
        assertThat(actual1, is(07L));
        assertThat(actual2, is(11l));
    }

    @Test
    void unmarshalLongValueAndIndirection() throws Exception {
        writeHex("" +
                "    7fffff02 00000035 524d493a 6a617661  \".......5RMI:java\"\n" +
                "    2e6c616e 672e4c6f 6e673a32 30354636  \".lang.Long:205F6\"\n" +
                "    43434630 30324536 4539303a 33423842  \"CCF002E6E90:3B8B\"\n" +
                "    45343930 43433846 32334446 00bdbdbd  \"E490CC8F23DF....\"\n" +
                "    00000000 00000002 ffffffff ffffffb4  \"................\"");
        Long actual1 = (Long)in.read_value(Long.class);
        Long actual2 = (Long)in.read_value(Long.class);
        assertThat(actual1, is(2L));
        assertThat(actual2, is(2L));
        assertThat(actual1, is(theInstance(actual2)));
    }

    @Test
    void unmarshalNameComponentValue() {
        writeHex("" +
                "    7fffff02 00000046 524d493a 6f72672e  \".......FRMI:org.\"\n" +
                "    6f6d672e 436f734e 616d696e 672e4e61  \"omg.CosNaming.Na\"\n" +
                "    6d65436f 6d706f6e 656e743a 45303638  \"meComponent:E068\"\n" +
                "    41373543 39383933 30443636 3a463136  \"A75C98930D66:F16\"\n" +
                "    34413231 39344136 36323832 4100bdbd  \"4A2194A66282A...\"\n" +
                "    00000006 68656c6c 6f00bdbd 00000006  \"....hello.......\"\n" +
                "    776f726c 6400                        \"world.\"");
        NameComponent actual = (NameComponent)in.read_value(NameComponent.class);
        NameComponent expected = NameComponents.stringToName("hello.world");
        NameComponents.assertEquals(expected, actual);
    }

    @Test
    void unmarshalNameComponentChunkedValue() {
        writeHex("" +
                "    7fffff0a 00000046 524d493a 6f72672e  \".......FRMI:org.\"\n" +
                "    6f6d672e 436f734e 616d696e 672e4e61  \"omg.CosNaming.Na\"\n" +
                "    6d65436f 6d706f6e 656e743a 45303638  \"meComponent:E068\"\n" +
                "    41373543 39383933 30443636 3a463136  \"A75C98930D66:F16\"\n" +
                "    34413231 39344136 36323832 4100bdbd  \"4A2194A66282A...\"\n" +
                "    00000016 00000006 68656c6c 6f00bdbd  \"........hello...\"\n" +
                "    00000006 776f726c 64000000 ffffffff  \"....world.......\"");
        NameComponent actual = (NameComponent)in.read_value(NameComponent.class);
        NameComponent expected = NameComponents.stringToName("hello.world");
        NameComponents.assertEquals(expected, actual);
    }

    @Test
    void unmarshalNameComponentArray() {
        writeHex("" +
                "7fffff02 00000049 524d493a 5b4c6f72  \".......IRMI:[Lor\"\n" +
                "672e6f6d 672e436f 734e616d 696e672e  \"g.omg.CosNaming.\"\n" +
                "4e616d65 436f6d70 6f6e656e 743b3a45  \"NameComponent;:E\"\n" +
                "30363841 37354339 38393330 4436363a  \"068A75C98930D66:\"\n" +
                "46313634 41323139 34413636 32383241  \"F164A2194A66282A\"\n" +
                "00000000 00000001 7fffff0a 00000046  \"...............F\"\n" +
                "524d493a 6f72672e 6f6d672e 436f734e  \"RMI:org.omg.CosN\"\n" +
                "616d696e 672e4e61 6d65436f 6d706f6e  \"aming.NameCompon\"\n" +
                "656e743a 45303638 41373543 39383933  \"ent:E068A75C9893\"\n" +
                "30443636 3a463136 34413231 39344136  \"0D66:F164A2194A6\"\n" +
                "36323832 41000000 00000016 00000006  \"6282A...........\"\n" +
                "68656c6c 6f00bdbd 00000006 776f726c  \"hello.......worl\"\n" +
                "64000000 ffffffff                    \"d.......\"");
        NameComponent[] actual = (NameComponent[])in.read_value(NameComponent[].class);
        NameComponent[] expected = NameComponents.stringToPath("hello.world");
        NameComponents.assertEquals(expected, actual);
    }

    @Test
    void unmarshalNameComponentArrayFromNeo() {
        writeHex("" +
                "7fffff02 00000049 524d493a 5b4c6f72  \".......IRMI:[Lor\"\n" +
                "672e6f6d 672e436f 734e616d 696e672e  \"g.omg.CosNaming.\"\n" +
                "4e616d65 436f6d70 6f6e656e 743b3a45  \"NameComponent;:E\"\n" +
                "30363841 37354339 38393330 4436363a  \"068A75C98930D66:\"\n" +
                "46313634 41323139 34413636 32383241  \"F164A2194A66282A\"\n" +
                "00000000 00000001 7fffff0a 00000046  \"...............F\"\n" +
                "524d493a 6f72672e 6f6d672e 436f734e  \"RMI:org.omg.CosN\"\n" +
                "616d696e 672e4e61 6d65436f 6d706f6e  \"aming.NameCompon\"\n" +
                "656e743a 45303638 41373543 39383933  \"ent:E068A75C9893\"\n" +
                "30443636 3a463136 34413231 39344136  \"0D66:F164A2194A6\"\n" +
                "36323832 41000000 00000025 0000001b  \"6282A......%....\"\n" +
                "5265736f 6c766162 6c65436f 734e616d  \"ResolvableCosNam\"\n" +
                "696e6743 6865636b 65720000 00000001  \"ingChecker......\"\n" +
                "00000000 ffffffff                    \"........\"");
        NameComponent[] actual = (NameComponent[])in.read_value(NameComponent[].class);
        NameComponent[] expected = NameComponents.stringToPath("ResolvableCosNamingChecker");
        NameComponents.assertEquals(expected, actual);
    }

    @Test
    void marshalDateCmsf1() {
        Date actual = new Date(0);
        out.write_value(actual);
        assertHex("" +
                "7fffff0a 00000035 524d493a 6a617661  \".......5RMI:java\"\n" +
                "2e757469 6c2e4461 74653a41 43313137  \".util.Date:AC117\"\n" +
                "45323846 45333635 3837413a 36383641  \"E28FE36587A:686A\"\n" +
                "38313031 34423539 37343139 00bdbdbd  \"81014B597419....\"\n" +
                "0000000c 0101bdbd 00000000 00000000  \"................\"\n" +
                "ffffffff                             \"....\"");
    }



    @Test
    void marshalDateCmsf2() {
        Date actual = new Date(0);

        try (SimplyCloseable x = CmsfThreadLocal.push((byte)2);){
            out.write_value(actual);
        }
        assertHex("" +
                "7fffff0a 00000035 524d493a 6a617661  \".......5RMI:java\"\n" +
                "2e757469 6c2e4461 74653a41 43313137  \".util.Date:AC117\"\n" +
                "45323846 45333635 3837413a 36383641  \"E28FE36587A:686A\"\n" +
                "38313031 34423539 37343139 00bdbdbd  \"81014B597419....\"\n" +
                "00000002 0201bdbd 7fffff0a 00000044  \"...............D\"\n" +
                "524d493a 6f72672e 6f6d672e 63757374  \"RMI:org.omg.cust\"\n" +
                "6f6d2e6a 6176612e 7574696c 2e446174  \"om.java.util.Dat\"\n" +
                "653a4143 31313745 32384645 33363538  \"e:AC117E28FE3658\"\n" +
                "37413a36 38364138 31303134 42353937  \"7A:686A81014B597\"\n" +
                "34313900 00000008 00000000 00000000  \"419.............\"\n" +
                "ffffffff                             \"....\"");
    }

    @Test
    void marshalDatelikeJava8Cmsf1() {
        Date actual = new Date(0);
        try (SimplyCloseable x = RoflThreadLocal.push(() -> IBM);) {
            out.write_value(actual);
        }
            assertHex("" +
                    "7fffff0a 00000035 524d493a 6a617661  \".......5RMI:java\"\n" +
                    "2e757469 6c2e4461 74653a41 43313137  \".util.Date:AC117\"\n" +
                    "45323846 45333635 3837413a 36383641  \"E28FE36587A:686A\"\n" +
                    "38313031 34423539 37343139 00bdbdbd  \"81014B597419....\"\n" +
                    "0000000c 0100bdbd 00000000 00000000  \"................\"\n" +
                    "ffffffff                             \"....\"");
    }

    @Test
    void marshalDatelikeJava8Cmsf2() {
        Date actual = new Date(0);
        try (
                SimplyCloseable x = CmsfThreadLocal.push((byte)2);
                SimplyCloseable y = RoflThreadLocal.push(() -> IBM)) {
            out.write_value(actual);
        }
        assertHex("" +
                "7fffff0a 00000035 524d493a 6a617661  \".......5RMI:java\"\n" +
                "2e757469 6c2e4461 74653a41 43313137  \".util.Date:AC117\"\n" +
                "45323846 45333635 3837413a 36383641  \"E28FE36587A:686A\"\n" +
                "38313031 34423539 37343139 00bdbdbd  \"81014B597419....\"\n" +
                "00000002 0200bdbd 7fffff0a 00000044  \"...............D\"\n" +
                "524d493a 6f72672e 6f6d672e 63757374  \"RMI:org.omg.cust\"\n" +
                "6f6d2e6a 6176612e 7574696c 2e446174  \"om.java.util.Dat\"\n" +
                "653a4143 31313745 32384645 33363538  \"e:AC117E28FE3658\"\n" +
                "37413a36 38364138 31303134 42353937  \"7A:686A81014B597\"\n" +
                "34313900 00000008 00000000 00000000  \"419.............\"\n" +
                "ffffffff                             \"....\"");
    }

    @Test
    void marshalSqlDateCmsf1() {
        Date actual = new java.sql.Date(0);
        out.write_value(actual);
        assertHex("" +
                "0000:  7fffff0a 00000034 524d493a 6a617661  \".......4RMI:java\"\n" +
                "0010:  2e73716c 2e446174 653a3044 30393638  \".sql.Date:0D0968\"\n" +
                "0020:  45383232 36323732 44333a31 34464134  \"E8226272D3:14FA4\"\n" +
                "0030:  36363833 46333536 36393700 00000010  \"6683F356697.....\"\n" +
                "0040:  0101bdbd bdbdbdbd 00000000 00000000  \"................\"\n" +
                "0050:  ffffffff                             \"....\"");
    }

    @Test
    void marshalSqlDateCmsf2() {
        Date actual = new java.sql.Date(0);
        try (SimplyCloseable x = CmsfThreadLocal.push((byte)2)) {
            out.write_value(actual);
        }
        assertHex("" +
                "7fffff0a 00000034 524d493a 6a617661  \".......4RMI:java\"\n" +
                "2e73716c 2e446174 653a3044 30393638  \".sql.Date:0D0968\"\n" +
                "45383232 36323732 44333a31 34464134  \"E8226272D3:14FA4\"\n" +
                "36363833 46333536 36393700 00000002  \"6683F356697.....\"\n" +
                "0201bdbd 7fffff0a 00000044 524d493a  \"...........DRMI:\"\n" +
                "6f72672e 6f6d672e 63757374 6f6d2e6a  \"org.omg.custom.j\"\n" +
                "6176612e 7574696c 2e446174 653a4143  \"ava.util.Date:AC\"\n" +
                "31313745 32384645 33363538 37413a36  \"117E28FE36587A:6\"\n" +
                "38364138 31303134 42353937 34313900  \"86A81014B597419.\"\n" +
                "0000000c bdbdbdbd 00000000 00000000  \"................\"\n" +
                "ffffffff                             \"....\"");
    }

    @Test
    void marshalSqlDatelikeJava8Cmsf1() {
        Date actual = new java.sql.Date(0);
        try (SimplyCloseable y = RoflThreadLocal.push(() -> IBM)) {
            out.write_value(actual);
        }
        assertHex("" +
                "0000:  7fffff0a 00000034 524d493a 6a617661  \".......4RMI:java\"\n" +
                "0010:  2e73716c 2e446174 653a3044 30393638  \".sql.Date:0D0968\"\n" +
                "0020:  45383232 36323732 44333a31 34464134  \"E8226272D3:14FA4\"\n" +
                "0030:  36363833 46333536 36393700 00000010  \"6683F356697.....\"\n" +
                "0040:  0100bdbd bdbdbdbd 00000000 00000000  \"................\"\n" +
                "0050:  ffffffff                             \"....\"");
    }

    @Test
    void marshalSqlDatelikeJava8Cmsf2() {
        Date actual = new java.sql.Date(0);
        try (
                SimplyCloseable x = CmsfThreadLocal.push((byte)2);
                SimplyCloseable y = RoflThreadLocal.push(() -> IBM)) {
            out.write_value(actual);
        }
        assertHex("" +
                "7fffff0a 00000034 524d493a 6a617661  \".......4RMI:java\"\n" +
                "2e73716c 2e446174 653a3044 30393638  \".sql.Date:0D0968\"\n" +
                "45383232 36323732 44333a31 34464134  \"E8226272D3:14FA4\"\n" +
                "36363833 46333536 36393700 00000002  \"6683F356697.....\"\n" +
                "0200bdbd 7fffff0a 00000044 524d493a  \"...........DRMI:\"\n" +
                "6f72672e 6f6d672e 63757374 6f6d2e6a  \"org.omg.custom.j\"\n" +
                "6176612e 7574696c 2e446174 653a4143  \"ava.util.Date:AC\"\n" +
                "31313745 32384645 33363538 37413a36  \"117E28FE36587A:6\"\n" +
                "38364138 31303134 42353937 34313900  \"86A81014B597419.\"\n" +
                "0000000c bdbdbdbd 00000000 00000000  \"................\"\n" +
                "ffffffff                             \"....\"");
    }

    @Test
    void unmarshalDateWithDefaultWriteObjectFlag() {
        writeHex("" +
                "7fffff0a 00000035 524d493a 6a617661  \".......5RMI:java\"\n" +
                "2e757469 6c2e4461 74653a41 43313137  \".util.Date:AC117\"\n" +
                "45323846 45333635 3837413a 36383641  \"E28FE36587A:686A\"\n" +
                "38313031 34423539 37343139 00bdbdbd  \"81014B597419....\"\n" +
                "00000002 0201bdbd 7fffff0a 00000044  \"...............D\"\n" +
                "524d493a 6f72672e 6f6d672e 63757374  \"RMI:org.omg.cust\"\n" +
                "6f6d2e6a 6176612e 7574696c 2e446174  \"om.java.util.Dat\"\n" +
                "653a4143 31313745 32384645 33363538  \"e:AC117E28FE3658\"\n" +
                "37413a36 38364138 31303134 42353937  \"7A:686A81014B597\"\n" +
                "34313900 00000008 00000000 00000000  \"419.............\"\n" +
                "ffffffff                             \"....\"");
        Date actual = (Date)in.read_value(Date.class);
        Date expected = new Date(0);
        assertEquals(expected, actual);
    }

    @Test
    void unmarshalDateWithoutDefaultWriteObjectFlag() {
        writeHex("" +
                "7fffff0a 00000035 524d493a 6a617661  \".......5RMI:java\"\n" +
                "2e757469 6c2e4461 74653a41 43313137  \".util.Date:AC117\"\n" +
                "45323846 45333635 3837413a 36383641  \"E28FE36587A:686A\"\n" +
                "38313031 34423539 37343139 00bdbdbd  \"81014B597419....\"\n" +
                "00000002 0200bdbd 7fffff0a 00000044  \"...............D\"\n" +
                "524d493a 6f72672e 6f6d672e 63757374  \"RMI:org.omg.cust\"\n" +
                "6f6d2e6a 6176612e 7574696c 2e446174  \"om.java.util.Dat\"\n" +
                "653a4143 31313745 32384645 33363538  \"e:AC117E28FE3658\"\n" +
                "37413a36 38364138 31303134 42353937  \"7A:686A81014B597\"\n" +
                "34313900 00000008 00000000 00000000  \"419.............\"\n" +
                "ffffffff                             \"....\"");
        Date actual = (Date)in.read_value(Date.class);
        Date expected = new Date(0);
        assertEquals(expected, actual);
    }
}
