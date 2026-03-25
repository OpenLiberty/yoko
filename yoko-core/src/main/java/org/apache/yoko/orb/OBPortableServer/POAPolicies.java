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

import org.apache.yoko.orb.OB.DispatchStrategy;
import org.apache.yoko.orb.OB.DispatchStrategyFactory;
import org.apache.yoko.orb.OB.ORBInstance;
import org.apache.yoko.orb.OB.ZERO_PORT_POLICY_ID;
import org.apache.yoko.orb.OB.ZeroPortPolicyHelper;
import org.apache.yoko.orb.PortableServer.IdAssignmentPolicy_impl;
import org.apache.yoko.orb.PortableServer.IdUniquenessPolicy_impl;
import org.apache.yoko.orb.PortableServer.ImplicitActivationPolicy_impl;
import org.apache.yoko.orb.PortableServer.LifespanPolicy_impl;
import org.apache.yoko.orb.PortableServer.RequestProcessingPolicy_impl;
import org.apache.yoko.orb.PortableServer.ServantRetentionPolicy_impl;
import org.omg.BiDirPolicy.BIDIRECTIONAL_POLICY_TYPE;
import org.omg.BiDirPolicy.BOTH;
import org.omg.BiDirPolicy.BidirectionalPolicyHelper;
import org.omg.CORBA.Policy;
import org.omg.PortableServer.ID_ASSIGNMENT_POLICY_ID;
import org.omg.PortableServer.ID_UNIQUENESS_POLICY_ID;
import org.omg.PortableServer.IMPLICIT_ACTIVATION_POLICY_ID;
import org.omg.PortableServer.IdAssignmentPolicyHelper;
import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.IdUniquenessPolicyHelper;
import org.omg.PortableServer.IdUniquenessPolicyValue;
import org.omg.PortableServer.ImplicitActivationPolicyHelper;
import org.omg.PortableServer.ImplicitActivationPolicyValue;
import org.omg.PortableServer.LIFESPAN_POLICY_ID;
import org.omg.PortableServer.LifespanPolicyHelper;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.REQUEST_PROCESSING_POLICY_ID;
import org.omg.PortableServer.RequestProcessingPolicyHelper;
import org.omg.PortableServer.RequestProcessingPolicyValue;
import org.omg.PortableServer.SERVANT_RETENTION_POLICY_ID;
import org.omg.PortableServer.ServantRetentionPolicyHelper;
import org.omg.PortableServer.ServantRetentionPolicyValue;
import org.omg.PortableServer.THREAD_POLICY_ID;
import org.omg.PortableServer.ThreadPolicyHelper;
import org.omg.PortableServer.ThreadPolicyValue;

import static org.apache.yoko.logging.VerboseLogging.POA_INIT_LOG;
import static org.apache.yoko.orb.OBPortableServer.SynchronizationPolicyValue.NO_SYNCHRONIZATION;
import static org.apache.yoko.orb.OBPortableServer.SynchronizationPolicyValue.SYNCHRONIZE_ON_ORB;
import static org.omg.PortableServer.IdAssignmentPolicyValue.SYSTEM_ID;
import static org.omg.PortableServer.IdUniquenessPolicyValue.UNIQUE_ID;
import static org.omg.PortableServer.ImplicitActivationPolicyValue.NO_IMPLICIT_ACTIVATION;
import static org.omg.PortableServer.LifespanPolicyValue.TRANSIENT;
import static org.omg.PortableServer.RequestProcessingPolicyValue.USE_ACTIVE_OBJECT_MAP_ONLY;
import static org.omg.PortableServer.ServantRetentionPolicyValue.RETAIN;
import static org.omg.PortableServer.ThreadPolicyValue.SINGLE_THREAD_MODEL;

final public class POAPolicies {
    private final boolean interceptorCallPolicy;
    private final SynchronizationPolicyValue synchronizationPolicy;
    private final DispatchStrategy dispatchStrategyPolicy;
    private final boolean zeroPortPolicy;
    private final LifespanPolicyValue lifespanPolicy;
    private final IdUniquenessPolicyValue idUniquenessPolicy;
    private final IdAssignmentPolicyValue idAssignmentPolicy;
    private final ImplicitActivationPolicyValue implicitActivationPolicy;
    private final ServantRetentionPolicyValue servantRetentionPolicy;
    private final RequestProcessingPolicyValue requestProcessingPolicy;
    private final short bidirPolicyValue;

