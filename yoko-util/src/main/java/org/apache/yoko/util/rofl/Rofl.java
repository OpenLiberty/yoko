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
package org.apache.yoko.util.rofl;

import org.omg.IOP.ServiceContext;
import org.omg.IOP.TaggedComponent;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;

import static java.util.Arrays.copyOf;
import static java.util.logging.Level.WARNING;
import static org.apache.yoko.util.Collectors.toUnmodifiableMap;
import static org.apache.yoko.util.HexConverter.toHex;
import static org.apache.yoko.util.rofl.Rofl.RemoteOrb.BAD;
import static org.apache.yoko.util.rofl.Rofl.RemoteOrb.IBM;
import static org.apache.yoko.util.rofl.Rofl.RemoteOrb.NO_DATA;

/**
 * <h2>ROFL &mdash; RemoteOrbFinessingLogic</h2>
 * This class encapsulates all the fixes that affect the stream format when talking to other ORBs.
 * These will be read in from two sources:
 * <ul>
 *     <li>a component tag in an IOR profile</li>
 *     <li>a service context in a GIOP packet</li>
 * </ul>
 * and referred to only when marshalling data.
 */
public interface Rofl extends Serializable {
    Rofl NONE = new None();

    enum RemoteOrb {
        /** The IBM Java ORB */
        IBM(0x49424D0A, IbmPartnerVersion::new, 0x49424D12, IbmPartnerVersion::new),
        BAD,
        NO_DATA
        ;
        // NOTE: enum static fields are initialized AFTER enum members, and therefore after the constructor
        /** Members that do not represent a real remote ORB */
        private static final EnumSet<RemoteOrb> SPECIAL_REMOTE_ORBS = EnumSet.of(BAD, NO_DATA);
        /** Members that do represent a real remote ORB */
        public static final EnumSet<RemoteOrb> KNOWN_REMOTE_ORBS = EnumSet.complementOf(SPECIAL_REMOTE_ORBS);
        /** Real remote ORBs indexed by service context ID */
        private static final Map<Integer, RemoteOrb> SC_ID_TO_RO_MAP = KNOWN_REMOTE_ORBS.stream()
                .collect(toUnmodifiableMap(HashMap::new, ro -> ro.serviceContextId));

        public final Integer tagComponentId;
        public final Integer serviceContextId;
        private final Function<TaggedComponent, Rofl> tcCtor;
        private final Function<ServiceContext, Rofl> scCtor;
        RemoteOrb() { this(null, null, null, null); }

        RemoteOrb(
                Integer tagComponentId, Function<TaggedComponent, Rofl> tcCtor,
                Integer serviceContextId, Function<ServiceContext, Rofl> scCtor) {
            this.tagComponentId = tagComponentId;
            this.serviceContextId = serviceContextId;
            this.tcCtor = tcCtor;
            this.scCtor = scCtor;
        }

        Rofl createRofl(TaggedComponent tc) {
            try {
                return tcCtor.apply(tc);
            } catch (Throwable t) {
                Logger.getLogger(Rofl.class.getName() + "." + name()).log(WARNING, "Failed to create ROFL for remote ORB of type " + this, t);
                return new Bad(tc, t);
            }

        }

        Rofl createRofl(ServiceContext sc) {
            try {
                return scCtor.apply(sc);
            } catch (Throwable t) {
                Logger.getLogger(Rofl.class.getName() + "." + name()).log(WARNING, "Failed to create ROFL for remote ORB of type " + this, t);
                return new Bad(sc, t);
            }
        }

        /**
         * Find the enum member for the given service context.
         * @param sc the service context for one of the known remote ORBs
         * @return the relevant enum member or <code>null</code> if the service context did not match any known remote ORB
         */
        static Optional<RemoteOrb> of(ServiceContext sc) { return Optional.of(sc.context_id).map(SC_ID_TO_RO_MAP::get); }
    }

    RemoteOrb type();

    enum SourceType { SERVICE_CONTEXT, TAGGED_COMPONENT }

    final class IbmPartnerVersion implements Rofl{
        private static final long serialVersionUID = 1L;
        public final SourceType sourceType;
        public final short major, minor, extended;
        IbmPartnerVersion(ServiceContext sc) { this(SourceType.SERVICE_CONTEXT, sc.context_data); }
        IbmPartnerVersion(TaggedComponent tc) { this(SourceType.TAGGED_COMPONENT, tc.component_data); }
        private IbmPartnerVersion(SourceType sourceType, byte[] data) {
            this.sourceType = sourceType;
            if (data.length != 8) {
                major = minor = extended = -1;
                return;
            }
            int i = 0;
            // 1 byte boolean: true iff littleEndian - ignore since Java is big-endian
            i++;
            // 1 byte padding - ignore
            i++;
            // extended short
            extended = (short)(((data[i++] & 0xFF) << 8) | (data[i++] & 0xFF));
            // major short
            major = (short)(((data[i++] & 0xFF) << 8) | (data[i++] & 0xFF));
            // minor short
            minor = (short)(((data[i++] & 0xFF) << 8) | (data[i++] & 0xFF));
        }
        public RemoteOrb type() { return IBM; }
        public String toString() { return String.format("IBM JAVA ORB[major=%04X minor=%04x, extended=%04X]", major, minor, extended); }
    }

    final class Bad implements Rofl {
        private static final long serialVersionUID = 1L;
        final SourceType sourceType;
        final int id;
        final byte[] data;
        final Throwable cause;
        Bad(ServiceContext sc, Throwable cause) {
            this.sourceType = SourceType.SERVICE_CONTEXT;
            this.id = sc.context_id;
            this.data = sc.context_data == null ? null : copyOf(sc.context_data, sc.context_data.length);
            this.cause = cause;
        }
        Bad(TaggedComponent tc, Throwable cause) {
            this.sourceType = SourceType.TAGGED_COMPONENT;
            this.id = tc.tag;
            this.data = tc.component_data == null ? null : copyOf(tc.component_data, tc.component_data.length);
            this.cause = cause;
        }
        public RemoteOrb type()  { return BAD; }
        public String toString() { return String.format("UNKNOWN ORB[%s(0x%08x) data=%s cause=%s]", sourceType, id, toHex(data), cause); }
    }

    final class None implements Rofl {
        public RemoteOrb type() { return NO_DATA; }
        public String toString() { return "NONE"; }
    }
}
