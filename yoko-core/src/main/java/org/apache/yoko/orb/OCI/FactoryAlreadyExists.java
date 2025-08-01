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
package org.apache.yoko.orb.OCI;

//
// IDL:orb.yoko.apache.org/OCI/FactoryAlreadyExists:1.0
//

import org.omg.CORBA.UserException;

/**
 *
 * A factory with the given plugin id already exists.
 *
 * @member id The plugin id.
 *
 **/

final public class FactoryAlreadyExists extends UserException
{
    private static final String _ob_id = "IDL:orb.yoko.apache.org/OCI/FactoryAlreadyExists:1.0";

    public
    FactoryAlreadyExists()
    {
        super(_ob_id);
    }

    public
    FactoryAlreadyExists(String id)
    {
        super(_ob_id);
        this.id = id;
    }

    public
    FactoryAlreadyExists(String _reason,
                         String id)
    {
        super(_ob_id + " " + _reason);
        this.id = id;
    }

    public String id;
}
