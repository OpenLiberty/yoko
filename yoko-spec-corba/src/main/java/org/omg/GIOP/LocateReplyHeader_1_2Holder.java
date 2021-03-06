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

package org.omg.GIOP;

//
// IDL:omg.org/GIOP/LocateReplyHeader_1_2:1.0
//
final public class LocateReplyHeader_1_2Holder implements org.omg.CORBA.portable.Streamable
{
    public LocateReplyHeader_1_2 value;

    public
    LocateReplyHeader_1_2Holder()
    {
    }

    public
    LocateReplyHeader_1_2Holder(LocateReplyHeader_1_2 initial)
    {
        value = initial;
    }

    public void
    _read(org.omg.CORBA.portable.InputStream in)
    {
        value = LocateReplyHeader_1_2Helper.read(in);
    }

    public void
    _write(org.omg.CORBA.portable.OutputStream out)
    {
        LocateReplyHeader_1_2Helper.write(out, value);
    }

    public org.omg.CORBA.TypeCode
    _type()
    {
        return LocateReplyHeader_1_2Helper.type();
    }
}
