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
package org.apache.yoko.orb.PortableInterceptor;

import org.apache.yoko.logging.VerboseLogging;
import org.apache.yoko.orb.IMR.ActiveState;
import org.apache.yoko.orb.IMR.POAStatus;
import org.apache.yoko.orb.IMR._NoSuchPOA;
import org.apache.yoko.orb.OBPortableInterceptor.PersistentORT_impl;
import org.omg.CORBA.INITIALIZE;
import org.omg.CORBA.INV_POLICY;
import org.omg.CORBA.Policy;
import org.omg.CORBA.SystemException;
import org.omg.PortableInterceptor.ACTIVE;
import org.omg.PortableInterceptor.DISCARDING;
import org.omg.PortableInterceptor.HOLDING;
import org.omg.PortableInterceptor.INACTIVE;
import org.omg.PortableInterceptor.IORInterceptor_3_0;
import org.omg.PortableInterceptor.ObjectReferenceTemplate;
import org.omg.PortableServer.LIFESPAN_POLICY_ID;
import org.omg.PortableServer.LifespanPolicy;
import org.omg.PortableServer.LifespanPolicyHelper;
import org.omg.PortableServer.LifespanPolicyValue;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;
import static org.apache.yoko.logging.VerboseLogging.IOR_LOG;

final public class IMRIORInterceptor_impl extends org.omg.CORBA.LocalObject implements IORInterceptor_3_0 {
    private final Hashtable poas_ = new Hashtable();

    private final ActiveState as_;

    private final String serverInstance_;

    private boolean running_;

    // ------------------------------------------------------------------
    // Private Member Functions
    // ------------------------------------------------------------------

    private POAStatus convertState(short state) {
        switch (state) {
        case INACTIVE.value:   return POAStatus.INACTIVE;
        case ACTIVE.value:     return POAStatus.ACTIVE;
        case HOLDING.value:    return POAStatus.HOLDING;
        case DISCARDING.value: return POAStatus.DISCARDING;
        }
        return POAStatus.NON_EXISTENT;
    }

    // ------------------------------------------------------------------
    // Public Member Functions
    // ------------------------------------------------------------------

    public IMRIORInterceptor_impl(ActiveState as, String serverInstance) {
        as_ = as;
        serverInstance_ = serverInstance;
        running_ = false;
    }

    // ------------------------------------------------------------------
    // IDL to Java Mapping
    // ------------------------------------------------------------------

    public String name() {
        return new String("IMRInterceptor");
    }

    public void destroy() {}

    public void establish_components(org.omg.PortableInterceptor.IORInfo info) {}

    public void components_established(org.omg.PortableInterceptor.IORInfo info) {
        // This method does nothing if this is not a persistent POA
        try {
            Policy p = info.get_effective_policy(LIFESPAN_POLICY_ID.value);
            LifespanPolicy policy = LifespanPolicyHelper.narrow(p);
            if (policy.value() != LifespanPolicyValue.PERSISTENT) return;
        } catch (INV_POLICY e) {
            // Default Lifespan policy is TRANSIENT
            return;
        }

        // Get the primary object-reference template
        ObjectReferenceTemplate primary = info.adapter_template();

        try {
            short state = info.state();
            POAStatus status = convertState(state);
            ObjectReferenceTemplate secondary = as_.poa_create(status, primary);
            info.current_factory(secondary);
        } catch (_NoSuchPOA e) {
            IOR_LOG.log(SEVERE, e, () -> Stream.of(e.poa).collect(joining("/", "IMR: POA not registered: ", "")));
            throw (INITIALIZE) new INITIALIZE().initCause(e);
        } catch (SystemException e) {
            IOR_LOG.log(SEVERE, e, () -> "IMR: Cannot contact: " + e.getMessage());
            throw (INITIALIZE) new INITIALIZE().initCause(e);
        }

        //
        // Update the poa hash table
        //
        String id = info.manager_id();
        String[] name = primary.adapter_name();

        Vector poas = (Vector) poas_.get(Integer.valueOf(id));
        if (poas != null) {
            //
            // Add poa to exiting entry
            //
            poas.addElement(name);

            // XXX Do I have to reput
        } else {
            //
            // Add a new entry for this adapter manager
            //
            poas = new Vector();
            poas.addElement(name);
            poas_.put(Integer.valueOf(id), poas);
        }
    }

    public void adapter_state_changed(
            ObjectReferenceTemplate[] templates,
            short state) {
        //
        // Only update the IMR from this point if the POAs have
        // been destroyed.
        //
        if (state != org.omg.PortableInterceptor.NON_EXISTENT.value)
            return;

        Vector poanames = new Vector();
        for (int i = 0; i < templates.length; ++i) {
            try {
                PersistentORT_impl persistentORT = (PersistentORT_impl) templates[i];
            } catch (ClassCastException ex) {
                //
                // If not a Persistent ORT continue
                //
                continue;
            }

            String[] adpaterName = templates[i].adapter_name();

            //
            // Add the POA to the list of POAs to send to the
            // IMR for the status update
            //
            poanames.addElement(adpaterName);

            //
            // Find the POA in the POAManager -> POAs map and
            // remove it.
            //
            Enumeration e = poas_.elements();
            while (e.hasMoreElements()) {
                Vector poas = (Vector) e.nextElement();

                //
                // Find the poa being deleted
                //
                int j;
                for (j = 0; j < poas.size(); ++j) {
                    String[] current = (String[]) poas.elementAt(j);
                    if (current.length != adpaterName.length)
                        continue;

                    boolean found = true;
                    for (int k = 0; k < adpaterName.length; ++k) {
                        if (!current[k].equals(adpaterName[k])) {
                            found = false;
                            break;
                        }
                    }
                    if (found)
                        break;
                }

                //
                // Shift back the remaining poas if match found
                //
                if (j != poas.size()) {
                    poas.removeElementAt(j);
                    break;
                }
            }
        }

        if (!poanames.isEmpty()) {
            try {
                String[][] poaArray = new String[poanames.size()][];
                poanames.copyInto(poaArray);
                as_.poa_status_update(poaArray,
                        POAStatus.NON_EXISTENT);
            } catch (SystemException ex) {
                String msg = "IMR: poa_destroy: " + ex.getMessage();
                IOR_LOG.log(WARNING, ex, () -> msg);
            }
        }
    }

    //
    // Update POA states for this adapter
    //
    public void adapter_manager_state_changed(String id, short state) {
        if (!running_) {
            //
            // Inform the IMR the server is now running if this
            // is the first call.
            //
            try {
                as_.set_status(serverInstance_,
                        org.apache.yoko.orb.IMR.ServerStatus.RUNNING);
            } catch (org.omg.CORBA.OBJECT_NOT_EXIST ex) {
                IOR_LOG.log(SEVERE, ex, () -> "IMR: Not registered");
                throw new INITIALIZE();
            } catch (SystemException ex) {
                IOR_LOG.log(SEVERE, ex, () -> "IMR: Cannot contact");
                throw new INITIALIZE();
            }

            running_ = true;
        }

        //
        // Inform the IMR of the POA status update
        //
        Vector poas = (Vector) poas_.get(Integer.valueOf(id));
        if (poas != null && poas.size() != 0) {
            try {
                String[][] poaArray = new String[poas.size()][];
                poas.copyInto(poaArray);
                POAStatus status = convertState(state);
                as_.poa_status_update(poaArray, status);
            } catch (SystemException ex) {
                //
                // XXX ????
                //
            }
        }
    }
}
