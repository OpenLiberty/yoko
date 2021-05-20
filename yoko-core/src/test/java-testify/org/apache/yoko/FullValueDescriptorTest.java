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
package org.apache.yoko;

import acme.Loader;
import acme.Processor;
import acme.Widget;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.Servant;
import testify.bus.Bus;
import testify.jupiter.annotation.iiop.ConfigureServer;
import testify.jupiter.annotation.iiop.ConfigureServer.BeforeServer;
import testify.jupiter.annotation.logging.Logging;

import javax.rmi.CORBA.Tie;
import javax.rmi.CORBA.Util;
import javax.rmi.PortableRemoteObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;

import static testify.jupiter.annotation.logging.Logging.LoggingLevel.FINE;
import static testify.jupiter.annotation.logging.Logging.LoggingLevel.FINER;

@ConfigureServer
public class FullValueDescriptorTest {
    private static final Loader CLIENT_LOADER = Loader.V1;
    private static final Loader SERVER_LOADER = Loader.V2;

    private static Constructor<? extends Widget> payloadConstructor;
    private static Processor stub;

    @BeforeServer
    public static void initServer(ORB orb, Bus bus) throws Exception {
        bus.log("Loading impl class for server target object");
        Class<? extends Processor> targetImplClass = SERVER_LOADER.loadClass("versioned.ProcessorImpl");
        bus.log("Loaded impl class for server target object");
        Constructor<? extends Processor> targetConstructor = targetImplClass.getConstructor(Bus.class);
        bus.log("Got constructor for server target object");
        Processor target = targetConstructor.newInstance(bus);
        bus.log("Created server target object");
        POA rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
        rootPOA.the_POAManager().activate();

        PortableRemoteObject.exportObject(target);
        bus.log("Exported server target object");
        Tie tie = Util.getTie(target);
        System.out.println("About to activate object with root POA");
        rootPOA.activate_object((Servant)tie);
        bus.log("Activated object with root POA");
        String ior = orb.object_to_string(tie.thisObject());
        bus.log("Target object ior = " + ior);
        bus.put("ior", ior);
    }

    @BeforeAll
    public static void initClient(ORB orb, Bus bus) throws Exception {
        bus.log("Waiting for IOR");
        String ior = bus.get("ior");
        bus.log("Received ior = " + ior);
        Object obj = orb.string_to_object(ior);
        bus.log("Converted ior to generic stub");
        stub = (Processor)PortableRemoteObject.narrow(obj, Processor.class);
        bus.log("Narrowed stub");
        Class<? extends Widget> payloadClass = CLIENT_LOADER.loadClass("versioned.WidgetImpl");
        bus.log("loaded VersionedImpl class for client");
        payloadConstructor = payloadClass.getConstructor();
        bus.log("Got VersionedImpl constructor");
    }

    @Test
    @Logging(value = "yoko.verbose.class", level = FINER)
    @Logging(value = "yoko.verbose.data.in", level = FINE)
    @Logging("yoko.verbose.request.in")
    @Logging("yoko.verbose.marshal")
    public void testMarshallingAnAbstract() throws Exception {
        Widget payload = payloadConstructor.newInstance();
        Widget returned = stub.processAbstract(payload, Widget::validateAndReplace);
        returned.validateAndReplace();
    }

    @Test
    @Logging("yoko.verbose")
    public void testMarshallingAnAny() throws Exception {
        Widget payload = payloadConstructor.newInstance();
        Widget returned = stub.processAny(payload, Widget::validateAndReplace);
        returned.validateAndReplace();
    }

    @Test
    @Logging("yoko.verbose")
    public void testMarshallingAValue() throws Exception {
        Widget payload = payloadConstructor.newInstance();
        Widget returned = stub.processValue(payload, Widget::validateAndReplace);
        returned.validateAndReplace();
    }

    @Test
    public void testSerializingAValue() throws Exception {
        Widget w1 = payloadConstructor.newInstance();
        Widget w2 = SERVER_LOADER.deserializeFromBytes(serializeToBytes(w1));
        w2 = w2.validateAndReplace();
        w1 = CLIENT_LOADER.deserializeFromBytes(serializeToBytes(w2));
        w1 = w1.validateAndReplace();
    }

    private byte[] serializeToBytes(Widget payload) throws IOException {
        try (
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
        ) {
            oos.writeObject(payload);
            oos.flush();
            return baos.toByteArray();
        }
    }
}


