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

package test.obv;

public class TestTrunc1Factory_impl implements
        org.omg.CORBA.portable.ValueFactory {
    public java.io.Serializable read_value(
            org.omg.CORBA_2_3.portable.InputStream in) {
        return in.read_value(new TestTrunc1_impl());
    }

    public static void install(org.omg.CORBA.ORB orb) {
        org.omg.CORBA_2_3.ORB orb_2_3 = (org.omg.CORBA_2_3.ORB) orb;
        orb_2_3.register_value_factory(TestTrunc1Helper.id(),
                new TestTrunc1Factory_impl());
    }
}
