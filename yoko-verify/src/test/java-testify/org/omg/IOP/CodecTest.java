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

package org.omg.IOP;/*
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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.IOP.CodecFactoryPackage.UnknownEncoding;
import testify.iiop.annotation.ConfigureOrb;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.omg.IOP.fooHelper.extract;
import static org.omg.IOP.fooHelper.insert;
import static org.omg.IOP.fooHelper.type;

@ConfigureOrb
public class CodecTest {
    static final short UNKNOWN_ENCODING = 1;
    static CodecFactory factory;
    static Codec cdrCodec;
    static Any anyFoo;

    @BeforeAll
    static void initCodecFactory(ORB orb) {
        factory = assertDoesNotThrow(() -> CodecFactoryHelper.narrow(orb.resolve_initial_references("CodecFactory")));
        assertNotNull(factory);
        cdrCodec = assertDoesNotThrow(() -> factory.create_codec(withFormat(ENCODING_CDR_ENCAPS.value)));
        assertNotNull(cdrCodec);
        anyFoo = orb.create_any();
        insert(anyFoo, new foo(10));
    }

    static Encoding withFormat(int format) { return new Encoding((short)format, (byte) 0, (byte) 0); }

    @Test
    void unknownEncoding() { assertThrows(UnknownEncoding.class, () -> factory.create_codec(withFormat(UNKNOWN_ENCODING))); }

    @Test
    void encodeAndDecodeIdlEntity() {
        final byte[] bytes = assertDoesNotThrow(() -> cdrCodec.encode(anyFoo));
        Any result = assertDoesNotThrow(() -> cdrCodec.decode(bytes));
        assertEquals(10, extract(result).l);
    }

    @Test
    void encodeAndDecodeFooAsValue() {
        final byte[] bytes = assertDoesNotThrow(() -> cdrCodec.encode_value(anyFoo));
        Any result = assertDoesNotThrow(() -> cdrCodec.decode_value(bytes, type()));
        assertEquals(10, extract(result).l);
    }
}
