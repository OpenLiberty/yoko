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
package org.apache.yoko.util.yasf;

import org.apache.yoko.util.InfoWrangler;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.ORB;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.TaggedComponent;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.IORInfo;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableInterceptor.ServerRequestInfo;

import java.util.Set;

import static org.apache.yoko.util.Exceptions.as;
import static org.apache.yoko.util.MinorCodes.MinorInvalidComponentId;
import static org.apache.yoko.util.MinorCodes.MinorInvalidServiceContextId;
import static org.apache.yoko.util.yasf.Yasf.TAG_YOKO_AUXILIARY_STREAM_FORMAT;
import static org.apache.yoko.util.yasf.Yasf.YOKO_AUXILIARY_STREAM_FORMAT_SC;

/**
 * HeaderInfoWrangler implementation for YASF (Yoko Auxiliary Stream Format)
 */
public enum YasfWrangler implements InfoWrangler<Set<Yasf>> {
    YASF_WRANGLER;

    @Override
    public void addTc(IORInfo info) {
        TaggedComponent tc = new TaggedComponent(TAG_YOKO_AUXILIARY_STREAM_FORMAT, Yasf.toData());
        info.add_ior_component(tc);
    }

    private static ServiceContext sc() {
        return new ServiceContext(YOKO_AUXILIARY_STREAM_FORMAT_SC, Yasf.toData());
    }

    @Override
    public void addSc(ClientRequestInfo ri) {
        ServiceContext sc = sc();
        ri.add_request_service_context(sc, false);
    }

    @Override
    public void addSc(ServerRequestInfo ri) {
        ServiceContext sc = sc();
        ri.add_reply_service_context(sc, false);
    }

    @Override
    public Set<Yasf> readData(ClientRequestInfo ri) {
        try {
            TaggedComponent tc = ri.get_effective_component(TAG_YOKO_AUXILIARY_STREAM_FORMAT);
            return Yasf.toSet(tc.component_data);
        } catch (BAD_PARAM e) {
            if (e.minor != MinorInvalidComponentId) throw e;
        }
        return null;
    }

    @Override
    public Set<Yasf> readData(ServerRequestInfo ri) {
        try {
            ServiceContext sc = ri.get_request_service_context(YOKO_AUXILIARY_STREAM_FORMAT_SC);
            return Yasf.toSet(sc.context_data);
        } catch (BAD_PARAM e) {
            if (e.minor != MinorInvalidServiceContextId) throw e;
        }
        return null;
    }

    @Override
    public void setSlot(int slotId, ServerRequestInfo ri, Set<Yasf> data) {
        Any any = ORB.init().create_any();
        any.insert_Value(Yasf.toData(data));
        try {
            ri.set_slot(slotId, any);
        } catch (InvalidSlot e) {
            throw as(INTERNAL::new, e, e.getMessage());
        }
    }

    @Override
    public Set<Yasf> getSlot(int slotId, ServerRequestInfo ri) {
        try {
            Any any = ri.get_slot(slotId);
            byte[] data = (byte[])any.extract_Value();
            return Yasf.toSet(data);
        } catch (InvalidSlot e) {
            throw as(INTERNAL::new, e, e.getMessage());
        }
    }
}
