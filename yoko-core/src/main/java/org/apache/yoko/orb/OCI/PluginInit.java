/*
 * Copyright 2010 IBM Corporation and others.
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
package org.apache.yoko.orb.OCI;

public interface PluginInit {
    //
    // Compatibility check. The plug-in should verify that it is
    // compatible with the given OCI version, and raise an exception
    // if not.
    //
    void version(org.omg.CORBA.ORB orb, String ver);

    //
    // Initialize the plug-in for an ORB
    //
    org.apache.yoko.orb.OCI.Plugin init(org.omg.CORBA.ORB orb,
            org.omg.CORBA.StringSeqHolder args);
}
