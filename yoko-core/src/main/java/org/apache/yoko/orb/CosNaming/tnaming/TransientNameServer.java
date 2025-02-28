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

import static org.apache.yoko.orb.CosNaming.tnaming.TransientNameService.DEFAULT_SERVICE_HOST;
import static org.apache.yoko.orb.CosNaming.tnaming.TransientNameService.DEFAULT_SERVICE_NAME;
import static org.apache.yoko.orb.CosNaming.tnaming.TransientNameService.DEFAULT_SERVICE_PORT;

/**
 * A stand-alone naming service launchable from a command line.
 */
public class TransientNameServer {
    /**
     * Launch a name service as a stand alone process.  The
     * Host, port, and service name are controlled using
     * program arguments.
     *
     * @param args   The array of arguments for tailoring the service.
     *
     * @exception Exception
     */
    public static void main(String args[])throws Exception {
        int port = DEFAULT_SERVICE_PORT;
        String host = DEFAULT_SERVICE_HOST;
        String serviceName = DEFAULT_SERVICE_NAME;

        // see if we have
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-ORBInitialPort")) {
                i++;
                if (i < args.length) {
                    port = Integer.parseInt(args[i]);
                }
                else {
                    throw new IllegalArgumentException("Invalid -ORBInitialPort option");
                }
            }
            else if (args[i].equals("-ORBInitialHost")) {
                i++;
                if (i < args.length) {
                    host = args[i];
                }
                else {
                    throw new IllegalArgumentException("Invalid -ORBInitialHost option");
                }
            }
            else if (args[i].equals("-ORBServiceName")) {
                i++;
                if (i < args.length) {
                    serviceName = args[i];
                }
                else {
                    throw new IllegalArgumentException("Invalid -ORBServiceName option");
                }
            }

        }
        // create a services, and just spin it off.  We wait forever after that.
        TransientNameService service = new TransientNameService(host, port, serviceName);
        service.run();

        // now we just sit and wait here.
        synchronized (service) {
            service.wait();
        }
    }
}
