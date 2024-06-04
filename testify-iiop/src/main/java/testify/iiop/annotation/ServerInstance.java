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
package testify.iiop.annotation;

import static org.apache.yoko.io.AlignmentBoundary.FOUR_BYTE_BOUNDARY;
import static org.apache.yoko.io.AlignmentBoundary.TWO_BYTE_BOUNDARY;
import static org.apache.yoko.io.Buffer.createReadBuffer;
import static testify.bus.TestLogLevel.WARN;
import static testify.hex.HexParser.HEX_STRING;

import java.util.Map;
import java.util.Properties;
import java.util.stream.IntStream;

import org.apache.yoko.io.ReadBuffer;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.IdUniquenessPolicyValue;
import org.omg.PortableServer.ImplicitActivationPolicyValue;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAManager;
import org.omg.PortableServer.RequestProcessingPolicyValue;
import org.omg.PortableServer.ServantRetentionPolicyValue;
import org.omg.PortableServer.ThreadPolicyValue;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.AdapterAlreadyExists;
import org.omg.PortableServer.POAPackage.InvalidPolicy;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import org.opentest4j.AssertionFailedError;

import testify.bus.Bus;
import testify.iiop.annotation.ConfigureServer.ServerName;
import testify.util.Maps;
import testify.util.Throw;

class ServerInstance {
    final Bus bus;
    final ORB orb;
    final Map<Class<?>, Object> paramMap;
    final POA childPoa;
    final int port;
    final String host;

    
    /**
     * To avoid using the internals of the POA Manager implementation
     * just create an IOR and fish out the host and port from that.
     */
    private final static class AddressIntrospector {
    	final String host;
    	final int port;
    	
    	public AddressIntrospector(ORB orb, POA poa) {
    		final byte[] iiopProfile = getIiopProfile(getIor(orb, poa));
			ReadBuffer rb = createReadBuffer(iiopProfile);
			rb.skipBytes(2); // skip iiop_Version
			// read the host name
			rb.align(FOUR_BYTE_BOUNDARY);
			int len = rb.readInt(); // host string length
			StringBuilder sb = new StringBuilder(len);
			// read all but last byte, since strings are null-terminated in the IOR
			IntStream.range(0, len - 1).forEach(i -> sb.append(rb.readByteAsChar()));
    		this.host = sb.toString();
    		rb.skipBytes(1); // consume the null terminator
    		// read the port number
    		rb.align(TWO_BYTE_BOUNDARY);
    		this.port = rb.readShort() & 0xffff;
		}

    	static String getIor(ORB orb, POA poa) {
			try {
				org.omg.CORBA.Object o = poa.create_reference("IDL:Special.ID.Object:1.0");
				return orb.object_to_string(o);
			} catch (WrongPolicy e) {
				throw new AssertionFailedError("internal test framework error", e);
			}
        }

        static byte[] getIiopProfile(String ior) {
        	System.out.println("fake ior to discover server endpoint:" + ior);
        	String hex = ior.substring("IOR:".length()); // strip leading "IOR:"
        	byte[] binary = HEX_STRING.parse(hex);
			ReadBuffer rb = createReadBuffer(binary); // parse IOR
			try {
				//skip the BOM
				rb.skipBytes(1);
	        	//skip past the type_id
				rb.align(FOUR_BYTE_BOUNDARY);
	        	int len = rb.readInt(); 
	        	rb.skipBytes(len);
	        	rb.align(FOUR_BYTE_BOUNDARY);
	        	// read how many profiles there are (typically 1)
	        	rb.readInt();
	        	// leaf through the profiles looking for the one with an ID of zero (i.e. the TAG_INTERNET_IOP profile)
	        	for (;;) {
	        		int profileId = rb.readInt();
	        		int dataLen = rb.readInt();
	        		if (0 == profileId) return rb.readBytes(new byte[dataLen]);
	        		rb.skipBytes(dataLen);
	        		rb.align(FOUR_BYTE_BOUNDARY);
	        	}
			} finally {
				System.out.println(rb.dumpAllDataWithPosition());
			}
        }
    }
    
    ServerInstance(Bus bus, ServerName name, String[] args, Properties props) {
        this.bus = bus;
        this.orb = ORB.init(args, props);
        try {
            final POA rootPoa = getRootPoa(bus, orb, 7);
            final POAManager pm = rootPoa.the_POAManager();
            pm.activate();
            AddressIntrospector info = new AddressIntrospector(orb, rootPoa);
            this.host = info.host;
            this.port = info.port;
            bus.log(() -> String.format("Server listening on host %s and port %d%n", host, port));
            // create the POA policies for the server
            Policy[] policies = {
                    rootPoa.create_thread_policy(ThreadPolicyValue.ORB_CTRL_MODEL),
                    rootPoa.create_lifespan_policy(LifespanPolicyValue.PERSISTENT),
                    rootPoa.create_id_assignment_policy(IdAssignmentPolicyValue.USER_ID),
                    rootPoa.create_id_uniqueness_policy(IdUniquenessPolicyValue.UNIQUE_ID),
                    rootPoa.create_servant_retention_policy(ServantRetentionPolicyValue.RETAIN),
                    rootPoa.create_request_processing_policy(RequestProcessingPolicyValue.USE_ACTIVE_OBJECT_MAP_ONLY),
                    rootPoa.create_implicit_activation_policy(ImplicitActivationPolicyValue.NO_IMPLICIT_ACTIVATION),
            };
            childPoa = rootPoa.create_POA(name.toString(), pm, policies);
            this.paramMap = Maps.of(ORB.class, orb, Bus.class, bus, POA.class, childPoa);
        } catch (InterruptedException | InvalidName | AdapterInactive | AdapterAlreadyExists | InvalidPolicy e) {
            throw Throw.andThrowAgain(e);
        }
    }

    static POA getRootPoa(Bus bus, ORB orb, int retries) throws InvalidName, InterruptedException {
        // This is where yoko tries to start the server socket — allow some retries
        try {
            POA rootPoa = (POA) orb.resolve_initial_references("RootPOA");
            return rootPoa;
        } catch (COMM_FAILURE e) {
            if (retries < 1) throw e;
            bus.log(WARN, "Server start failed. Retrying..");
            // sleep to allow the port to become available
            Thread.sleep(200);
            return getRootPoa(bus, orb, retries - 1);
        }
    }

    void stop() {
        try {
            bus.log("Calling orb.shutdown(true)");
            orb.shutdown(true);
            bus.log("ORB shutdown complete, calling orb.destroy()");
            orb.destroy();
            bus.log("orb.destroy() returned");
        } catch (BAD_INV_ORDER e) {
            // The ORB is sometimes already shut down.
            // This should not cause an error in the test.
            // TODO: find out how this happens
            if (e.minor != 4) throw e;
        }
    }
}
