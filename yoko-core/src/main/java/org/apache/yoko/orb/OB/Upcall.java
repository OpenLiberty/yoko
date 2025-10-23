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
package org.apache.yoko.orb.OB;

import static java.util.logging.Logger.getLogger;
import static org.apache.yoko.io.Buffer.createWriteBuffer;
import static org.apache.yoko.orb.OB.SendingContextRuntimes.SENDING_CONTEXT_RUNTIME;
import static org.apache.yoko.orb.OCI.GiopVersion.GIOP1_2;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.yoko.orb.CORBA.YokoInputStream;
import org.apache.yoko.orb.CORBA.YokoOutputStream;
import org.apache.yoko.orb.IOP.ServiceContexts;
import org.apache.yoko.orb.OBPortableServer.POA_impl;
import org.apache.yoko.orb.OCI.GiopVersion;
import org.apache.yoko.orb.OCI.ProfileInfo;
import org.apache.yoko.orb.OCI.TransportInfo;
import org.apache.yoko.util.Assert;
import org.apache.yoko.util.Timeout;
import org.apache.yoko.util.cmsf.CmsfThreadLocal;
import org.apache.yoko.util.cmsf.CmsfThreadLocal.CmsfOverride;
import org.omg.CORBA.Any;
import org.omg.CORBA.Policy;
import org.omg.CORBA.PolicyManager;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.UserException;
import org.omg.CORBA.portable.UnknownException;
import org.omg.IOP.IOR;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.UnknownExceptionInfo;
import org.omg.PortableServer.Servant;

public class Upcall {
    private static final Logger logger = getLogger(Upcall.class.getName());
    private final ORBInstance orbInstance_;

    // Upcall delegates to UpcallReturn upon return from the upcall.
    // If this is null, no response is expected - i.e. this call is oneway.
    protected final UpcallReturn upcallReturn_;

    private final ProfileInfo profileInfo_;

    protected final TransportInfo transportInfo_;

    private final int reqId_;

    protected final String op_;

    // Holds the inout/out parameters and return value
    private YokoOutputStream out_;

    // Holds the in/inout parameters
    private final YokoInputStream in_;

    protected final ServiceContexts requestContexts;
    protected final ServiceContexts replyContexts;
    private DispatchRequest dispatchRequest_;
    private DispatchStrategy dispatchStrategy_;
    private Servant servant_;
    private POA_impl poa_;
    private boolean postinvokeCalled_;

    // Whether beginUserException has been called. We must delay the
    // call to endUserException until we are sure nothing else will
    // be marshalled (e.g., in case a SystemException occurs after
    // marshalling). This flag is set to true in beginUserException.
    // Only a SystemException or LocationForward can occur after
    // a UserException, so the flag is reset to false in
    // setSystemException and setLocationForward.
    private boolean userEx_;

    protected ServiceContext codeSetSC_;
    private ServiceContext codeBaseSC_;
    private final Timeout timeout;

    public Upcall(ORBInstance orbInstance, UpcallReturn upcallReturn, ProfileInfo profileInfo,
                  TransportInfo transportInfo, int requestId, String op, YokoInputStream in, ServiceContexts requestContexts) {
        this.orbInstance_ = orbInstance;
        this.upcallReturn_ = upcallReturn;
        this.profileInfo_ = profileInfo;
        this.transportInfo_ = transportInfo;
        this.reqId_ = requestId;
        this.op_ = op;
        this.in_ = in;
        this.requestContexts = requestContexts;
        this.replyContexts = new ServiceContexts();
        this.servant_ = null;
        this.poa_ = null;
        this.postinvokeCalled_ = false;

        this.userEx_ = false;

        logger.fine("Creating upcall request for operation " + op + " and request id " + requestId);
        in._OB_ORBInstance(orbInstance_);

        // get the reply timeout
        PolicyManager pm = orbInstance.getPolicyManager();
        final Policy[] policy_overrides = pm.get_policy_overrides(new int[0]);
        RefCountPolicyList policies = new RefCountPolicyList(policy_overrides);
        timeout = Timeout.in(policies.replyTimeout);
    }

    public ORBInstance orbInstance() {
        return orbInstance_;
    }

    public ProfileInfo profileInfo() {
        return profileInfo_;
    }

    public TransportInfo transportInfo() {
        return transportInfo_;
    }

    public int requestId() {
        return reqId_;
    }

    public String operation() {
        return op_;
    }

    public boolean responseExpected() {
        return upcallReturn_ != null;
    }

    public boolean postinvokeCalled() {
        return postinvokeCalled_;
    }

    public YokoOutputStream output() {
        return out_;
    }

    public YokoInputStream input() {
        return in_;
    }

    public void createOutputStream(int offset) {
        final GiopVersion giopVersion = GiopVersion.get(profileInfo_.major, profileInfo_.minor);
        out_ = new YokoOutputStream(createWriteBuffer(offset).padAll(), in_._OB_codeConverters(), giopVersion);
    }

    public YokoInputStream preUnmarshal() {
        return in_;
    }

    public void unmarshalEx(SystemException ex) {
        throw ex;
    }

    public void postUnmarshal() throws LocationForward {
    }

