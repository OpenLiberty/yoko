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
package org.apache.yoko.orb.OBPortableInterceptor;

import org.apache.yoko.orb.OB.ORBInstance;
import org.apache.yoko.orb.OB.ObjectFactory;
import org.apache.yoko.orb.OB.ObjectKey;
import org.apache.yoko.orb.OB.ObjectKeyData;
import org.apache.yoko.orb.OCI.AccFactory;
import org.apache.yoko.orb.OCI.AccFactoryRegistry;
import org.omg.IOP.IOR;
import org.omg.IOP.IORHolder;
import org.omg.IOP.TaggedProfile;

import static java.lang.System.arraycopy;

//
// The Transient ObjectReferenceTemplate
//
final public class TransientORT_impl extends TransientORT {
    //
    // The ORBInstance object
    //
    private ORBInstance orbInstance_;

    // ------------------------------------------------------------------
    // Public member implementations
    // ------------------------------------------------------------------

    public TransientORT_impl(ORBInstance orbInstance) {
        orbInstance_ = orbInstance;
    }

    public TransientORT_impl(ORBInstance orbInstance,
                             String serverId, String orbId, String[] adapterName,
                             int createTime, IOR iorTemplate) {
        orbInstance_ = orbInstance;

        the_server_id = serverId;
        the_orb_id = orbId;
        the_adapter_name = adapterName;
        the_create_time = createTime;
        the_ior_template = iorTemplate;
    }

    // ------------------------------------------------------------------
    // Private member implementation
    // ------------------------------------------------------------------

    private org.omg.CORBA.Object makeObject(String repoid, byte[] id,
            String[] name) {
        //
        // Create the actual ORBacus ObjectKey
        //
        // TODO:
        // CreatePersistentObjectKey/CreateTransientObjectKey instead of
        // populating this ObjectKey data to avoid the copy?
        //
        ObjectKeyData obkey = new ObjectKeyData();
        obkey.serverId = the_server_id;
        if (obkey.serverId.length() == 0)
            obkey.serverId = "_RootPOA";
        obkey.poaId = name;
        obkey.oid = id;
        obkey.persistent = false;
        obkey.createTime = the_create_time;

        byte[] key = ObjectKey.CreateObjectKey(obkey);

        IOR ior = new IOR();
        ior.type_id = repoid;
        ior.profiles = new TaggedProfile[the_ior_template.profiles.length];
        for (int profile = 0; profile < the_ior_template.profiles.length; ++profile) {
            ior.profiles[profile] = new TaggedProfile();
            ior.profiles[profile].tag = the_ior_template.profiles[profile].tag;
            ior.profiles[profile].profile_data = new byte[the_ior_template.profiles[profile].profile_data.length];
            arraycopy(the_ior_template.profiles[profile].profile_data,
                    0, ior.profiles[profile].profile_data, 0,
                    ior.profiles[profile].profile_data.length);
        }
        IORHolder iorH = new IORHolder(ior);

        AccFactoryRegistry registry = orbInstance_
                .getAccFactoryRegistry();
        AccFactory[] factories = registry
                .get_factories();
        for (int i = 0; i < factories.length; ++i)
            factories[i].change_key(iorH, key);

        //
        // Create and return reference
        //
        ObjectFactory objectFactory = orbInstance_
                .getObjectFactory();
        return objectFactory.createObject(iorH.value);
    }

    // ------------------------------------------------------------------
    // Standard IDL to Java mapping
    // ------------------------------------------------------------------

    public String server_id() {
        return the_server_id;
    }

    public String orb_id() {
        return the_orb_id;
    }

    public String[] adapter_name() {
        String[] result = new String[the_adapter_name.length];
        arraycopy(the_adapter_name, 0, result, 0,
                the_adapter_name.length);
        return result;
    }

    public org.omg.CORBA.Object make_object(String repoid, byte[] id) {
        return makeObject(repoid, id, the_adapter_name);
    }

    public org.omg.CORBA.Object make_object_for(String repoid, byte[] id,
            String[] adapterName) {
        return makeObject(repoid, id, adapterName);
    }
}
