/*
 * Copyright 2025 IBM Corporation and others.
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

import org.apache.yoko.orb.OB.LOCATION_TRANSPARENCY_STRICT;
import org.apache.yoko.orb.OB.LocationForward;
import org.apache.yoko.orb.OB.ORBInstance;
import org.apache.yoko.orb.OB.ObjectIdHasher;
import org.apache.yoko.orb.OB.PIManager;
import org.apache.yoko.orb.OB.RefCountPolicyList;
import org.apache.yoko.orb.PortableServer.PoaCurrentImpl;
import org.apache.yoko.util.Assert;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.SystemException;
import org.omg.PortableServer.DynamicImplementation;
import org.omg.PortableServer.POAPackage.ObjectAlreadyActive;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import org.omg.PortableServer.Servant;
import org.omg.PortableServer.ServantLocatorPackage.CookieHolder;
import org.omg.PortableServer.ServantManagerOperations;

import java.util.Hashtable;
import java.util.Vector;

import static org.apache.yoko.orb.OB.Util.printOctets;
import static org.apache.yoko.orb.OBPortableServer.TableEntry.ACTIVATE_PENDING;
import static org.apache.yoko.orb.OBPortableServer.TableEntry.ACTIVE;
import static org.apache.yoko.orb.OBPortableServer.TableEntry.DEACTIVATED;
import static org.apache.yoko.orb.OBPortableServer.TableEntry.DEACTIVATE_PENDING;
import static org.apache.yoko.util.MinorCodes.MinorCannotDispatch;
import static org.apache.yoko.util.MinorCodes.describeObjectNotExist;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;
import static org.omg.PortableServer.IdUniquenessPolicyValue.UNIQUE_ID;

//
// Mapping for ObjectId to a sequence of DirectStubImpl
//
class DirectSeqEntry {
    private Vector seq_;

    private byte[] oid_; // TODO: tmp

    private void traceoid() {
        printOctets(System.out, oid_, 0,
                oid_.length);
    }

    DirectSeqEntry(byte[] oid) {
        seq_ = new Vector();
        oid_ = oid;
    }

    protected void finalize() throws Throwable {
        deactivate();
        super.finalize();
    }

    void deactivate() {
        // traceoid();
        // System.out.println("deactivate: ");
        for (int i = 0; i < seq_.size(); i++)
            ((DirectServant) seq_.elementAt(i)).deactivate();
        seq_.removeAllElements();
    }

    void add(DirectServant directServant) {
        // traceoid();
        // System.out.println("add: " + directServant);

        seq_.addElement(directServant);
    }

    boolean remove(DirectServant directServant) {
        // traceoid();
        // System.out.println("remove: " + directServant);

        for (int i = 0; i < seq_.size(); i++) {
            if (seq_.elementAt(i) == directServant) {
                seq_.removeElementAt(i);
                return seq_.isEmpty();
            }
        }
        return false;
    }
}

//
// If USE_ACTIVE_OBJECT_MAP_ONLY this strategy is used
//
class ActiveObjectOnlyStrategy implements ServantLocationStrategy {
    //
    // The AOM
    //
    protected Hashtable activeObjectTable_;

    //
    // Reverse map from servant to id
    //
    protected Hashtable servantIdTable_;

    //
    // Mapping for ObjectId's to DirectStubImpl
    //
    private Hashtable directSeqTable_;

    //
    // The ORBInstance
    //
    private ORBInstance orbInstance_;

    //
    // This method is synchronized on the TableEntry
    //
    protected void completeActivation(
            ObjectIdHasher oid,
            Servant servant, TableEntry entry) {
        //
        // If there is a DirectStubImpl that refers to a default servant
        // under this oid then deactivate each
        //
        synchronized (directSeqTable_) {
            DirectSeqEntry table;
            table = (DirectSeqEntry) directSeqTable_.get(oid);
            if (table != null) {
                table.deactivate();
                directSeqTable_.remove(oid);
            }
        }

        //
        // If using UNIQUE_ID add the servant to the servantIdTable
        //
        if (servantIdTable_ != null) {
            Assert.ensure(!servantIdTable_
                    .containsKey(servant));
            servantIdTable_.put(servant, oid.getObjectId());
        }

        //
        // Set the servant's delegate
        //
        ((org.omg.CORBA_2_3.ORB) orbInstance_.getORB()).set_delegate(servant);

        //
        // Update the object entry
        //
        entry.setServant(servant);
        entry.setActive();
    }

    protected void completeDeactivate(org.omg.PortableServer.POA poa,
            ObjectIdHasher oid, TableEntry entry) {
        //
        // Mark each DirectServant associated with this oid as
        // deactivated
        //
        synchronized (directSeqTable_) {
            DirectSeqEntry table = (DirectSeqEntry) directSeqTable_.get(oid);
            if (table != null) {
                table.deactivate();
                directSeqTable_.remove(oid);
            }
        }

        //
        // If we're using UNIQUE_ID then remove the servant from the
        // servantIdTable
        //
        if (servantIdTable_ != null) {
            Servant servant = entry.getServant();
            Assert.ensure(servantIdTable_
                    .containsKey(servant));
            servantIdTable_.remove(servant);
        }

        //
        // Release the servant reference before calling etherealize and
        // mark the entry as deactivated.
        //
        entry.setDeactivated();
        entry.clearServant();
    }

    protected DirectServant completeDirectStubImpl(
            org.omg.PortableServer.POA poa, byte[] rawoid,
            Servant servant,
            RefCountPolicyList policies) {
        DirectServant directServant;

        ObjectIdHasher oid = new ObjectIdHasher(
                rawoid);

        //
        // No direct invocations for DSI servants
        //
        if (servant instanceof DynamicImplementation)
            return null;

        //
        // We must have direct invocations if the servant has native
        // types. Always use direct invocations, if possible, if there are
        // no interceptors installed.
        //
        // TODO: Check the POA interceptor policy
        //
        // We need this hack in Java because the servant class is
        // standardized, so we can't invoke _OB_haveNativeTypes().
        //
        boolean haveNativeTypes = (servant instanceof ServantManagerOperations);
        PIManager piManager = orbInstance_
                .getPIManager();
        if (!haveNativeTypes
                && (policies.locationTransparency == LOCATION_TRANSPARENCY_STRICT.value
                        || piManager.haveClientInterceptors() || piManager
                        .haveServerInterceptors()))
            return null;

        //
        // Create a DirectServant
        //
        directServant = new DirectServant(
                (POA_impl) poa, oid
                        .getObjectId(), servant);

        //
        // Add the DirectServant to the table
        //
        synchronized (directSeqTable_) {
            DirectSeqEntry table = (DirectSeqEntry) directSeqTable_.get(oid);
            if (table == null) {
                table = new DirectSeqEntry(oid.getObjectId());
                directSeqTable_.put(oid, table);
            }
            table.add(directServant);
        }

        return directServant;
    }

    ActiveObjectOnlyStrategy(
            POAPolicies policies,
            ORBInstance orbInstance) {
        activeObjectTable_ = new Hashtable(1023);
        directSeqTable_ = new Hashtable(1023);
        orbInstance_ = orbInstance;

        if (policies.idUniquenessPolicy() == UNIQUE_ID)
            servantIdTable_ = new Hashtable(1023);
        else
            servantIdTable_ = null;
    }

    public void destroy(org.omg.PortableServer.POA poa, boolean etherealize) {
        synchronized (activeObjectTable_) {
            activeObjectTable_.clear();
        }

        synchronized (directSeqTable_) {
            directSeqTable_.clear();
        }

        if (servantIdTable_ != null)
            servantIdTable_.clear();
    }

    public void etherealize(org.omg.PortableServer.POA poa) {
        // Do nothing
    }

    public void activate(byte[] rawoid, Servant servant)
            throws ServantAlreadyActive,
            WrongPolicy,
            ObjectAlreadyActive {
        ObjectIdHasher oid = new ObjectIdHasher(
                rawoid);

        while (true) {
            boolean incarnate = false;
            TableEntry entry;

            //
            // Find out whether a servant is already bound under this id
            // if not add an entry into the AOM
            //
            synchronized (activeObjectTable_) {
                entry = (TableEntry) activeObjectTable_.get(oid);
                if (entry == null) {
                    //
                    // If using UNIQUE_ID, then verify that the
                    // servant isn't already activated.
                    //
                    if (servantIdTable_ != null
                            && servantIdTable_.containsKey(servant)) {
                        throw new ServantAlreadyActive();
                    }

                    //
                    // Insert the servant in the active object table
                    // with the provided id.
                    //
                    entry = new TableEntry();
                    activeObjectTable_.put(oid, entry);
                }
            }

            synchronized (entry) {
                switch (entry.state()) {
                case DEACTIVATE_PENDING:
                    entry.waitForStateChange();
                    continue;

                case ACTIVATE_PENDING:
                    incarnate = true;
                    break;

                case ACTIVE:
                    throw new ObjectAlreadyActive();

                case DEACTIVATED:
                    break;
                }

                if (incarnate) {
                    completeActivation(oid, servant, entry);
                    return;
                }
            }
        }
    }

    public void deactivate(org.omg.PortableServer.POA poa, byte[] rawoid)
            throws ObjectNotActive,
            WrongPolicy {
        ObjectIdHasher oid = new ObjectIdHasher(
                rawoid);

        //
        // If no object in the active object table associated with
        // this key then raise an ObjectNotActive exception.
        //
        TableEntry entry;
        synchronized (activeObjectTable_) {
            entry = (TableEntry) activeObjectTable_.get(oid);
            if (entry == null)
                throw new ObjectNotActive();
        }

        boolean deactivate = false;
        synchronized (entry) {
            switch (entry.state()) {
            case ACTIVE:
                entry.setDeactivatePending();
                deactivate = entry.getOutstandingRequests() == 0;
                break;

            case DEACTIVATE_PENDING:
                return;

            case ACTIVATE_PENDING:
            case DEACTIVATED:
                throw new ObjectNotActive();
            }

            if (deactivate) {
                completeDeactivate(poa, oid, entry);

                //
                // Remove the entry from the active object map
                //
                synchronized (activeObjectTable_) {
                    activeObjectTable_.remove(oid);
                }
            }
        }
    }

    public byte[] servantToId(Servant servant,
                              PoaCurrentImpl poaCurrent) {
        byte[] id = null;
        if (servantIdTable_ != null)
            id = (byte[]) servantIdTable_.get(servant);
        return id;
    }

    public Servant idToServant(byte[] rawoid,
            boolean useDefaultServant) {
        ObjectIdHasher oid = new ObjectIdHasher(
                rawoid);
        while (true) {
            TableEntry entry;
            synchronized (activeObjectTable_) {
                entry = (TableEntry) activeObjectTable_.get(oid);
                if (entry == null)
                    return null;
            }

            synchronized (entry) {
                switch (entry.state()) {
                case DEACTIVATE_PENDING:
                case ACTIVATE_PENDING:
                    entry.waitForStateChange();
                    continue;

                case ACTIVE:
                    return entry.getServant();

                case DEACTIVATED:
                    return null;
                }
            }
        }
    }

    public Servant locate(byte[] rawoid,
            org.omg.PortableServer.POA poa, String op,
            CookieHolder cookie)
            throws LocationForward {
        Servant servant = idToServant(rawoid, false);
        if (servant == null) {
            //
            // If the servant isn't in the table then this is an
            // OBJECT_NOT_EXIST exception
            //
            throw new OBJECT_NOT_EXIST(
                    describeObjectNotExist(MinorCannotDispatch),
                    MinorCannotDispatch,
                    COMPLETED_NO);
        }
        return servant;
    }

    public void preinvoke(byte[] rawoid) {
        ObjectIdHasher oid = new ObjectIdHasher(
                rawoid);

        TableEntry entry;
        synchronized (activeObjectTable_) {
            entry = (TableEntry) activeObjectTable_.get(oid);
            if (entry == null)
                return;
        }

        synchronized (entry) {
            entry.incOutstandingRequest();
        }
    }

    public void postinvoke(byte[] rawoid, org.omg.PortableServer.POA poa,
            String op, Object cookie,
            Servant servant) {
        ObjectIdHasher oid = new ObjectIdHasher(
                rawoid);

        TableEntry entry;
        synchronized (activeObjectTable_) {
            entry = (TableEntry) activeObjectTable_.get(oid);
            if (entry == null)
                return;
        }

        //
        // If the number of outstanding requests is now 0 and the
        // entry has been deactivated then complete the deactivation
        //
        boolean deactivate = false;
        synchronized (entry) {
            if (entry.decOutstandingRequest() == 0)
                deactivate = entry.state() == DEACTIVATE_PENDING;

            if (deactivate) {
                completeDeactivate(poa, oid, entry);

                //
                // Remove the entry for the active object map
                //
                synchronized (activeObjectTable_) {
                    activeObjectTable_.remove(oid);
                }
            }
        }
    }

    public DirectServant createDirectStubImpl(org.omg.PortableServer.POA poa,
            byte[] oid, RefCountPolicyList policies)
            throws LocationForward {
        try {
            CookieHolder cookie = null;
            Servant servant = locate(oid, poa, "",
                    cookie);
            return completeDirectStubImpl(poa, oid, servant, policies);
        } catch (SystemException ex) {
        }
        return null;
    }

    public void removeDirectStubImpl(byte[] rawoid, DirectServant directStubImpl) {
        ObjectIdHasher oid = new ObjectIdHasher(
                rawoid);
        synchronized (directSeqTable_) {
            DirectSeqEntry table = (DirectSeqEntry) directSeqTable_.get(oid);
            if (table != null) {
                if (table.remove(directStubImpl))
                    directSeqTable_.remove(oid);
            }
        }
    }

    public ServantManagerStrategy getServantManagerStrategy() {
        return null;
    }

    public DefaultServantHolder getDefaultServantHolder() {
        return null;
    }
}
