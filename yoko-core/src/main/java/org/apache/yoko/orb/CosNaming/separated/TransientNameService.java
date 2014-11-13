/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
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


/**
 * @version $Rev: 555715 $ $Date: 2007-07-12 11:36:16 -0700 (Thu, 12 Jul 2007) $
 */
package org.apache.yoko.orb.CosNaming.separated;

import java.util.Properties;

import org.omg.CORBA.INITIALIZE;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitializer;
import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.ServantRetentionPolicyValue;

/**
 * A transient name service attached to an ORB.  This
 * class manages all of the housekeeping for creating a
 * TransientNamingContext and a exposing it using an
 * ORB.
 */
public class TransientNameService {
	// the default registered name service
	static public final String DEFAULT_SERVICE_NAME = "TNameService";
	// the default listening port
	static public final int DEFAULT_SERVICE_PORT = 900;
	// the default host name
	static public final String DEFAULT_SERVICE_HOST = "localhost";
	// initial listening port
	protected int port;
	// initial listening host
	protected String host;
	// the service name (used for registing for the corbaloc:: URL name
	protected String serviceName;
	// the orb instance we're running on
	protected ORB createdOrb;


	/**
	 * Create a new TransientNameService, using all default
	 * attributes.
	 */
	public TransientNameService() {
		this(DEFAULT_SERVICE_HOST, DEFAULT_SERVICE_PORT, DEFAULT_SERVICE_NAME);
	}

	/**
	 * Create a default-named name service using the specified
	 * host and port parameters.
	 *
	 * @param host   The host to expose this under.
	 * @param port   The initial listening port.
	 */
	public TransientNameService(String host, int port) {
		this(host, port, DEFAULT_SERVICE_NAME);
	}


	/**
	 * Create a specifically-named name service using the specified
	 * host and port parameters.
	 *
	 * @param host   The host to expose this under.
	 * @param port   The initial listening port.
	 * @param name   The name to register this service under using the
	 *               BootManager.
	 */
	public TransientNameService(String host, int port, String name) {
		this.port = port;
		this.host = host;
		this.serviceName = name;
	}

	/**
	 * Start up the name service, including creating an
	 * ORB instance to expose it under.
	 *
	 * @exception TransientServiceException
	 */
	public void run() throws TransientServiceException {
		// Create an ORB object
		java.util.Properties props = new Properties();
		props.putAll(System.getProperties());

		props.put("org.omg.CORBA.ORBServerId", "1000000" ) ;
		props.put("org.omg.CORBA.ORBClass", "org.apache.yoko.orb.CORBA.ORB");
		props.put("org.omg.CORBA.ORBSingletonClass", "org.apache.yoko.orb.CORBA.ORBSingleton");
		props.put("org.omg.PortableInterceptor.ORBInitializerClass." + Initializer.class.getName(), "");
		props.put("yoko.orb.oa.endpoint", "iiop --host " + host + " --port " + port);
		//      props.put("yoko.orb.poamanager.TNameService.endpoint", "iiop --host " + host);

		createdOrb = ORB.init(new String[] { "ORBNameService=" + serviceName }, props) ;

		// service initialized by orb initializer
	}

	public final static class Initializer extends LocalObject implements ORBInitializer {
		private static final long serialVersionUID = 1L;

		@Override
		public void pre_init(ORBInitInfo info) {
			try {
				final NamingContextImpl local = new NamingContextImpl();
				info.register_initial_reference("NameService", local);
			} catch (Exception e) {
				throw (INITIALIZE)(new INITIALIZE().initCause(e));
			}
		}

		@Override
		public void post_init(ORBInitInfo info) {
			try {
				final POA rootPOA = (POA) info.resolve_initial_references("RootPOA");
				rootPOA.the_POAManager().activate();

				final Policy[] policies = new Policy[3];
				policies[0] = rootPOA.create_lifespan_policy(LifespanPolicyValue.TRANSIENT);
				policies[1] = rootPOA.create_id_assignment_policy(IdAssignmentPolicyValue.SYSTEM_ID);
				policies[2] = rootPOA.create_servant_retention_policy(ServantRetentionPolicyValue.RETAIN);

				final POA nameServicePOA = rootPOA.create_POA("TNameService", null, policies);
				nameServicePOA.the_POAManager().activate();

				final NamingContextImpl local = 
						(NamingContextImpl) info.resolve_initial_references("NameService");

				final String serviceName = getServiceName(info);

				final org.apache.yoko.orb.OB.BootManager bootManager = 
						org.apache.yoko.orb.OB.BootManagerHelper.narrow(info.resolve_initial_references("BootManager"));
				byte[] objectId = serviceName.getBytes();
				bootManager.add_binding(objectId, local);

				local.getServant(nameServicePOA, false);
			} catch (Exception e) {
				throw (INITIALIZE)(new INITIALIZE().initCause(e));
			}
		}

		private String getServiceName(ORBInitInfo info) {
			for (String arg: info.arguments()) {
				if (arg.startsWith("ORBServiceName=")) {
					return arg.substring("ORBServiceName=".length());
				}
			}
			return "TNameService";
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
}

