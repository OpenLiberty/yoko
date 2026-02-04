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
package org.apache.yoko.orb.CORBA;

import org.apache.yoko.orb.OB.CodecPair;
import org.apache.yoko.orb.OB.DowncallStub;
import org.apache.yoko.orb.OB.FailureException;
import org.apache.yoko.orb.OB.GIOPOutgoingMessage;
import org.apache.yoko.orb.OB.LocationForward;
import org.apache.yoko.orb.OB.ORBInstance;
import org.apache.yoko.orb.OCI.ProfileInfo;
import org.apache.yoko.orb.OCI.ProfileInfoHolder;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CORBA.portable.RemarshalException;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.ServiceContextListHolder;
import org.omg.MessageRouting.PersistentRequest;
import org.omg.Messaging.ReplyHandler;

//
// ObjectImpl is the base class for proprietary stubs with full
// interceptor support
//
abstract public class ObjectImpl extends org.omg.CORBA_2_4.portable.ObjectImpl {
    public DowncallStub _OB_getDowncallStub()
            throws LocationForward,
            FailureException {
        Delegate delegate = (Delegate) _get_delegate();
        return delegate._OB_getDowncallStub();
    }

    public void _OB_handleException(Exception ex, RetryInfo info) {
        Delegate delegate = (Delegate) _get_delegate();
        delegate._OB_handleException(ex, info, false);
    }

    public GIOPOutgoingMessage _OB_ami_router_preMarshal(
            String operation, boolean responseExpected, OutputStreamHolder out,
            ProfileInfoHolder info) {
        Delegate delegate = (Delegate) _get_delegate();

        try {
            DowncallStub downStub = delegate
                    ._OB_getDowncallStub();
            GIOPOutgoingMessage message = downStub
                    .AMIRouterPreMarshal(operation, responseExpected, out, info);

            return message;
        } catch (LocationForward ex) {
        } catch (FailureException ex) {
        }

        return null;
    }

    public void _OB_ami_router_postMarshal(
            GIOPOutgoingMessage message,
            OutputStreamHolder out) {
        Delegate delegate = (Delegate) _get_delegate();

        try {
            DowncallStub downStub = delegate
                    ._OB_getDowncallStub();
            downStub.AMIRouterPostMarshal(message, out);
        } catch (LocationForward ex) {
        } catch (FailureException ex) {
        }
    }

    // public org.apache.yoko.orb.CORBA.OutputStream
    public CodecPair _OB_setup_ami_poll_request(
            ServiceContextListHolder sclHolder,
            OutputStreamHolder out) {
        Delegate delegate = (Delegate) _get_delegate();

        try {
            DowncallStub downStub = delegate
                    ._OB_getDowncallStub();
            CodecPair cc = downStub
                    .setupPollingRequest(sclHolder, out);

            return cc;
        } catch (LocationForward ex) {
        } catch (FailureException ex) {
        }

        return null;
    }

    public org.omg.CORBA.Object _OB_get_ami_poll_target() {
        //
        // This is needed since we don't have access to the IOR information
        // from the DowncallStub like we do in C++ (MarshalStub)
        //
        Delegate delegate = (Delegate) _get_delegate();
        try {
            DowncallStub downStub = delegate
                    ._OB_getDowncallStub();
            return downStub.getAMIPollTarget();
        } catch (LocationForward ex) {
        } catch (FailureException ex) {
        }

        return null;
    }

    public ORBInstance _OB_get_ami_poll_ORBInstance() {
        //
        // We need to be able to retrieve the ORB instance to use with a
        // persistent poller in case we want to use pollable sets
        //
        Delegate delegate = (Delegate) _get_delegate();
        try {
            DowncallStub downStub = delegate
                    ._OB_getDowncallStub();
            return downStub._OB_getORBInstance();
        } catch (LocationForward ex) {
        } catch (FailureException ex) {
        }

        return null;
    }

    public PersistentRequest _OB_ami_poll_request(
            OutputStream out, String operation,
            ServiceContext[] scl)
            throws RemarshalException {
        Delegate delegate = (Delegate) _get_delegate();

        try {
            DowncallStub downStub = delegate
                    ._OB_getDowncallStub();
            PersistentRequest req = downStub
                    .ami_poll_request(out, operation, scl);
            return req;
        } catch (LocationForward ex) {
        } catch (FailureException ex) {
        }

        //
        // Something happened other than what we expected. Remarshal so
        // the stub can try again.
        //
        throw new RemarshalException();
    }

    public boolean _OB_ami_callback_request(
            OutputStream out,
            ReplyHandler reply,
            ProfileInfo info)
            throws RemarshalException {
        Delegate delegate = (Delegate) _get_delegate();

        boolean success = false;
        try {
            DowncallStub downStub = delegate
                    ._OB_getDowncallStub();
            success = downStub.ami_callback_request(out, reply, info);
        } catch (LocationForward ex) {
        } catch (FailureException ex) {
        }

        return success;
    }
}
