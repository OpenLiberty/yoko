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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.apache.yoko.orb.CORBA.YokoInputStream;
import org.apache.yoko.util.HexConverter;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.ORB;
import org.omg.IOP.IOR;
import org.omg.IOP.IORHelper;

import testify.iiop.annotation.ConfigureOrb;

@ConfigureOrb
public class IORDumpTest {
    static final String IOR = "IOR:00bdbdbd0000003149444c3a636f6d2e69626d2f57736e4f7074696d697a65644e616d696e672f4e616d696e67436f6e746578743a312e3000bdbdbd00000001000000000000032c000102bd0000000d49424d2d504631543348583000bd0000000000694a4d4249000000124773e3aa37643062633737336533616166633334000000240000004549454a500200c92bdd9107736572766572311a57736e44697374436f734f626a65637441646170746572574c4d00000014697a6843656c6c2f6170706c69636174696f6e73bdbdbd0000000a000000010000001400bdbdbd0501000100000000000101000000000049424d0a0000000800bd00291800000000000026000000020002bdbd49424d04000000050005020102bdbdbd0000001f0000000400bd0003000000200000000400bd0001000000250000000400bd000300000021000001600001bdbd000000020002bdbd000000240000001e00bd00260002bdbd000000010000000c3139322e3136382e312e3700e43000400000bdbd000000080606678102010101000000280401000806066781020101010000001864656661756c7457494d46696c6542617365645265616c6d0400000000000001494210ce0000001d0000001964656661756c7457494d46696c6542617365645265616c6d00bdbdbd000000010000000806066781020101010000000f0002bdbd000000240000001e00bd00260002bdbd000000010000000c3139322e3136382e312e3700e43000400000bdbd0000000806062b1200021e06000000280401000806062b1200021e060000001864656661756c7457494d46696c6542617365645265616c6d0400000000000001494210ce0000001d0000001964656661756c7457494d46696c6542617365645265616c6d00bdbdbd000000010000000806062b1200021e060000000f49424d21000000b400bd00010001bdbd0000001057494d557365725265676973747279000000003449424d20576562537068657265204170706c69636174696f6e20536572766572204e6574776f726b204465706c6f796d656e740000000008392e302e302e370000000007332f322f313800bd0000000c63663037313830382e3031000000002f2863656c6c293a697a6843656c6c3a286e6f6465293a697a684e6f64653a28736572766572293a7365727665723100bdffff0001000000140000000800bd00764002e430";
    
    final ORB orb;
    
    public IORDumpTest(ORB orb) { this.orb = orb; }
    
    @Test
    void testDescribeIor() {
        String s = describe(IOR);
        System.out.println(s);
        assertThat(s, not(containsString("org.omg.CORBA.MARSHAL")));
        assertThat(s, not(containsString("org.omg.CORBA.BAD_PARAM")));
        assertThat(s, containsString("Component: TAG_SSL_SEC_TRANS"));
    }

    private String describe(String ref) {
        byte[] data = HexConverter.asciiToOctets(ref, 4);
        assertThat(data, is(not(nullValue())));
        YokoInputStream in = new YokoInputStream(data);
        in._OB_readEndian();
        IOR ior = IORHelper.read(in);
        String s = IORDump.describeIor(orb, ior);
        return s;
    }
}
