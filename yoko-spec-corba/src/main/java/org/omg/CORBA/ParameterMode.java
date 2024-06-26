/*
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
package org.omg.CORBA;

//
// IDL:omg.org/CORBA/ParameterMode:1.0
//
/***/

public class ParameterMode implements org.omg.CORBA.portable.IDLEntity
{
    private static ParameterMode [] values_ = new ParameterMode[3];
    private int value_;

    public final static int _PARAM_IN = 0;
    public final static ParameterMode PARAM_IN = new ParameterMode(_PARAM_IN);
    public final static int _PARAM_OUT = 1;
    public final static ParameterMode PARAM_OUT = new ParameterMode(_PARAM_OUT);
    public final static int _PARAM_INOUT = 2;
    public final static ParameterMode PARAM_INOUT = new ParameterMode(_PARAM_INOUT);

    protected
    ParameterMode(int value)
    {
        values_[value] = this;
        value_ = value;
    }

    public int
    value()
    {
        return value_;
    }

    public static ParameterMode
    from_int(int value)
    {
        if(value < values_.length)
            return values_[value];
        else
            throw new org.omg.CORBA.BAD_PARAM("Value (" + value  + ") out of range", 25, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
    }

    private java.lang.Object
    readResolve()
        throws java.io.ObjectStreamException
    {
        return from_int(value());
    }
}
