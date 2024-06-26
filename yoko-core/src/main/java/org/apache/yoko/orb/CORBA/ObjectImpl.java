/*
 * Copyright 2020 IBM Corporation and others.
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

//
// ObjectImpl is the base class for proprietary stubs with full
// interceptor support
//
abstract public class ObjectImpl extends org.omg.CORBA_2_4.portable.ObjectImpl {
    public org.apache.yoko.orb.OB.DowncallStub _OB_getDowncallStub()
            throws org.apache.yoko.orb.OB.LocationForward,
            org.apache.yoko.orb.OB.FailureException {
        Delegate delegate = (Delegate) _get_delegate();
        return delegate._OB_getDowncallStub();
    }

    public void _OB_handleException(Exception ex, RetryInfo info) {
        Delegate delegate = (Delegate) _get_delegate();
        delegate._OB_handleException(ex, info, false);
    }

    public org.apache.yoko.orb.OB.GIOPOutgoingMessage _OB_ami_router_preMarshal(
            String operation, boolean responseExpected, OutputStreamHolder out,
            org.apache.yoko.orb.OCI.ProfileInfoHolder info) {
        Delegate delegate = (Delegate) _get_delegate();

        try {
            org.apache.yoko.orb.OB.DowncallStub downStub = delegate
                    ._OB_getDowncallStub();
            org.apache.yoko.orb.OB.GIOPOutgoingMessage message = downStub
                    .AMIRouterPreMarshal(operation, responseExpected, out, info);

            return message;
        } catch (org.apache.yoko.orb.OB.LocationForward ex) {
        } catch (org.apache.yoko.orb.OB.FailureException ex) {
        }

        return null;
    }

    public void _OB_ami_router_postMarshal(
            org.apache.yoko.orb.OB.GIOPOutgoingMessage message,
            OutputStreamHolder out) {
        Delegate delegate = (Delegate) _get_delegate();

        try {
            org.apache.yoko.orb.OB.DowncallStub downStub = delegate
                    ._OB_getDowncallStub();
            downStub.AMIRouterPostMarshal(message, out);
        } catch (org.apache.yoko.orb.OB.LocationForward ex) {
        } catch (org.apache.yoko.orb.OB.FailureException ex) {
        }
    }

    // public org.apache.yoko.orb.CORBA.OutputStream
    public org.apache.yoko.orb.OB.CodeConverters _OB_setup_ami_poll_request(
            org.omg.IOP.ServiceContextListHolder sclHolder,
            org.apache.yoko.orb.CORBA.OutputStreamHolder out) {
        Delegate delegate = (Delegate) _get_delegate();

        try {
            org.apache.yoko.orb.OB.DowncallStub downStub = delegate
                    ._OB_getDowncallStub();
            org.apache.yoko.orb.OB.CodeConverters cc = downStub
                    .setupPollingRequest(sclHolder, out);

            return cc;
        } catch (org.apache.yoko.orb.OB.LocationForward ex) {
        } catch (org.apache.yoko.orb.OB.FailureException ex) {
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
            org.apache.yoko.orb.OB.DowncallStub downStub = delegate
                    ._OB_getDowncallStub();
            return downStub.getAMIPollTarget();
        } catch (org.apache.yoko.orb.OB.LocationForward ex) {
        } catch (org.apache.yoko.orb.OB.FailureException ex) {
        }

        return null;
    }

    public org.apache.yoko.orb.OB.ORBInstance _OB_get_ami_poll_ORBInstance() {
        //
        // We need to be able to retrieve the ORB instance to use with a
        // persistent poller in case we want to use pollable sets
        //
        Delegate delegate = (Delegate) _get_delegate();
        try {
            org.apache.yoko.orb.OB.DowncallStub downStub = delegate
                    ._OB_getDowncallStub();
            return downStub._OB_getORBInstance();
        } catch (org.apache.yoko.orb.OB.LocationForward ex) {
        } catch (org.apache.yoko.orb.OB.FailureException ex) {
        }

        return null;
    }

    public org.omg.MessageRouting.PersistentRequest _OB_ami_poll_request(
            org.omg.CORBA.portable.OutputStream out, String operation,
            org.omg.IOP.ServiceContext[] scl)
            throws org.omg.CORBA.portable.RemarshalException {
        Delegate delegate = (Delegate) _get_delegate();

        try {
            org.apache.yoko.orb.OB.DowncallStub downStub = delegate
                    ._OB_getDowncallStub();
            org.omg.MessageRouting.PersistentRequest req = downStub
                    .ami_poll_request(out, operation, scl);
            return req;
        } catch (org.apache.yoko.orb.OB.LocationForward ex) {
        } catch (org.apache.yoko.orb.OB.FailureException ex) {
        }

        //
        // Something happened other than what we expected. Remarshal so
        // the stub can try again.
        //
        throw new org.omg.CORBA.portable.RemarshalException();
    }

    public boolean _OB_ami_callback_request(
            org.omg.CORBA.portable.OutputStream out,
            org.omg.Messaging.ReplyHandler reply,
            org.apache.yoko.orb.OCI.ProfileInfo info)
            throws org.omg.CORBA.portable.RemarshalException {
        Delegate delegate = (Delegate) _get_delegate();

        boolean success = false;
        try {
            org.apache.yoko.orb.OB.DowncallStub downStub = delegate
                    ._OB_getDowncallStub();
            success = downStub.ami_callback_request(out, reply, info);
        } catch (org.apache.yoko.orb.OB.LocationForward ex) {
        } catch (org.apache.yoko.orb.OB.FailureException ex) {
        }

        return success;
    }
}
