/*
 * Copyright 2021 IBM Corporation and others.
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

import static org.apache.yoko.util.Hex.formatHexLine;

//
// The data contained in an object key
//
final public class ObjectKeyData {
    public String serverId; // The Server Id

    public String[] poaId; // The POA to which this key refers

    public byte[] oid; // The object-id to which this key refers

    public boolean persistent; // Is the POA that created this key persistent?

    public int createTime; // If transient, what time was the POA created?

    public ObjectKeyData() {
    }

    public ObjectKeyData(String _serverId, String[] _poaId, byte[] _oid,
            boolean _persistent, int _createTime) {
        serverId = _serverId;
        poaId = _poaId;
        oid = _oid;
        persistent = _persistent;
        createTime = _createTime;
    }
    
    public String toString() {
        StringBuilder buf = new StringBuilder(); 
        buf.append(serverId); 
        buf.append(':'); 
        if (poaId != null) {
            for (int i = 0; i < poaId.length; i++) {
                buf.append('/'); 
                buf.append(poaId[i]); 
            }
        }
        buf.append(':'); 
        if (oid != null) {
            formatHexLine(oid, buf);
        }
        return buf.toString(); 
    }
}
