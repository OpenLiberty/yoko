/*
 * Copyright 2024 IBM Corporation and others.
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.yoko.orb.OCI.TransportInfo;
import org.omg.CORBA.LocalObject;

public class UnknownExceptionInfo_impl extends LocalObject
        implements UnknownExceptionInfo {
    private String operation_;

    private boolean responseExpected_;

    private TransportInfo transportInfo_;

    private RuntimeException ex_;

    // ------------------------------------------------------------------
    // UnknownExceptionInfo_impl constructor
    // ------------------------------------------------------------------

    public UnknownExceptionInfo_impl(String operation,
            boolean responseExpected,
            TransportInfo transportInfo,
            RuntimeException ex) {
        operation_ = operation;
        responseExpected_ = responseExpected;
        transportInfo_ = transportInfo;
        ex_ = ex;
    }

    // ------------------------------------------------------------------
    // Standard IDL to Java Mapping
    // ------------------------------------------------------------------

    public String operation() {
        return operation_;
    }

    public boolean response_expected() {
        return responseExpected_;
    }

    public TransportInfo transport_info() {
        return transportInfo_;
    }

    public String describe_exception() {
        String result = "";
        try (StringWriter sw = new StringWriter();
             PrintWriter pw = new PrintWriter(sw);) {
            ex_.printStackTrace(pw);
            pw.flush();
            result += sw;
        } catch (IOException ignored) {
        }
        return result;
    }

    public void raise_exception() {
        throw ex_;
    }
}
