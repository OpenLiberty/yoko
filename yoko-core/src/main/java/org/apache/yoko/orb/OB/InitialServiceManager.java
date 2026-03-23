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

import org.apache.yoko.util.Assert;
import org.omg.CORBA.INITIALIZE;
import org.omg.CORBA.INV_POLICY;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CORBA.Policy;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.apache.yoko.util.Assert.ensure;
import static org.apache.yoko.util.MinorCodes.MinorORBDestroyed;
import static org.apache.yoko.util.MinorCodes.describeInitialize;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;
import static org.omg.CORBA.SetOverrideType.SET_OVERRIDE;

public final class InitialServiceManager {
    static final Logger logger = Logger.getLogger(InitialServiceManager.class.getName());

    //
    // Set of available initial services
    //
    private static class Service {
        String ref;
        org.omg.CORBA.Object obj;
    }

    private Hashtable<String, Service> services_ = new Hashtable<>(37);

    private String defaultInitRef_;

    private boolean destroy_ = false; // True if destroy() was called

    private ORBInstance orbInstance_; // The ORBInstance object

    // ----------------------------------------------------------------------
    // InitialServiceManager private and protected member implementations
    // ----------------------------------------------------------------------

    protected void finalize() throws Throwable {
        Assert.ensure(destroy_);
        super.finalize();
    }

    // ----------------------------------------------------------------------
    // InitialServiceManager package member implementations
    // ----------------------------------------------------------------------

    synchronized void destroy() {
        Assert.ensure(!destroy_); // May only be destroyed once
        destroy_ = true;

        services_ = null;
        orbInstance_ = null;
    }

    // ----------------------------------------------------------------------
    // InitialServiceManager public member implementations
    // ----------------------------------------------------------------------

    public InitialServiceManager() {
    }

    //
    // Set the ORBInstance object. Note that the initial service map
    // isn't populated until this method is called.
    //
    public void setORBInstance(ORBInstance instance) {
        orbInstance_ = instance;

        //
        // Populate the services map
        //
        Properties properties = orbInstance_.getProperties();

        //
        // Obtain the INS default initial reference URL
        //
        String value = properties.getProperty("yoko.orb.default_init_ref");
        if (value == null)
            defaultInitRef_ = "";
        else
            defaultInitRef_ = value;

        //
        // Add those services configured in the "yoko.orb.service" property
        //
        String propRoot = "yoko.orb.service.";
        Enumeration<Object> keys = properties.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            if (!key.startsWith(propRoot))
                continue;

            value = properties.getProperty(key);
            Assert.ensure(value != null);
            key = key.substring(propRoot.length());
            try {
                addInitialReference(key, value, true);
            } catch (InvalidName ex) {
                throw Assert.fail(ex);
            }
        }

    }

    public synchronized String[] listInitialServices() {
        //
        // The ORB destroys this object, so it's an initialization error
        // if this operation is called after ORB destruction
        //
        if (destroy_)
            throw new INITIALIZE(describeInitialize(MinorORBDestroyed),
                    MinorORBDestroyed,
                    COMPLETED_NO);

        String[] list = new String[services_.size()];

        int i = 0;
        Enumeration<String> e = services_.keys();
        while (e.hasMoreElements())
            list[i++] = e.nextElement();

        return list;
    }

    public synchronized org.omg.CORBA.Object resolveInitialReferences(String identifier) throws InvalidName {
        //
        // The ORB destroys this object, so it's an initialization error
        // if this operation is called after ORB destruction
        //
        if (destroy_) {
            throw new INITIALIZE(describeInitialize(MinorORBDestroyed),
                    MinorORBDestroyed,
                    COMPLETED_NO);
        }

        ensure(identifier != null);

        logger.fine(() -> "Resolving initial ORB reference for " + identifier);

        ObjectFactory objectFactory = orbInstance_.getObjectFactory();

        org.omg.CORBA.Object obj = null;

        //
        // Search the list of initial references
        //
        Service svc = services_.get(identifier);
        if (svc != null) {
            if (svc.obj != null) {
                obj = svc.obj;
            } else if (!svc.ref.isEmpty()) {
                obj = objectFactory.stringToObject(svc.ref);
                svc.obj = obj;
                services_.put(identifier, svc);
            }
        }

        logger.fine(() -> "No match found for ORB initial reference " + identifier);

        //
        // If no match was found, and there's a default initial
        // reference "template", then try to compose a URL using
        // the identifier as the object-key. However, we only do
        // this if the service really doesn't exist in our table,
        // since there could be a service with a nil value.
        //
        if (obj == null && !defaultInitRef_.isEmpty() && !services_.containsKey(identifier)) {
            String url = defaultInitRef_ + '/' + identifier;
            obj = objectFactory.stringToObject(url);
        }

        if (obj == null) {
            logger.fine(() -> "No default initializer found for ORB initial reference " + identifier);
            throw new InvalidName();
        }

        //
        // If the object is an l-c object, return the object now
        //
        if (obj instanceof LocalObject) {
            return obj;
        }

        // If the object is remote, return a new reference with the
        // current set of policies applied, but only set ORB policies
        // if they are not already set on the object
        Policy[] policies = Stream.of(objectFactory.policies())
                .map(getPolicyOverrider(obj))
                .toArray(Policy[]::new);
        return obj._set_policy_override(policies, SET_OVERRIDE);
    }

    /**
     * For a given default policy, find the object-specific override or return the default
     */
    private static Function<Policy, Policy> getPolicyOverrider(org.omg.CORBA.Object o) {
        return p -> {
            try {
                return o._get_policy(p.policy_type());
            } catch (INV_POLICY ignored) {
                return p;
            }
        };
    }

    public void addInitialReference(String name, org.omg.CORBA.Object obj) throws InvalidName {
        addInitialReference(name, obj, false);
    }

    public synchronized void addInitialReference(String name, String iorString, boolean override) throws InvalidName {
        logger.fine(() -> "Adding initial reference name=" + name + ", ior=" + iorString);
        //
        // The ORB destroys this object, so it's an initialization error
        // if this operation is called after ORB destruction
        //
        if (destroy_) throw new INITIALIZE(describeInitialize(MinorORBDestroyed),
                    MinorORBDestroyed,
                    COMPLETED_NO);

        ensure(name != null && iorString != null);

        if (services_.containsKey(name) && !override) {
            logger.fine(() -> "Initial reference name=" + name + "already exists");
            throw new InvalidName();
        }

        Service svc = new Service();
        svc.ref = iorString;
        services_.put(name, svc);
    }

    public synchronized void addInitialReference(String name,
            org.omg.CORBA.Object p, boolean override)
            throws InvalidName {
        if (p != null) {
            logger.fine(() -> "Adding initial reference name=" + name + " of type " + p.getClass().getName());
        }
        else {
            logger.fine(() -> "Adding initial reference name=" + name + " with null implementation");
        }
        //
        // The ORB destroys this object, so it's an initialization error
        // if this operation is called after ORB destruction
        //
        if (destroy_) {
            throw new INITIALIZE(describeInitialize(MinorORBDestroyed),
                    MinorORBDestroyed,
                    COMPLETED_NO);
        }

        Assert.ensure(name != null);

        if (services_.containsKey(name) && !override) {
            throw new InvalidName();
        }

        Service svc = new Service();
        svc.ref = "";
        svc.obj = p;
        services_.put(name, svc);
    }
}
