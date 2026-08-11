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
package org.apache.yoko.orb.OBPortableInterceptor;

import org.apache.yoko.orb.OB.InitialServiceManager;
import org.apache.yoko.orb.OB.PIManager;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.ORB;
import org.omg.IOP.CodecFactory;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.IORInterceptor;
import org.omg.PortableInterceptor.ORBInitInfoPackage.DuplicateName;
import org.omg.PortableInterceptor.ORBInitInfoPackage.InvalidName;
import org.omg.PortableInterceptor.PolicyFactory;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import java.util.Arrays;

//
// An ORBacus-specific derivation of PortableInterceptor::ORBInitInfo
//
final public class ORBInitInfo_impl extends LocalObject implements
        ORBInitInfo {
    //
    // The ORB
    //
    private ORB orb_;

    //
    // Arguments
    //
    private String[] args_;

    //
    // The ORB id
    //
    private String id_;

    //
    // The PIManager
    //
    private PIManager manager_;

    //
    // The initial service mgr
    //
    private InitialServiceManager initServiceManager_;

    //
    // The Codec Factory
    //
    private CodecFactory codecFactory_; // The codec factory

    private boolean destroy_;

    // ------------------------------------------------------------------
    // Public member implementations
    // ------------------------------------------------------------------

    public ORBInitInfo_impl(ORB orb, String[] args, String id,
                            PIManager manager,
                            InitialServiceManager initServiceManager,
                            CodecFactory codecFactory) {
        orb_ = orb;
        args_ = args;
        id_ = id;
        manager_ = manager;
        initServiceManager_ = initServiceManager;
        codecFactory_ = codecFactory;
        destroy_ = false;
    }

    // ------------------------------------------------------------------
    // Standard IDL to Java mapping
    // ------------------------------------------------------------------

    public String[] arguments() {
        if (destroy_)
            throw new OBJECT_NOT_EXIST(
                    "Object has been destroyed");

        String[] seq = new String[args_.length];
        System.arraycopy(args_, 0, seq, 0, args_.length);
        return seq;
    }

    public String orb_id() {
        if (destroy_)
            throw new OBJECT_NOT_EXIST(
                    "Object has been destroyed");

        return id_;
    }

    public CodecFactory codec_factory() {
        if (destroy_)
            throw new OBJECT_NOT_EXIST(
                    "Object has been destroyed");

        return codecFactory_;
    }

    public void register_initial_reference(String name, org.omg.CORBA.Object obj)
            throws InvalidName {
        if (destroy_)
            throw new OBJECT_NOT_EXIST(
                    "Object has been destroyed");

        try {
            initServiceManager_.addInitialReference(name, obj);
        } catch (org.omg.CORBA.ORBPackage.InvalidName ex) {
            throw (InvalidName)new
                InvalidName().initCause(ex);
        }
    }

    public org.omg.CORBA.Object resolve_initial_references(String name)
            throws InvalidName {
        if (destroy_)
            throw new OBJECT_NOT_EXIST(
                    "Object has been destroyed");

        // TODO: check state
        try {
            // we delegate this to the ORB rather than call the initial service manager directly. 
            // because the ORB has a special test to initialize the RootPOA service if it 
            // doesn't exist. 
            return orb_.resolve_initial_references(name);
        } catch (org.omg.CORBA.ORBPackage.InvalidName ex) {
            throw (InvalidName)new
                InvalidName().initCause(ex);
        }
    }

    public void add_client_request_interceptor(
            ClientRequestInterceptor i)
            throws DuplicateName {
        if (destroy_)
            throw new OBJECT_NOT_EXIST(
                    "Object has been destroyed");

        manager_.addClientRequestInterceptor(i);
    }

    public void add_server_request_interceptor(
            ServerRequestInterceptor i)
            throws DuplicateName {
        if (destroy_)
            throw new OBJECT_NOT_EXIST(
                    "Object has been destroyed");

        manager_.addServerRequestInterceptor(i);
    }

    public void add_ior_interceptor(IORInterceptor i)
            throws DuplicateName {
        if (destroy_)
            throw new OBJECT_NOT_EXIST(
                    "Object has been destroyed");

        manager_.addIORInterceptor(i, false);
    }

    public int allocate_slot_id() {
        if (destroy_)
            throw new OBJECT_NOT_EXIST(
                    "Object has been destroyed");

        return manager_.allocateSlotId();
    }

    public void register_policy_factory(int type,
            PolicyFactory factory) {
        if (destroy_)
            throw new OBJECT_NOT_EXIST(
                    "Object has been destroyed");

        manager_.registerPolicyFactory(type, factory);
    }

    public ORB orb() {
        if (destroy_)
            throw new OBJECT_NOT_EXIST(
                    "Object has been destroyed");

        return orb_;
    }

    public void _OB_destroy() {
        destroy_ = true;
        orb_ = null;
        manager_ = null;
        initServiceManager_ = null;
        codecFactory_ = null;
    }

    @Override
    public String toString() {
        return "ORBInitInfo_impl{" +
                "orb_=" + orb_ +
                ", args_=" + Arrays.toString(args_) +
                ", id_='" + id_ + '\'' +
                ", manager_=" + manager_ +
                ", initServiceManager_=" + initServiceManager_ +
                ", codecFactory_=" + codecFactory_ +
                ", destroy_=" + destroy_ +
                '}';
    }
}
