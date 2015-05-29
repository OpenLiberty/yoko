/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
*  contributor license agreements.  See the NOTICE file distributed with
*  this work for additional information regarding copyright ownership.
*  The ASF licenses this file to You under the Apache License, Version 2.0
*  (the "License"); you may not use this file except in compliance with
*  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.yoko.orb.OB;

import static org.apache.yoko.orb.OCI.GiopVersion.GIOP1_0;

import org.apache.yoko.orb.OCI.GiopVersion;

public interface OB_Extras {
    //
    // Whether or not we are building a server capable of the
    // legacy compatible wide-char marshaling/unmarshaling
    // 
    public final boolean COMPAT_WIDE_MARSHAL = false;

    //
    // the default GIOP Version to set for the streams
    //
    public final GiopVersion DEFAULT_GIOP_VERSION = GIOP1_0;
}
