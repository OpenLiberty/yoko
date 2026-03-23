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
package org.apache.yoko.orb.OBPortableServer;

import static org.apache.yoko.orb.OB.ObjectKey.ParseObjectKey;
import static org.apache.yoko.orb.OB.Server.Blocking;
import static org.apache.yoko.orb.OB.Server.Threaded;
import static org.apache.yoko.util.Assert.ensure;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;
import static org.omg.PortableServer.POAManagerPackage.State._ACTIVE;
import static org.omg.PortableServer.POAManagerPackage.State._INACTIVE;

import java.util.Hashtable;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.yoko.orb.CORBA.Delegate;
import org.apache.yoko.orb.OB.BootManager_impl;
import org.apache.yoko.orb.OB.CollocatedServer;
import org.apache.yoko.orb.OB.InitialServiceManager;
import org.apache.yoko.orb.OB.LocationForward;
import org.apache.yoko.orb.OB.OAInterface;
import org.apache.yoko.orb.OB.ORBInstance;
import org.apache.yoko.orb.OB.ObjectKeyData;
import org.apache.yoko.orb.OB.PIManager;
import org.apache.yoko.orb.OB.RefCountPolicyList;
import org.apache.yoko.orb.OB.ServerManager;
import org.apache.yoko.orb.OCI.Acceptor;
import org.apache.yoko.orb.PortableServer.PoaCurrentImpl;
import org.apache.yoko.util.Assert;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_POLICY_TYPE;
import org.omg.CORBA.BAD_POLICY_VALUE;
import org.omg.CORBA.INITIALIZE;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.Object;
import org.omg.CORBA.Policy;
import org.omg.CORBA.PolicyError;
import org.omg.CORBA.TRANSIENT;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CORBA.portable.ObjectImpl;
import org.omg.GIOP.Version;
import org.omg.IOP.IOR;
import org.omg.PortableInterceptor.ACTIVE;
import org.omg.PortableInterceptor.DISCARDING;
import org.omg.PortableInterceptor.HOLDING;
import org.omg.PortableInterceptor.INACTIVE;
import org.omg.PortableServer.CurrentPackage.NoContext;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAManagerPackage.State;

final public class POAManager_impl extends LocalObject implements POAManager {
    static final Logger logger = Logger.getLogger(POAManager_impl.class.getName());
    private final ORBInstance orbInstance;
    private final Hashtable<POANameHasher, org.omg.PortableServer.POA> poas;
    private volatile State state;
    private Acceptor[] acceptors;
    private final ServerManager serverManager;
    private final String id;
    String adapterManagerId;
    private final OAInterface oaInterface;
    private final Version giopVersion = new Version();
    private final BootManager_impl bootManager;
    private final POALocator poaLocator;
    private final String serverId;


    // ------------------------------------------------------------------
    // POAManager_impl private and protected member implementations
    // ------------------------------------------------------------------

    // If we're in the context of a method invocation returns true, false otherwise.
    private boolean isInORBUpcall() {
        // Find out whether we're inside a method invocation
        try {
            InitialServiceManager ism = orbInstance.getInitialServiceManager();
            PoaCurrentImpl current = (PoaCurrentImpl) ism.resolveInitialReferences("POACurrent");

            if (current._OB_inUpcall()) {
                // Check whether the request is dispatched in this POAManager's ORB or another ORB.
                return ((POA_impl) current.get_POA())._OB_ORBInstance() == orbInstance;
            }
        } catch (NoContext | InvalidName | ClassCastException ignored) {}
        return false;
    }

    // Wait for all pending requests to complete
    private void waitPendingRequests() {
        // Wait for all pending requests from all POAs to complete
        poas.values().stream().map(POA_impl.class::cast).forEach(POA_impl::_OB_waitPendingRequests);
    }

    // Etherealize each of the servants associated with each POA
    private void etherealizePOAs() {
        try {
            InitialServiceManager initialServiceManager = orbInstance.getInitialServiceManager();
            POA_impl rootPOA = (POA_impl) initialServiceManager.resolveInitialReferences("RootPOA");

            // Etherealize recursively from the RootPOA and only POAs associated to this POAManager.
            rootPOA._OB_etherealize(this);
        } catch (InvalidName ignored) {}
    }

    // ------------------------------------------------------------------
    // Standard IDL to Java Mapping
    // ------------------------------------------------------------------

