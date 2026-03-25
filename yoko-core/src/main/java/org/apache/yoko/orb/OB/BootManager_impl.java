/*
 * Copyright 2026 IBM Corporation and others.
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
package org.apache.yoko.orb.OB;

import org.apache.yoko.orb.CORBA.Delegate;
import org.apache.yoko.orb.OB.BootManagerPackage.AlreadyExists;
import org.apache.yoko.orb.OB.BootManagerPackage.NotFound;
import org.omg.CORBA.BooleanHolder;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CORBA.ObjectHolder;
import org.omg.CORBA.portable.ObjectImpl;
import org.omg.IOP.IOR;

import java.util.Hashtable;
import java.util.logging.Logger;

import static org.apache.yoko.util.Hex.formatHexPara;

public final class BootManager_impl extends LocalObject implements
        BootManager {
    static final Logger logger = Logger.getLogger(BootManager_impl.class.getName());
    //
    // Set of known bindings
    //
    private final Hashtable<ObjectIdHasher, org.omg.CORBA.Object> bindings_;

    //
    // The Boot Locator. There is no need for the BootLocatorHolder
    // since assign and read methods are atomic in Java.
    //
    private BootLocator locator_ = null;
    
    // the ORB that created us 
    private final ORB orb_;

    public BootManager_impl(ORB orb) {
        bindings_ = new Hashtable<>(17);
        orb_ = orb; 
    }

    // ------------------------------------------------------------------
    // Standard IDL to Java Mapping
    // ------------------------------------------------------------------

    public void add_binding(byte[] id, org.omg.CORBA.Object obj) throws AlreadyExists {
        ObjectIdHasher oid = new ObjectIdHasher(id);
        logger.fine(() -> "Adding binding under id " + formatHexPara(id));

        //
        // If binding id is not already mapped add the binding.
        //
        synchronized (bindings_) {
            if (bindings_.containsKey(oid))
                throw new AlreadyExists();

            bindings_.put(oid, obj);
        }
    }

    public void remove_binding(byte[] id) throws NotFound {
        ObjectIdHasher oid = new ObjectIdHasher(id);
        logger.fine(() -> "Removing binding with id " + formatHexPara(id));

        //
        // If binding id is mapped remove the binding
        //
        synchronized (bindings_) {
            if (bindings_.remove(oid) == null)
                throw new NotFound();
        }
    }

    public void set_locator(BootLocator locator) {
        //
        // Set the BootLocator
        //
        locator_ = locator;
    }

    // -------------------------------------------------------------------
    // BootManager_impl internal methods
    // ------------------------------------------------------------------

    public IOR _OB_locate(byte[] id) {
        //
        // First check the internal hash table and then the
        // registered BootLocator (if there is one) to find the
        // binding for the requested ObjectId.
        //
        ObjectIdHasher oid = new ObjectIdHasher(id);

        logger.fine(() -> "Searching for binding with id " + formatHexPara(id));
        org.omg.CORBA.Object obj = bindings_.get(oid);
        if (obj == null && locator_ != null) {
            logger.fine(() -> "Object not found, passing on to locator");
            try {
                ObjectHolder objHolder = new ObjectHolder();
                BooleanHolder addHolder = new BooleanHolder();
                locator_.locate(id, objHolder, addHolder);

                obj = objHolder.value;
                if (addHolder.value) {
                    bindings_.put(oid, obj);
                }
            } catch (NotFound ignored) {
            }
        }

        if (obj == null) {
            // these should map to initial references as well when used as a corbaloc name.
            // convert the key to a string and try for one of those 
            String keyString = new String(id);
            try {
                obj = orb_.resolve_initial_references(keyString);
            } catch (InvalidName ex) {
                // if this is not valid, it won't work 
                return null;
            }
            // just return null if still not there 
            if (obj == null) {
                return null;
            }
        }

        Delegate p = (Delegate) (((ObjectImpl) obj)
                ._get_delegate());
        return p._OB_IOR();
    }
}
