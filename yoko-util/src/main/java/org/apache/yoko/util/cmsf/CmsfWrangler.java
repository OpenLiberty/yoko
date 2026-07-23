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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.apache.yoko.util.cmsf;

import org.apache.yoko.util.InfoWrangler;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.ORB;
import org.omg.IOP.RMICustomMaxStreamFormat;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.TAG_RMI_CUSTOM_MAX_STREAM_FORMAT;
import org.omg.IOP.TaggedComponent;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.IORInfo;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableInterceptor.ServerRequestInfo;

import static org.apache.yoko.util.Exceptions.as;
import static org.apache.yoko.util.MinorCodes.MinorInvalidComponentId;
import static org.apache.yoko.util.MinorCodes.MinorInvalidServiceContextId;
import static org.apache.yoko.util.cmsf.Cmsf.CMSFv1;
import static org.apache.yoko.util.cmsf.Cmsf.CMSFv2;

/**
 * HeaderInfoWrangler implementation for CMSF (CodeBase Message Stream Format)
 */
public enum CmsfWrangler implements InfoWrangler<Cmsf> {
    CMSF_WRANGLER;

    @Override
    public void addTc(IORInfo info) {
        info.add_ior_component(new TaggedComponent(TAG_RMI_CUSTOM_MAX_STREAM_FORMAT.value, CMSFv2.toData()));
    }

    @Override
    public void addSc(ClientRequestInfo ri) {
        ri.add_request_service_context(new ServiceContext(RMICustomMaxStreamFormat.value, CMSFv2.toData()), false);
    }

    @Override
    public void addSc(ServerRequestInfo ri) {
        ri.add_reply_service_context(new ServiceContext(RMICustomMaxStreamFormat.value, CMSFv2.toData()), false);
    }

    @Override
    public Cmsf readData(ClientRequestInfo ri) {
        try {
            TaggedComponent tc = ri.get_effective_component(TAG_RMI_CUSTOM_MAX_STREAM_FORMAT.value);
            return readData(tc.component_data);
        } catch (BAD_PARAM e) {
            if (e.minor != MinorInvalidComponentId) throw e;
        }
        return CMSFv1;
    }

    @Override
    public Cmsf readData(ServerRequestInfo ri) {
        try {
            ServiceContext sc = ri.get_request_service_context(RMICustomMaxStreamFormat.value);
            return readData(sc.context_data);
        } catch (BAD_PARAM e) {
            if (e.minor != MinorInvalidServiceContextId) throw e;
        }
        return CMSFv1;
    }

    private Cmsf readData(byte[] data) {
        try {
            return (null == data) ? CMSFv1 : Cmsf.get(data[1]);
        } catch (Exception e) {
            throw as(MARSHAL::new, e, e.getMessage());
        }
    }

    @Override
    public void setSlot(int slotId, ServerRequestInfo ri, Cmsf cmsf) {
        Any any = ORB.init().create_any();
        any.insert_octet(cmsf.getValue());
        try {
            ri.set_slot(slotId, any);
        } catch (InvalidSlot e) {
            throw as(INTERNAL::new, e, e.getMessage());
        }
    }

    @Override
    public Cmsf getSlot(int slotId, ServerRequestInfo ri) {
        try {
            Any any = ri.get_slot(slotId);
            return (null == any) ? CMSFv1 : Cmsf.get(any.extract_octet());
        } catch (InvalidSlot e) {
            throw as(INTERNAL::new, e, e.getMessage());
        }
    }
}
