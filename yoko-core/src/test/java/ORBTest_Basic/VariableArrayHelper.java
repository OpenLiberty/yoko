/*==============================================================================
 * Copyright 2010 IBM Corporation and others.
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *=============================================================================*/
package ORBTest_Basic;

//
// IDL:ORBTest_Basic/VariableArray:1.0
//
final public class VariableArrayHelper
{
    public static void
    insert(org.omg.CORBA.Any any, String[][] val)
    {
        org.omg.CORBA.portable.OutputStream out = any.create_output_stream();
        write(out, val);
        any.read_value(out.create_input_stream(), type());
    }

    public static String[][]
    extract(org.omg.CORBA.Any any)
    {
        if(any.type().equivalent(type()))
            return read(any.create_input_stream());
        else
            throw new org.omg.CORBA.BAD_OPERATION();
    }

    private static org.omg.CORBA.TypeCode typeCode_;

    public static org.omg.CORBA.TypeCode
    type()
    {
        if(typeCode_ == null)
        {
            org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init();
            typeCode_ = orb.create_alias_tc(id(), "VariableArray", orb.create_array_tc(2, orb.create_array_tc(3, orb.get_primitive_tc(org.omg.CORBA.TCKind.tk_string))));
        }

        return typeCode_;
    }

    public static String
    id()
    {
        return "IDL:ORBTest_Basic/VariableArray:1.0";
    }

    public static String[][]
    read(org.omg.CORBA.portable.InputStream in)
    {
        String[][] _ob_v;
        int len0 = 2;
        _ob_v = new String[len0][];
        for(int i0 = 0; i0 < len0; i0++)
        {
            int len1 = 3;
            _ob_v[i0] = new String[len1];
            for(int i1 = 0; i1 < len1; i1++)
                _ob_v[i0][i1] = in.read_string();
        }
        return _ob_v;
    }

    public static void
    write(org.omg.CORBA.portable.OutputStream out, String[][] val)
    {
        int len0 = val.length;
        if(len0 != 2)
             throw new org.omg.CORBA.MARSHAL();
        for(int i0 = 0; i0 < len0; i0++)
        {
            int len1 = val[i0].length;
            if(len1 != 3)
                 throw new org.omg.CORBA.MARSHAL();
            for(int i1 = 0; i1 < len1; i1++)
                out.write_string(val[i0][i1]);
        }
    }
}
