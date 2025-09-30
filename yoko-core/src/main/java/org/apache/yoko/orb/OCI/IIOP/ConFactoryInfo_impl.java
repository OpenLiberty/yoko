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
package org.apache.yoko.orb.OCI.IIOP;

import org.apache.yoko.orb.OCI.ConnectCB;
import org.omg.IOP.TAG_INTERNET_IOP;

import java.util.Vector;

final class ConFactoryInfo_impl extends org.omg.CORBA.LocalObject implements
        ConFactoryInfo {
    //
    // All connect callback objects
    //
    private Vector connectCBVec_ = new Vector();

    private ConnectionHelper connectionHelper_;   // the helper for managing socket connections.

    // ------------------------------------------------------------------
    // Standard IDL to Java Mapping
    // ------------------------------------------------------------------

    public String id() {
        return PLUGIN_ID.value;
    }

    public int tag() {
        return TAG_INTERNET_IOP.value;
    }

    public synchronized String describe() {
        String desc = "id: " + PLUGIN_ID.value;

        return desc;
    }

    public synchronized void add_connect_cb(ConnectCB cb) {
        int length = connectCBVec_.size();
        for (int i = 0; i < length; i++)
            if (connectCBVec_.elementAt(i) == cb)
                return; // Already registered
        connectCBVec_.addElement(cb);
    }

    public synchronized void remove_connect_cb(
            ConnectCB cb) {
        int length = connectCBVec_.size();
        for (int i = 0; i < length; i++)
            if (connectCBVec_.elementAt(i) == cb) {
                connectCBVec_.removeElementAt(i);
                return;
            }
    }

    // ------------------------------------------------------------------
    // Yoko internal functions
    // Application programs must not use these functions directly
    // ------------------------------------------------------------------

    synchronized ConnectCB[] _OB_getConnectCBSeq() {
        int length = connectCBVec_.size();
        ConnectCB[] cbs = new ConnectCB[length];
        for (int i = 0; i < length; i++) {
            ConnectCB cb = (ConnectCB) connectCBVec_
                    .elementAt(i);
            cbs[i] = cb;
        }
        return cbs;
    }
}
