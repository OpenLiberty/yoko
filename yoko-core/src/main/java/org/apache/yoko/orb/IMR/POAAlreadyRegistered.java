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
package org.apache.yoko.orb.IMR;

import org.omg.CORBA.UserException;

//
// IDL:orb.yoko.apache.org/IMR/POAAlreadyRegistered:1.0
//
/**
 *
 * A POAAlreadyRegistered exception indicates that the POA detailed in
 * the member <code>poa</code> doesn't exist in the server specified
 * in in the member <code>name</code>.
 *
 **/

public final class POAAlreadyRegistered extends UserException
{
    private static final String _ob_id = "IDL:orb.yoko.apache.org/IMR/POAAlreadyRegistered:1.0";

    public
    POAAlreadyRegistered()
    {
        super(_ob_id);
    }

    public
    POAAlreadyRegistered(String name,
                         String[] poa)
    {
        super(_ob_id);
        this.name = name;
        this.poa = poa;
    }

    public
    POAAlreadyRegistered(String _reason,
                         String name,
                         String[] poa)
    {
        super(_ob_id + " " + _reason);
        this.name = name;
        this.poa = poa;
    }

    public String name;
    public String[] poa;
}
