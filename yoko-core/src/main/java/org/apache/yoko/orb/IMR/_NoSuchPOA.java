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
// IDL:orb.yoko.apache.org/IMR/NoSuchPOA:1.0
//
/**
 *
 * A NoSuchPOA exception indicates that the poa detailed in the member
 * <code>poa</code> doesn't exist.
 *
 **/

public final class _NoSuchPOA extends UserException
{
    private static final String _ob_id = "IDL:orb.yoko.apache.org/IMR/NoSuchPOA:1.0";

    public
    _NoSuchPOA()
    {
        super(_ob_id);
    }

    public
    _NoSuchPOA(String[] poa)
    {
        super(_ob_id);
        this.poa = poa;
    }

    public
    _NoSuchPOA(String _reason,
               String[] poa)
    {
        super(_ob_id + " " + _reason);
        this.poa = poa;
    }

    public String[] poa;
}
