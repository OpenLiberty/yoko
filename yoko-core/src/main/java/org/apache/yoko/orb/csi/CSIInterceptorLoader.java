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
package org.apache.yoko.orb.csi;

import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;

import org.omg.CORBA.INITIALIZE;
import org.omg.CORBA.LocalObject;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactoryPackage.UnknownEncoding;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitializer;
import org.omg.PortableInterceptor.PolicyFactory;
import org.omg.PortableInterceptor.ORBInitInfoPackage.DuplicateName;
import org.omg.Security.*;

import org.apache.yoko.orb.csi.gssup.SecGSSUPPolicy;


/**
 * @author Jeppe Sommer (jso@eos.dk)
 */
public class CSIInterceptorLoader extends LocalObject implements
                                                                    ORBInitializer
{

    static Logger log = Logger.getLogger(CSIInterceptorLoader.class.getName());

    CSIClientRequestInterceptor client_interceptor;
    CSIServerRequestInterceptor server_interceptor;
    GSSUPIORInterceptor ior_interceptor;

    public void pre_init(ORBInitInfo info) {
        log.fine("********  Running PortableCSILoader ******** ");

        Codec codec = null;
        try {
            codec = info.codec_factory()
                    .create_codec(
                            new Encoding(ENCODING_CDR_ENCAPS.value, (byte) 1,
                                         (byte) 2));
        }
        catch (UnknownEncoding ex) {
            log.log(SEVERE, "Could not get codec: ", ex);
            return;
        }

        client_interceptor = new CSIClientRequestInterceptor(codec);
        server_interceptor = new CSIServerRequestInterceptor(codec);
        ior_interceptor = new GSSUPIORInterceptor(codec);

        // Install factory for security policies...
        PolicyFactory factory = new CSIPolicyFactory();
        info.register_policy_factory(SecMechanismsPolicy.value, factory);
        info.register_policy_factory(SecInvocationCredentialsPolicy.value,
                                     factory);
        info.register_policy_factory(SecQOPPolicy.value, factory);
        info.register_policy_factory(SecEstablishTrustPolicy.value, factory);
        info.register_policy_factory(SecGSSUPPolicy.value, factory);
        info.register_policy_factory(SecDelegationDirectivePolicy.value,
                                     factory);

        try {
            info.add_client_request_interceptor(client_interceptor);
            info.add_server_request_interceptor(server_interceptor);
            info.add_ior_interceptor(ior_interceptor);

        }
        catch (DuplicateName ex) {
            throw (INITIALIZE)new INITIALIZE(ex.getMessage()).initCause(ex);
        }

    }

    public void post_init(ORBInitInfo info) {
    }

}
