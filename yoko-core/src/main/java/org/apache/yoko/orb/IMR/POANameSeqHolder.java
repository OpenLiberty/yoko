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

import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CORBA.portable.Streamable;

//
// IDL:orb.yoko.apache.org/IMR/POANameSeq:1.0
//
public final class POANameSeqHolder implements Streamable
{
    public String[][] value;

    public
    POANameSeqHolder()
    {
    }

    public
    POANameSeqHolder(String[][] initial)
    {
        value = initial;
    }

    public void
    _read(InputStream in)
    {
        value = POANameSeqHelper.read(in);
    }

    public void
    _write(OutputStream out)
    {
        POANameSeqHelper.write(out, value);
    }

    public TypeCode
    _type()
    {
        return POANameSeqHelper.type();
    }
}
