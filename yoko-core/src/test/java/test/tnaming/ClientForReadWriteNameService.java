/*==============================================================================
 * Copyright 2015 IBM Corporation and others.
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
package test.tnaming;

import java.util.Properties;

import static test.tnaming.Client.NameServiceType.*;

public class ClientForReadWriteNameService {
    public static void main(String args[]) throws Exception {
        final String refFile = args[0];
        java.util.Properties props = new Properties();
        props.putAll(System.getProperties());
        props.put("org.omg.CORBA.ORBClass", "org.apache.yoko.orb.CORBA.ORB");
        props.put("org.omg.CORBA.ORBSingletonClass", "org.apache.yoko.orb.CORBA.ORBSingleton");

        try (Client client = new Client(STANDALONE, refFile, props, "-ORBInitRef", "NameService=" + Util.NS_LOC)) {
            Util.createBindingsOverWhichToIterate(client.orb, client.rootNamingContext);
            client.run();
        }
    }
}
