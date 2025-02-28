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
package org.apache.yoko.orb.CosNaming.tnaming;

import static org.omg.PortableServer.IdAssignmentPolicyValue.SYSTEM_ID;
import static org.omg.PortableServer.LifespanPolicyValue.TRANSIENT;
import static org.omg.PortableServer.ServantRetentionPolicyValue.RETAIN;

import java.util.Properties;

import org.apache.yoko.orb.OB.BootManager;
import org.apache.yoko.orb.OB.BootManagerHelper;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.ServantRetentionPolicyValue;

/**
 * A transient name service attached to an ORB. This class manages all of the
 * housekeeping for creating a TransientNamingContext and a exposing it using an
 * ORB.
 */
public class TransientNameService implements AutoCloseable {
    // the default registered name service
    public static final String DEFAULT_SERVICE_NAME = "TNameService";
    // the default listening port
    public static final int DEFAULT_SERVICE_PORT = 900;
    // the default host name
    public static final String DEFAULT_SERVICE_HOST = "localhost";

    // the service root context
    protected TransientNamingContext initialContext;
    // initial listening port
    protected int port;
    // initial listening host
    protected String host;
    // the service name (used for registering for the corbaloc:: URL name
    protected String serviceName;
    // the orb instance we're running on
    protected ORB createdOrb;

    /**
     * Create a new TransientNameService, using all default attributes.
     */
    public TransientNameService() {
        this(DEFAULT_SERVICE_HOST, DEFAULT_SERVICE_PORT, DEFAULT_SERVICE_NAME);
    }

    /**
     * Create a default-named name service using the specified host and port
     * parameters.
     * @param host The host to expose this under.
     * @param port The initial listening port.
     */
    public TransientNameService(String host, int port) {
        this(host, port, DEFAULT_SERVICE_NAME);
    }

    /**
     * Create a specifically-named name service using the specified host and
     * port parameters.
     * @param host The host to expose this under.
     * @param port The initial listening port.
     * @param name The name to register this service under using the
     *            BootManager.
     */
    public TransientNameService(String host, int port, String name) {
        this.port = port;
        this.host = host;
        this.serviceName = name;
    }

    /**
     * Start up the name service, including creating an ORB instance to expose
     * it under.
     * @exception TransientServiceException
     */
    public void run() throws TransientServiceException {
        // Create an ORB object
        java.util.Properties props = new Properties();
        props.putAll(System.getProperties());

        props.put("org.omg.CORBA.ORBServerId", "1000000");
        props.put("org.omg.CORBA.ORBClass", "org.apache.yoko.orb.CORBA.ORB");
        props.put("org.omg.CORBA.ORBSingletonClass", "org.apache.yoko.orb.CORBA.ORBSingleton");
        props.put("yoko.orb.oa.endpoint", "iiop --host " + host + " --port " + port);

        createdOrb = ORB.init((String[]) null, props);

        // now initialize the service
        initialize(createdOrb);
    }

    /**
     * Initialize a transient name service on a specific ORB.
     * @param orb The ORB hosting the service.
     * @exception TransientServiceException
     */
    public void initialize(ORB orb) throws TransientServiceException {
        try {
            // Fire up the RootPOA
            POA rootPOA = (POA) orb.resolve_initial_references("RootPOA");
            rootPOA.the_POAManager().activate();

            // we need to create a POA to manage this named instance, and then activate a context on it.
            Policy[] policy = new Policy[3];
            policy[0] = rootPOA.create_lifespan_policy(TRANSIENT);
            policy[1] = rootPOA.create_id_assignment_policy(SYSTEM_ID);
            policy[2] = rootPOA.create_servant_retention_policy(RETAIN);

            POA nameServicePOA = rootPOA.create_POA("TNameService", null, policy);
            nameServicePOA.the_POAManager().activate();

            // create our initial context, and register that with the ORB as the name service
            initialContext = new TransientNamingContext(orb, nameServicePOA);

            // Resolve the Boot Manager and register the context object so we can resolve it using a corbaloc:: URL
            BootManager bootManager = BootManagerHelper.narrow(orb
                    .resolve_initial_references("BootManager"));
            byte[] objectId = serviceName.getBytes();
            bootManager.add_binding(objectId, initialContext.getRootContext());
            // now register this as the naming service for the ORB as well.
            ((org.apache.yoko.orb.CORBA.ORB) orb).register_initial_reference("NameService",
                    initialContext.getRootContext());
        } catch (Exception e) {
            throw new TransientServiceException("Unable to initialize name service", e);
        }
    }

    /**
     * Destroy the created service.
     */
    public void destroy() {
        // only destroy this if we created the orb instance.
        if (createdOrb != null) {
            createdOrb.destroy();
            createdOrb = null;
        }
    }

    @Override
    public void close() throws Exception {
        destroy();
    }
}
