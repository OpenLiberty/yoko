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

import static org.apache.yoko.logging.VerboseLogging.REQ_IN_LOG;

import org.omg.CORBA.LocalObject;

/**
 * An UnknownExceptionStrategy will be called by the ORB when a servant raises an unexpected exception
 */
public class UnknownExceptionStrategy_impl extends LocalObject implements UnknownExceptionStrategy {
    //
    // Handle an unknown exception. If this method doesn't throw
    // a SystemException, the ORB will return CORBA::UNKNOWN to
    // the client.
    //
    public void unknown_exception(UnknownExceptionInfo info) {
        String msg = "Servant method raised a non-CORBA exception";
        if (info.response_expected()) msg += "\n\tClient receives this exception as CORBA::UNKNOWN";
        msg += "\n\toperation name: \""+ info.operation() + '"';
        msg += "\n\ttransport info: " + info.transport_info();
        msg += "\n\texception: " + info.describe_exception();

        REQ_IN_LOG.warning(msg);
    }
}