    public synchronized void activate() throws AdapterInactive {
        logger.fine(() -> "Activating POAManager " + id + " current state is " + state);
        // If the POA manager is in inactive state then raise the AdapterInactive exception
        switch (state.value()) {
            case _INACTIVE:
                throw new AdapterInactive();
            case _ACTIVE:
                logger.fine(() -> "POAManager already active, returning");
                return;
        }
        state = State.ACTIVE;

        // Notify all of a state transition
        notifyAll();

        serverManager.activate();

        // Tell the OAInterface to accept requests
        oaInterface.activate();

        // Call the state change interceptor
        PIManager piManager = orbInstance.getPIManager();
        piManager.adapterManagerStateChange(adapterManagerId, _OB_getAdapterState());
    }

    public void hold_requests(boolean waitCompletion) throws AdapterInactive {
        synchronized (this) {
            switch (state.value()) {
                case State._INACTIVE: throw new AdapterInactive();
                case State._HOLDING: return;
            }

            if (waitCompletion && isInORBUpcall())
                throw new BAD_INV_ORDER("Invocation in progress", 0, COMPLETED_NO);

            state = State.HOLDING;

            // Notify all of a state transition
            notifyAll();

            serverManager.hold();

            // Tell the OAInterface to accept requests
            oaInterface.activate();

            // Call the state change interceptor
            PIManager piManager = orbInstance.getPIManager();
            piManager.adapterManagerStateChange(adapterManagerId, _OB_getAdapterState());
        }

        // Wait for all pending requests to complete, if asked
        if (waitCompletion) waitPendingRequests();
    }

    public void discard_requests(boolean waitCompletion) throws AdapterInactive {
        synchronized (this) {
            switch (state.value()) {
                case State._INACTIVE: throw new AdapterInactive();
                case State._DISCARDING: return;
            }

            if (waitCompletion && isInORBUpcall())
                throw new BAD_INV_ORDER("Invocation in progress", 0, COMPLETED_NO);

            state = State.DISCARDING;

            notifyAll();
            oaInterface.discard();
            serverManager.activate();

            PIManager piManager = orbInstance.getPIManager();
            piManager.adapterManagerStateChange(adapterManagerId, _OB_getAdapterState());
        }

        // Wait for all pending requests to complete, if asked
        if (waitCompletion) waitPendingRequests();
    }

    public void deactivate(boolean etherealize, boolean waitCompletion) throws AdapterInactive {
        synchronized (this) {
            if (state.value() == State._INACTIVE) return;

            if (waitCompletion && isInORBUpcall())
                throw new BAD_INV_ORDER("Invocation in progress", 0, COMPLETED_NO);
            serverManager.destroy();

            // Clear the acceptor sequence
            acceptors = null;

            // Set the state to INACTIVE *after* the serverManager has been destroyed, to avoid a race condition.
            state = State.INACTIVE;

            notifyAll();

            // Call the state change interceptor
            PIManager piManager = orbInstance.getPIManager();
            piManager.adapterManagerStateChange(adapterManagerId, _OB_getAdapterState());
        }

        if (waitCompletion) waitPendingRequests();
        if (etherealize) etherealizePOAs();
    }

    public State get_state() { return state; }

    // Mapping for OBPortableServer::POAManager
    public String get_id() { return id; }

    public synchronized Acceptor[] get_acceptors() throws AdapterInactive {
        if (state.value() == State._INACTIVE) throw new AdapterInactive();

        Acceptor[] result = new Acceptor[acceptors.length];
        System.arraycopy(acceptors, 0, result, 0, acceptors.length);
        return result;
    }

