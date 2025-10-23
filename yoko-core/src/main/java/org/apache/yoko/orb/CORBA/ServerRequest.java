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
package org.apache.yoko.orb.CORBA;

import static org.apache.yoko.orb.OB.Util.isSystemException;
import static org.apache.yoko.orb.OB.Util.readSysEx;
import static org.apache.yoko.util.MinorCodes.MinorInvalidUseOfDSIArguments;
import static org.apache.yoko.util.MinorCodes.MinorInvalidUseOfDSIContext;
import static org.apache.yoko.util.MinorCodes.MinorInvalidUseOfDSIResult;
import static org.apache.yoko.util.MinorCodes.MinorNoExceptionInAny;
import static org.apache.yoko.util.MinorCodes.describeBadInvOrder;
import static org.apache.yoko.util.MinorCodes.describeBadParam;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;
import static org.omg.CORBA.TCKind.tk_except;

import org.apache.yoko.orb.OB.LocationForward;
import org.apache.yoko.orb.OB.PIUpcall;
import org.apache.yoko.orb.OB.RuntimeLocationForward;
import org.apache.yoko.orb.OB.Upcall;
import org.apache.yoko.orb.PortableServer.Delegate;
import org.apache.yoko.util.Assert;
import org.omg.CORBA.ARG_IN;
import org.omg.CORBA.ARG_OUT;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.Bounds;
import org.omg.CORBA.NVList;
import org.omg.CORBA.NamedValue;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.TypeCodePackage.BadKind;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;
import org.omg.PortableServer.DynamicImplementation;

public class ServerRequest extends org.omg.CORBA.ServerRequest {
    private DynamicImplementation servant_;

    private Delegate delegate_;

    private Upcall up_;

    private InputStream in_;

    private NVList arguments_;

    private org.omg.CORBA.Context ctx_;

    private Any result_;

    private Any exception_;

    // ------------------------------------------------------------------
    // Standard IDL to Java Mapping
    // ------------------------------------------------------------------

    public String operation() {
        return up_.operation();
    }

    public void arguments(NVList parameters) {
        if (arguments_ != null)
            throw new BAD_INV_ORDER(
                    describeBadInvOrder(MinorInvalidUseOfDSIArguments),
                    MinorInvalidUseOfDSIArguments,
                    COMPLETED_NO);

        try {
            arguments_ = parameters;
            in_ = delegate_._OB_preUnmarshal(servant_, up_);
            try {
                for (int i = 0; i < parameters.count(); i++) {
                    NamedValue nv = parameters.item(i);

                    if (nv.flags() != ARG_OUT.value)
                        nv.value().read_value(in_, nv.value().type());
                }
            } catch (Bounds ex) {
                throw Assert.fail(ex);
            } catch (SystemException ex) {
                delegate_._OB_unmarshalEx(servant_, up_, ex);
            }
        } catch (LocationForward ex) {
            //
            // Translate into a RuntimeException to bypass standardized
            // interfaces
            //
            throw new RuntimeLocationForward(ex.ior,
                    ex.perm);
        }

        if (up_ instanceof PIUpcall) {
            PIUpcall piup = (PIUpcall) up_;
            piup.setArguments(parameters);
        }
    }

    public org.omg.CORBA.Context ctx() {
        if (arguments_ == null || ctx_ != null || result_ != null
                || exception_ != null)
            throw new BAD_INV_ORDER(
                    describeBadInvOrder(MinorInvalidUseOfDSIContext),
                    MinorInvalidUseOfDSIContext,
                    COMPLETED_NO);

        try {
            try {
                int len = in_.read_ulong();
                String[] strings = new String[len];
                for (int i = 0; i < len; i++)
                    strings[i] = in_.read_string();
                ctx_ = new Context(up_.orbInstance().getORB(), "", strings);
            } catch (SystemException ex) {
                delegate_._OB_unmarshalEx(servant_, up_, ex);
            }
        } catch (LocationForward ex) {
            //
            // Translate into a RuntimeException to bypass standardized
            // interfaces
            //
            throw new RuntimeLocationForward(ex.ior,
                    ex.perm);
        }

        return ctx_;
    }

    public void set_result(Any value) {
        if (arguments_ == null || result_ != null || exception_ != null)
            throw new BAD_INV_ORDER(
                    describeBadInvOrder(MinorInvalidUseOfDSIResult),
                    MinorInvalidUseOfDSIResult,
                    COMPLETED_NO);

        result_ = value;

        if (up_ instanceof PIUpcall) {
            PIUpcall piup = (PIUpcall) up_;
            piup.setResult(value);
        }
    }

    public void set_exception(Any value) {
        if (arguments_ == null)
            throw new BAD_INV_ORDER("arguments() has not "
                    + "been called");

        if (result_ != null)
            throw new BAD_INV_ORDER("set_result() has already "
                    + "been called");

        if (exception_ != null)
            throw new BAD_INV_ORDER("set_exception() has "
                    + "already been called");

        TypeCode origTC = TypeCodeImpl._OB_getOrigType(value.type());
        if (origTC.kind() != tk_except)
            throw new BAD_PARAM(
                    describeBadParam(MinorNoExceptionInAny),
                    MinorNoExceptionInAny,
                    COMPLETED_NO);

        exception_ = value;
    }

    // ------------------------------------------------------------------
    // Yoko internal functions
    // Application programs must not use these functions directly
    // ------------------------------------------------------------------

    public ServerRequest(DynamicImplementation servant, Upcall upcall) {
        servant_ = servant;
        delegate_ = (Delegate) servant._get_delegate();
        up_ = upcall;
    }

    public void _OB_finishUnmarshal() throws LocationForward {
        if (arguments_ == null) delegate_._OB_preUnmarshal(servant_, up_);
        delegate_._OB_postUnmarshal(servant_, up_);
    }

    public void _OB_postinvoke() throws LocationForward {
        if (exception_ == null) delegate_._OB_postinvoke(servant_, up_);
    }

    public void _OB_doMarshal() throws LocationForward {
        if (exception_ != null) {
            TypeCode tc = exception_.type();
            String id = null;
            try {
                id = tc.id();
            } catch (BadKind ex) {
                throw Assert.fail(ex);
            }

            if (isSystemException(id)) {
                InputStream in = exception_.create_input_stream();
                SystemException ex = readSysEx(in);
                throw ex;
            } else {
                up_.setUserException(exception_);
            }
        } else {
            OutputStream out = delegate_._OB_preMarshal(servant_, up_);

            try {
                if (result_ != null) result_.write_value(out);
                if (arguments_ != null) {
                    try {
                        for (int i = 0; i < arguments_.count(); i++) {
                            NamedValue nv = arguments_.item(i);
                            if (nv.flags() != ARG_IN.value) {
                                nv.value().write_value(out);
                            }
                        }
                    } catch (Bounds ex) {
                        throw Assert.fail(ex);
                    }
                }
            } catch (SystemException ex) {
                delegate_._OB_marshalEx(servant_, up_, ex);
            }

            delegate_._OB_postMarshal(servant_, up_);
        }
    }
}