    public void postinvoke() {
        if (servant_ != null) {
            Assert.ensure(poa_ != null && !postinvokeCalled_);
            servant_ = null;
            postinvokeCalled_ = true;
            poa_._OB_postinvoke(); // May raise SystemException
        }
    }

    // initialize internal service contexts
    private void initServiceContexts() {
        if (codeBaseSC_ == null) codeBaseSC_ = SENDING_CONTEXT_RUNTIME;
        // NOTE: We don't initialize the INVOCATION_POLICIES service context
        // here because the list of policies can change from one invocation to
        // the next. Instead, we need to get the policies and build the
        // service context each time we make an invocation.
    }

    public YokoOutputStream preMarshal() throws LocationForward {
        // If we have an UpcallReturn object, then invoking upcallBeginReply
        // will eventually result in a call to createOutputStream.
        //
        // If we don't have an UpcallReturn object, then it means a oneway
        // invocation was made for a twoway operation. We return a dummy
        // OutputStream to make the skeleton happy and avoid a crash.
        if (upcallReturn_ != null) {
            addUnsentConnectionServiceContexts();
            upcallReturn_.upcallBeginReply(this, replyContexts);
        } else {
            out_ = new YokoOutputStream(in_._OB_codeConverters(), GiopVersion.get(profileInfo_.major, profileInfo_.minor));
        }
        out_._OB_ORBInstance(this.orbInstance());
        if (out_ != null) out_.setTimeout(timeout);
        return out_;
    }

    private void addUnsentConnectionServiceContexts() {
        if (upcallReturn_.replySent()) return;
        if (profileInfo_.major <= 1 && profileInfo_.minor < 1) return;
        initServiceContexts();
        Assert.ensure(codeBaseSC_ != null);
        replyContexts.mutable().add(codeBaseSC_);
    }

    public void marshalEx(SystemException ex) {
        throw ex;
    }

    public void postMarshal() {
        if (upcallReturn_ != null)
            upcallReturn_.upcallEndReply(this);
        out_.setTimeout(Timeout.NEVER);
    }

    public void setUserException(Any any) {
        if (upcallReturn_ != null) {
            upcallReturn_.upcallBeginUserException(this, replyContexts);
            try {
                any.write_value(out_);
            } catch (SystemException ex) {
                marshalEx(ex);
            }
            //
            // In Java, we must delay the call to upcallEndUserException
            //
            // upcallReturn_.upcallEndUserException(this);
            userEx_ = true;
            if (out_ != null) out_.setTimeout(Timeout.NEVER);
        }
    }

    // The skeleton marshals the exception. If called by a portable
    // skeleton, the exception will be null.
    public YokoOutputStream beginUserException(UserException ex) {
        if (upcallReturn_ != null) {
            upcallReturn_.upcallBeginUserException(this, replyContexts);
            userEx_ = true;
            return out_;
        }

        return null;
    }

    public boolean userException() {
        return userEx_;
    }

    public void endUserException() {
        if (upcallReturn_ != null) {
            Assert.ensure(userEx_);
            upcallReturn_.upcallEndUserException(this);
        }
    }

    public void setSystemException(SystemException ex) {
        if (upcallReturn_ != null) {
            addUnsentConnectionServiceContexts();
            userEx_ = false;
            if (ex instanceof UnknownException) {
                // need to create service contexts for underlying exception
                createUnknownExceptionServiceContexts((UnknownException)ex, replyContexts);
            }
            upcallReturn_.upcallSystemException(this, ex, replyContexts);
        }
    }

    private static void createUnknownExceptionServiceContexts(UnknownException ex, ServiceContexts replyContexts) {
        final Throwable t = ex.originalEx;
        try (CmsfOverride o = CmsfThreadLocal.override()) {
            CodeConverters codeConverters = CodeConverters.createForWcharWriteOnly();
            try (YokoOutputStream os = new YokoOutputStream(codeConverters, GIOP1_2)) {
                os._OB_writeEndian();
                os.write_value(t, Throwable.class);
                ServiceContext sc = new ServiceContext(UnknownExceptionInfo.value, os.copyWrittenBytes());
                replyContexts.mutable().add(sc, false);
            }
        }
    }

    public void setLocationForward(IOR ior, boolean perm) {
        if (upcallReturn_ == null) return;
        userEx_ = false;
        upcallReturn_.upcallForward(this, ior, perm, replyContexts);
    }

    public void contextSwitch() {
        // Do nothing.
    }

    public void setDispatchInfo(DispatchRequest dispatchRequest, DispatchStrategy dispatchStrategy) {
        dispatchRequest_ = dispatchRequest;
        dispatchStrategy_ = dispatchStrategy;
    }

    public void setServantAndPOA(Servant servant, POA_impl poa) {
        servant_ = servant;
        poa_ = poa;
    }

    public void invoke() {
        // If the creation of the Upcall object resulted in an error of
        // some sort then there will be no method invocation to perform.
        // In this case do nothing.
        try {
            if (dispatchStrategy_ != null) {
                logger.fine("Dispatching request " + reqId_ + " with dispatch strategy " + dispatchStrategy_.getClass().getName());
                dispatchStrategy_.dispatch(dispatchRequest_);
            }
        } catch (SystemException ex) {
            logger.log(Level.FINE, "Exception received dispatching request", ex);
            setSystemException(ex);
        }
    }
}
