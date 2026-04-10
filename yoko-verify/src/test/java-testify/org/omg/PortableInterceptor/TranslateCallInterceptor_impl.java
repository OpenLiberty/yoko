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
package org.omg.PortableInterceptor;

import org.apache.yoko.orb.OB.Util;
import org.omg.CORBA.Any;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.portable.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class TranslateCallInterceptor_impl extends LocalObject implements ClientRequestInterceptor {
    private SystemException requestEx;
    private SystemException replyEx;
    private SystemException exceptionEx;
    private SystemException expected;
    public String name() { return "tCRI"; }
    public void destroy() {}
    public void send_request(ClientRequestInfo ri) { if (requestEx != null) throw requestEx; }
    public void send_poll(ClientRequestInfo ri) {}

    public void receive_reply(ClientRequestInfo ri) {
        assertNull(expected);
        if (replyEx != null) throw replyEx;
    }

    public void receive_other(ClientRequestInfo ri) {}

    public void receive_exception(ClientRequestInfo ri) {
        if (expected != null) {
            Any any = ri.received_exception();
            InputStream in = any.create_input_stream();
            SystemException ex = Util.readSysEx(in);
            assertEquals(expected.getClass().getName(), ex.getClass().getName());
        }
        if (exceptionEx != null) throw exceptionEx;
    }
    void throwOnRequest(SystemException ex) { requestEx = ex; }
    void noThrowOnRequest() { requestEx = null; }
    void throwOnReply(SystemException ex) { replyEx = ex; }
    void noThrowOnReply() { replyEx = null; }
    void throwOnException(SystemException ex) { exceptionEx = ex; }
    void noThrowOnException() { exceptionEx = null; }
    void expectException(SystemException ex) { expected = ex; }
    void noExpectedException() { expected = null; }
}
