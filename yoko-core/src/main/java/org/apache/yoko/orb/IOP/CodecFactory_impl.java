/*
 * Copyright 2021 IBM Corporation and others.
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
package org.apache.yoko.orb.IOP;

import org.apache.yoko.util.Assert;

final public class CodecFactory_impl extends org.omg.CORBA.LocalObject
        implements org.omg.IOP.CodecFactory {
    private org.omg.IOP.Codec cdrCodec_; // Cached CDR Codec

    private org.apache.yoko.orb.OB.ORBInstance orbInstance_; // The
                                                                // ORBInstance

    // ----------------------------------------------------------------------
    // CodecFactory_impl public member implementation
    // ----------------------------------------------------------------------

    public org.omg.IOP.Codec create_codec(org.omg.IOP.Encoding encoding)
            throws org.omg.IOP.CodecFactoryPackage.UnknownEncoding {
        Assert.ensure(orbInstance_ != null);

        // TODO: check major/minor version
        if (encoding.format != org.omg.IOP.ENCODING_CDR_ENCAPS.value)
            throw new org.omg.IOP.CodecFactoryPackage.UnknownEncoding();

        synchronized (this) {
            if (cdrCodec_ == null)
                cdrCodec_ = new CDRCodec(orbInstance_);
        }

        return cdrCodec_;
    }

    // ------------------------------------------------------------------
    // Yoko internal functions
    // Application programs must not use these functions directly
    // ------------------------------------------------------------------

    public void _OB_setORBInstance(
            org.apache.yoko.orb.OB.ORBInstance orbInstance) {
        orbInstance_ = orbInstance;
    }
}
