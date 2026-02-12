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
package org.apache.yoko.orb.OBPortableServer;

import org.apache.yoko.orb.CORBA.ServerRequest;
import org.apache.yoko.orb.CORBA.YokoInputStream;
import org.apache.yoko.orb.CORBA.YokoOutputStream;
import org.apache.yoko.orb.OB.LocationForward;
import org.apache.yoko.orb.OB.RuntimeLocationForward;
import org.apache.yoko.orb.OB.Upcall;
import org.apache.yoko.util.Assert;
import org.omg.CORBA.InterfaceDef;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.InvokeHandler;
import org.omg.CORBA.portable.OutputStream;
import org.omg.PortableServer.DynamicImplementation;
import org.omg.PortableServer.Servant;

//
// There are several ways a request can be dispatched in Java:
//
// 1) Portable stream-based skeleton
// 2) DSI
// 3) Proprietary skeleton
// 
// We cannot simply invoke _OB_dispatch() on the servant, because
// org.omg.PortableServer.Servant is standardized.
//
// To support portable skeletons, this class also implements the
// standard ResponseHandler interface.
//
final class ServantDispatcher implements org.omg.CORBA.portable.ResponseHandler {
    private final Upcall upcall;
    private final Servant servant;

    // Used to bypass a portable skeleton
    private static class Abort extends RuntimeException {}

    ServantDispatcher(Upcall upcall, Servant servant) {
        this.upcall = upcall;
        this.servant = servant;
    }

    private boolean dispatchBase() throws LocationForward {
        String opName = upcall.operation();

        switch (opName) {
        case "_interface":
        {
            upcall.preUnmarshal();
            upcall.postUnmarshal();
            InterfaceDef def = servant._get_interface();
            upcall.postinvoke();
            final YokoOutputStream out = upcall.preMarshal();
            try {
                out.write_Object(def);
            } catch (SystemException ex) {
                upcall.marshalEx(ex);
            }
            upcall.postMarshal();
            return true;
        }

        case "_is_a": // _is_a
        {
            final YokoInputStream in = upcall.preUnmarshal();
            String id = null;
            try {
                id = in.read_string();
            } catch (SystemException ex) {
                upcall.unmarshalEx(ex);
            }
            upcall.postUnmarshal();
            boolean b = servant._is_a(id);
            upcall.postinvoke();
            final YokoOutputStream out = upcall.preMarshal();
            try {
                out.write_boolean(b);
            } catch (SystemException ex) {
                upcall.marshalEx(ex);
            }
            upcall.postMarshal();
            return true;
        }

        case "_non_existent": // _non_existent
        {
            upcall.preUnmarshal();
            upcall.postUnmarshal();
            boolean b = servant._non_existent();
            upcall.postinvoke();
            final YokoOutputStream out  = upcall.preMarshal();
            try {
                out.write_boolean(b);
            } catch (SystemException ex) {
                upcall.marshalEx(ex);
            }
            upcall.postMarshal();
            return true;
        }
        default:
            return false;
        }
    }

    void dispatch() throws LocationForward {
        //
        // Handle common operations
        //
        if (dispatchBase()) return;

        //
        // Case 1: Servant is org.apache.yoko.orb.PortableServer.Servant, i.e.,
        // a proprietary skeleton with full interceptor support
        //
        if (servant instanceof org.apache.yoko.orb.PortableServer.Servant) {
            org.apache.yoko.orb.PortableServer.Servant s = (org.apache.yoko.orb.PortableServer.Servant) servant;
            s._OB_dispatch(upcall);
        }
        //
        // Case 2: Servant is a org.omg.CORBA.portable.InvokeHandler,
        // i.e., a portable stream-based skeleton. For a normal reply,
        // the skeleton will call back to createReply(). If a user
        // exception occurred, the skeleton will call back to
        // createExceptionReply(). SystemExceptions are raised
        // directly.
        //
        else if (servant instanceof InvokeHandler) {
            try {
                InvokeHandler inv = (InvokeHandler) servant;

                //
                // Prepare to unmarshal
                //
                InputStream in = upcall.preUnmarshal();

                //
                // Call postUnmarshal now. There may be interceptors that
                // need to be called before dispatching to the servant.
                // When using a portable skeleton, the interceptors cannot
                // obtain parameter information.
                //
                upcall.postUnmarshal();

                //
                // Invoke the portable skeleton
                //
                OutputStream out = inv._invoke(upcall.operation(), in, this);

                //
                // The OutputStream returned by _invoke() should be
                // the Upcall's OutputStream
                //
                Assert.ensure(out == upcall.output());

                //
                // Finish up
                //
                if (!upcall.userException()) upcall.postMarshal();
            } catch (Abort ex) {
                //
                // Abort is raised by createExceptionReply()
                //
            } catch (RuntimeLocationForward ex) {
                //
                // RuntimeLocationForward is raised by createReply() and
                // createExceptionReply() to bypass the portable
                // skeleton and report a location-forward
                //
                throw new LocationForward(ex.ior, ex.perm);
            }
        }
        //
        // Case 3: DSI
        //
        else if (servant instanceof DynamicImplementation) {
            DynamicImplementation impl = (DynamicImplementation) servant;
            ServerRequest request = new ServerRequest(impl, upcall);

            try {
                impl.invoke(request);
                request._OB_finishUnmarshal();
                request._OB_postinvoke();
                request._OB_doMarshal();
            } catch (SystemException ex) {
                request._OB_finishUnmarshal();
                throw ex;
            }
        } else
            throw Assert.fail();
    }

    // ----------------------------------------------------------------------
    // ResponseHandler standard method implementations
    // ----------------------------------------------------------------------

    //
    // Called by a portable skeleton for a normal reply
    //
    public OutputStream createReply() {
        try {
            upcall.postinvoke();
            return upcall.preMarshal();
        } catch (LocationForward ex) {
            //
            // We need to raise an exception in order to abort the
            // current execution context and return control to
            // DispatchStrategy_impl. We do this by raising a
            // RuntimeException containing the location forward
            // parameters.
            //
            // Note that the user can interfere with this process
            // if they trap RuntimeException.
            //
            throw new RuntimeLocationForward(ex.ior, ex.perm);
        }
    }

    //
    // Called by a portable skeleton for a user exception
    //
    public OutputStream createExceptionReply() {
        OutputStream out = upcall.beginUserException(null);

        //
        // If the return value of beginUserException is null, then
        // we cannot let the skeleton attempt to marshal. So we'll
        // raise the Abort exception to bypass the portable skeleton.
        //
        // Note that the user can interfere with this process
        // if they trap RuntimeException.
        //
        if (out == null) throw new Abort();
        return out;
    }
}