    // ------------------------------------------------------------------
    // Yoko internal functions
    // Application programs must not use these functions directly
    // ------------------------------------------------------------------
    POAManager_impl(ORBInstance orbInstance,
                    POALocator poaLocator, String id, String adapterManagerId,
                    Acceptor[] acceptors,
                    Policy[] policies) throws PolicyError {
        this.orbInstance = orbInstance;
        this.poas = new Hashtable<>(63);
        this.state = State.HOLDING;
        this.acceptors = acceptors;
        this.id = id;
        this.adapterManagerId = adapterManagerId;
        this.poaLocator = poaLocator;
        this.serverId = Optional.of(orbInstance.getServerId()).filter(s -> !s.isEmpty()).orElse("_RootPOA");
        this.oaInterface = new POAOAInterface_impl(this, this.orbInstance);

        // Construct the root name of the property key for this POAManager instance
        String rootKey = "yoko.orb.poamanager." + this.id + ".";
        Properties properties = orbInstance.getProperties();

        // If policies are provided, they will take precedence over the configuration properties.
        CommunicationsConcurrencyPolicy commsPolicy = null;
        GIOPVersionPolicy giopPolicy = null;
        for (Policy policy : policies) {
            switch (policy.policy_type()) {
                case COMMUNICATIONS_CONCURRENCY_POLICY_ID.value:
                    commsPolicy = CommunicationsConcurrencyPolicyHelper.narrow(policy);
                    break;
                case GIOP_VERSION_POLICY_ID.value:
                    giopPolicy = GIOPVersionPolicyHelper.narrow(policy);
                    break;
                default:
                    throw new PolicyError(BAD_POLICY_TYPE.value);
            }
        }

        // Check over the POAManager properties and find out whether an unknown property is present
        properties.keySet()
                .stream()
                .map(String.class::cast)
                .forEach(key -> {
                    validateProp(rootKey, key);
                });

        giopVersion.major = 1;
        giopVersion.minor = 2;
        final int concModel;

        if (commsPolicy == null) {
            validateConcModel(properties, rootKey);
            concModel = Threaded;
        } else {
            switch (commsPolicy.value()) {
                case COMMUNICATIONS_CONCURRENCY_POLICY_REACTIVE.value: concModel = Blocking; break;
                case COMMUNICATIONS_CONCURRENCY_POLICY_THREADED.value: concModel = Threaded; break;
                default: throw new PolicyError(BAD_POLICY_VALUE.value);
            }
        }

        if (giopPolicy == null) {
            extractGiopVersion(properties, rootKey, giopVersion);
        } else {
            switch (giopPolicy.value()) {
                case GIOP_VERSION_POLICY_1_0.value: giopVersion.major = 1; giopVersion.minor = 0; break;
                case GIOP_VERSION_POLICY_1_1.value: giopVersion.major = 1; giopVersion.minor = 1; break;
                case GIOP_VERSION_POLICY_1_2.value: giopVersion.major = 1; giopVersion.minor = 2; break;
                default: throw new PolicyError(BAD_POLICY_VALUE.value);
            }
        }

        serverManager = new ServerManager(this.orbInstance, this.acceptors, oaInterface, concModel);
        bootManager = (BootManager_impl) orbInstance.getBootManager();
    }

    private static void validateConcModel(Properties properties, String rootKey) {
        // First check the specific POAManager key
        String fullKey = rootKey + "conc_model";
        String value = properties.getProperty(fullKey);

        if (value == null) {
            fullKey = "yoko.orb.oa.conc_model";
            value = properties.getProperty(fullKey);
            if (value == null) return;
        }

        // Technically the only valid values for
        // yoko.orb.poamanager.*.conc_model are "reactive" and
        // "threaded" since this is the communications conc model.
        // However, we'll also accept the following values since
        // we might be parsing "yoko.orb.oa.conc_model" (which
        // represents the default value for both the comm conc
        // model *and* the method dispatch model).
        switch (value) {
            case "threaded":
            case "thread_per_client":
            case "thread_per_request":
            case "thread_pool":
                return;
        }
        String finalFullKey = fullKey;
        logger.warning(() -> finalFullKey + ": unknown value");
    }

    private static void extractGiopVersion(Properties properties, String rootKey, Version giopVersion) {
        // First check the specific POAManager key
        String fullKey = rootKey + "version";
        String value = properties.getProperty(fullKey);

        if (value == null) {
            fullKey = "yoko.orb.oa.version";
            value = properties.getProperty(fullKey);
            if (value == null) return;
        }

        switch (value) {
            case "1.0": giopVersion.major = 1; giopVersion.minor = 0; return;
            case "1.1": giopVersion.major = 1; giopVersion.minor = 1; return;
            case "1.2": giopVersion.major = 1; giopVersion.minor = 2; return;
        }
        String err = fullKey + ": expected `1.0', `1.1' or `1.2'";
        logger.severe(err);
        throw new INITIALIZE(err);
    }

    private static void validateProp(String rootKey, String key) {
        if (key.equals("yoko.orb.oa.thread_pool")) return;
        // Remove the property prefix
        final String prop;
        if (key.startsWith(rootKey)) prop = key.substring(rootKey.length());
        else if (key.startsWith("yoko.orb.oa.")) prop = key.substring("yoko.orb.oa.".length());
        else return;

        // Check for a match among the supported properties
        switch (prop) {
            case "conc_model": return;
            case "endpoint": return;
            case "version": return;
        }
        String err = key + ": unknown property";
        logger.warning(() -> err);
    }