    POAPolicies(ORBInstance orbInstance, Policy[] policies) {
        // Set the default policy values.
        // These are temporary variables to allow the final fields to be set after the for loop.
        // TODO: refactor to use a builder pattern for conciseness
        boolean interceptorCall = true;
        SynchronizationPolicyValue synchronization = null;
        ThreadPolicyValue thread = null;
        boolean zeroPort = false;
        LifespanPolicyValue lifespan = TRANSIENT;
        IdUniquenessPolicyValue idUniqueness = UNIQUE_ID;
        IdAssignmentPolicyValue idAssignment = SYSTEM_ID;
        ImplicitActivationPolicyValue implicitActivation = NO_IMPLICIT_ACTIVATION;
        ServantRetentionPolicyValue servantRetention = RETAIN;
        RequestProcessingPolicyValue requestProcessing = USE_ACTIVE_OBJECT_MAP_ONLY;
        DispatchStrategy dispatchStrategy = null;
        short bidir = BOTH.value;

        if (policies != null) {
            for (Policy policy : policies) {
                switch (policy.policy_type()) {
                    // CORBA standard policies
                    case THREAD_POLICY_ID.value: thread = ThreadPolicyHelper.narrow(policy).value(); break;
                    case LIFESPAN_POLICY_ID.value: lifespan = LifespanPolicyHelper.narrow(policy).value(); break;
                    case ID_UNIQUENESS_POLICY_ID.value: idUniqueness = IdUniquenessPolicyHelper.narrow(policy).value(); break;
                    case ID_ASSIGNMENT_POLICY_ID.value: idAssignment = IdAssignmentPolicyHelper.narrow(policy).value(); break;
                    case IMPLICIT_ACTIVATION_POLICY_ID.value: implicitActivation = ImplicitActivationPolicyHelper.narrow(policy).value(); break;
                    case SERVANT_RETENTION_POLICY_ID.value: servantRetention = ServantRetentionPolicyHelper.narrow(policy).value(); break;
                    case REQUEST_PROCESSING_POLICY_ID.value: requestProcessing = RequestProcessingPolicyHelper.narrow(policy).value(); break;
                    case BIDIRECTIONAL_POLICY_TYPE.value: bidir = BidirectionalPolicyHelper.narrow(policy).value(); break;
                    // Yoko proprietary policies
                    case SYNCHRONIZATION_POLICY_ID.value: synchronization = SynchronizationPolicyHelper.narrow(SynchronizationPolicyHelper.narrow(policy)).value(); break;
                    case DISPATCH_STRATEGY_POLICY_ID.value: dispatchStrategy = DispatchStrategyPolicyHelper.narrow(DispatchStrategyPolicyHelper.narrow(policy)).value(); break;
                    case INTERCEPTOR_CALL_POLICY_ID.value: interceptorCall = InterceptorCallPolicyHelper.narrow(InterceptorCallPolicyHelper.narrow(policy)).value(); break;
                    case ZERO_PORT_POLICY_ID.value: zeroPort = ZeroPortPolicyHelper.narrow(ZeroPortPolicyHelper.narrow(policy)).value(); break;
                    default:
                        // Unknown policy
                        POA_INIT_LOG.warning(() -> String.format("Ignoring unsupported policy of type 0x%x", policy.policy_type()));
                }
            }
        }

        zeroPortPolicy = zeroPort;
        interceptorCallPolicy = interceptorCall;
        bidirPolicyValue = bidir;
        requestProcessingPolicy = requestProcessing;
        servantRetentionPolicy = servantRetention;
        implicitActivationPolicy = implicitActivation;
        idAssignmentPolicy = idAssignment;
        idUniquenessPolicy = idUniqueness;
        lifespanPolicy = lifespan;
        // the synchronization policy can be set explicitly (proprietary), or derived from the thread policy (standard)
        if (null == synchronization) {
            // only consider the thread policy if there was no explicit synchronization policy
            synchronizationPolicy = SINGLE_THREAD_MODEL == thread ? SYNCHRONIZE_ON_ORB : NO_SYNCHRONIZATION;
        } else {
            synchronizationPolicy = synchronization;
        }
        if (dispatchStrategy == null) {
            DispatchStrategyFactory dsf = orbInstance.getDispatchStrategyFactory();
            dispatchStrategy = dsf.create_default_dispatch_strategy();
        }
        dispatchStrategyPolicy = dispatchStrategy;
    }

    public boolean interceptorCallPolicy() {
        return interceptorCallPolicy;
    }

    public boolean zeroPortPolicy() {
        return zeroPortPolicy;
    }

    public SynchronizationPolicyValue synchronizationPolicy() {
        // TODO: Fix this
        // if(OBORB_impl.server_conc_model() ==
        // ORBORB_impl.ServerConcModelThreaded)
        // return org.apache.yoko.orb.OB.SYNCHRONIZE_ON_ORB;

        return synchronizationPolicy;
    }

    public DispatchStrategy dispatchStrategyPolicy() {
        return dispatchStrategyPolicy;
    }

    public LifespanPolicyValue lifespanPolicy() {
        return lifespanPolicy;
    }

    public IdUniquenessPolicyValue idUniquenessPolicy() {
        return idUniquenessPolicy;
    }

    public IdAssignmentPolicyValue idAssignmentPolicy() {
        return idAssignmentPolicy;
    }

    public ImplicitActivationPolicyValue implicitActivationPolicy() {
        return implicitActivationPolicy;
    }

    public ServantRetentionPolicyValue servantRetentionPolicy() {
        return servantRetentionPolicy;
    }

    public RequestProcessingPolicyValue requestProcessingPolicy() {
        return requestProcessingPolicy;
    }

    public short bidirPolicy() {
        return bidirPolicyValue;
    }

    public Policy[] recreate() {
        //
        // TODO:
        //
        // No ThreadPolicy policy appended. The problem is that some
        // values of SyncPolicy don't map. I guess the real solution
        // to this is to only create those policies that were
        // provided. Also, providing both Sync policy and ThreadPolicy
        // should be invalid.
        //
        Policy[] pl = {
            new LifespanPolicy_impl(lifespanPolicy),
            new IdUniquenessPolicy_impl(idUniquenessPolicy),
            new IdAssignmentPolicy_impl(idAssignmentPolicy),
            new ImplicitActivationPolicy_impl(implicitActivationPolicy),
            new ServantRetentionPolicy_impl(servantRetentionPolicy),
            new RequestProcessingPolicy_impl(requestProcessingPolicy),
            new SynchronizationPolicy_impl(synchronizationPolicy),
            new DispatchStrategyPolicy_impl(dispatchStrategyPolicy),
            new InterceptorCallPolicy_impl(interceptorCallPolicy),
        };
        return pl;
    }
}