    // Register a POA with this POAManager
    synchronized void _OB_addPOA(org.omg.PortableServer.POA poa, String[] id) {
        POANameHasher idKey = new POANameHasher(id);

        logger.fine(() -> "Adding new poa with id " + idKey);
        ensure(!poas.containsKey(idKey));
        poas.put(idKey, poa);

        poaLocator.add(poa, id);
    }

    // Un-register a POA with this POAManager
    synchronized void _OB_removePOA(String[] id) {
        POANameHasher idKey = new POANameHasher(id);
        logger.fine(() -> "Removing poa with id " + idKey);
        ensure(poas.containsKey(idKey));
        poas.remove(idKey);
        poaLocator.remove(id);
    }

    DirectServant _OB_getDirectServant(byte[] key, RefCountPolicyList policies) throws LocationForward, AdapterInactive {
        if (state.value() == State._INACTIVE) throw new AdapterInactive();
        ObjectKeyData data = new ObjectKeyData();
        
        if (ParseObjectKey(key, data)) {
            org.omg.PortableServer.POA poa;
            synchronized (this) { poa = _OB_locatePOA(data); }
            if (poa != null) return ((POA_impl) poa)._OB_getDirectServant(data.oid, policies);
        }

        // Check to see if the BootManager knows of a reference for the ObjectKey. If so, forward the request.
        synchronized (this) {
            IOR ior = bootManager._OB_locate(key);
            if (ior != null) throw new LocationForward(ior, false);
        }
        throw new OBJECT_NOT_EXIST("No POA for local servant");
    }

    org.omg.PortableServer.POA _OB_locatePOA(ObjectKeyData data) throws LocationForward {
        // If the GIOP engine sends a request while the POAManager is in INACTIVE state, then something is wrong.
        ensure(get_state() != State.INACTIVE);
        logger.fine(() -> "Searching for direct servant with key " + data);

        if (!data.serverId.equals(serverId)) return null;
        POANameHasher key = new POANameHasher(data.poaId);
        logger.fine(() -> "Searching for direct servant with poa key " + key);
        POA poa = poas.get(key);
        if (poa == null) {
            // The POA isn't contained in our local POA table. Ask the POALocator to locate the POA.
            poa = poaLocator.locate(data);

            // If the POA is connected to some other POAManager (and hence some other end-point) then location forward
            if (poa != null) {
                logger.fine(() -> "Attempting to obtain a local reference to an object activated on a different POA");
                org.omg.PortableServer.POAManager manager = poa.the_POAManager();
                if (manager != this) {
                    Object obj = poa.create_reference_with_id(data.oid, "");

                    Delegate p = (Delegate) ((ObjectImpl) obj)._get_delegate();
                    IOR ior = p._OB_IOR();
                    throw new LocationForward(ior, false);
                }
            }
        }
        // If the POA doesn't match the ObjectKeyData then this POA isn't present.
        if (poa != null) {
            POA_impl poaImpl = (POA_impl) poa;
            if (!poaImpl._OB_poaMatches(data, false)) {
                logger.fine(() -> "POA located but object key data doesn't match");
                poa = null;
            }
        }
        return poa;
    }

    public CollocatedServer _OB_getCollocatedServer() { return serverManager.getCollocatedServer(); }

    public synchronized void _OB_validateState() {
       for (;;) {
           // If POAManager::activate() has been called then we're done
           switch (state.value()) {
               case State._ACTIVE: return;
               case State._INACTIVE: //fall through
               case State._DISCARDING:
                   throw new TRANSIENT("POAManager is inactive or discarding requests", 0, COMPLETED_NO);
           }

           // Wait for a state transition
           try {
               wait();
           } catch (InterruptedException ignored) {}
        }
    }

    Version _OB_getGIOPVersion() { return new Version(giopVersion.major, giopVersion.minor); }

    String _OB_getAdapterManagerId() { return adapterManagerId; }

    short _OB_getAdapterState() {
        switch (state.value()) {
            case State._INACTIVE: return INACTIVE.value;
            case State._ACTIVE: return ACTIVE.value;
            case State._HOLDING: return HOLDING.value;
            case State._DISCARDING: return DISCARDING.value;
        }
        throw Assert.fail();
    }

    public Acceptor[] _OB_getAcceptors() {
        Acceptor[] result = new Acceptor[acceptors.length];
        System.arraycopy(acceptors, 0, result, 0, acceptors.length);
        return result;
    }

    public ServerManager _OB_getServerManager() { return serverManager; }

    public OAInterface _OB_getOAInterface() { return oaInterface; }
}
