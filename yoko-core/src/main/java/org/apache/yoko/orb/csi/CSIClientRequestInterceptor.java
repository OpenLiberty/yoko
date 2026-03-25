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
package org.apache.yoko.orb.csi;

import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.Object;
import org.omg.CORBA.UserException;
import org.omg.CORBA.portable.ObjectImpl;
import org.omg.CSI.EstablishContext;
import org.omg.CSI.ITTDistinguishedName;
import org.omg.CSI.ITTPrincipalName;
import org.omg.CSI.IdentityToken;
import org.omg.CSI.MTCompleteEstablishContext;
import org.omg.CSI.MTContextError;
import org.omg.CSI.MTEstablishContext;
import org.omg.CSI.MTMessageInContext;
import org.omg.CSI.SASContextBody;
import org.omg.CSIIOP.AS_ContextSec;
import org.omg.CSIIOP.CompoundSecMech;
import org.omg.CSIIOP.CompoundSecMechList;
import org.omg.CSIIOP.CompoundSecMechListHelper;
import org.omg.CSIIOP.EstablishTrustInClient;
import org.omg.CSIIOP.SAS_ContextSec;
import org.omg.CSIIOP.TAG_CSI_SEC_MECH_LIST;
import org.omg.GSSUP.InitialContextToken;
import org.omg.IOP.Codec;
import org.omg.IOP.SecurityAttributeService;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.TaggedComponent;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ForwardRequest;

import java.util.Arrays;
import java.util.logging.Logger;

import static org.apache.yoko.orb.csi.CSIInterceptorBase.CallStatus.pushIsLocal;
import static org.apache.yoko.orb.csi.SecurityContext.getAuthenticationInfo;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;

public class CSIClientRequestInterceptor extends CSIInterceptorBase
        implements org.omg.PortableInterceptor.ClientRequestInterceptor
{

    CSIClientRequestInterceptor(Codec codec) {
        super(codec);
    }

    private static final Logger log = Logger
            .getLogger(CSIClientRequestInterceptor.class.getName());

    //
    // CLIENT REQUEST API
    //

    public void send_request(ClientRequestInfo ri) throws ForwardRequest {
        Object target = ri.effective_target();

        if (target instanceof ObjectImpl) {
            boolean isLocal = ((ObjectImpl) target)
                    ._is_local();

            // save value of isLocal
            if (ri.response_expected())
                pushIsLocal(isLocal);

            // ignore CSI for local calls
            if (isLocal) {
                return;
            }
        }

        final CompoundSecMech mech;
        try {
            TaggedComponent tc = ri.get_effective_component(TAG_CSI_SEC_MECH_LIST.value);

            byte[] data = tc.component_data;

            Any sl_any = codec.decode_value(data, CompoundSecMechListHelper.type());
            CompoundSecMechList sl = CompoundSecMechListHelper.extract(sl_any);

            if (sl.mechanism_list.length == 0) {
                log.fine(() -> "empty sec mech list");
                return;
            }

            mech = sl.mechanism_list[0];

        } catch (BAD_PARAM ex) {
            log.fine(() -> "no security mechanism");
            return;
        } catch (UserException e) {
            MARSHAL me = new MARSHAL("cannot decode local security descriptor", 0, COMPLETED_NO);
            me.initCause(e);
            throw me;
        }

        log.fine(() -> "transport_mech tag = " + mech.transport_mech.tag);

        final String target_name;
        final boolean target_supports_gssup;
        final boolean target_requires_gssup;

        AS_ContextSec as = mech.as_context_mech;
        if (as != null && Arrays.equals(GSSUP_OID, as.client_authentication_mech)) {
            target_requires_gssup = (as.target_requires & EstablishTrustInClient.value) != 0;
            target_supports_gssup = (as.target_supports & EstablishTrustInClient.value) != 0;
            target_name = decodeGSSExportedName(as.target_name);
            log.fine(() -> "decoded target name = " + target_name);
        } else {
            target_supports_gssup = false;
            target_requires_gssup = false;
        }

        boolean support_gssup_delegation = false;
        boolean support_x500_delegation = false;

        if (target_supports_gssup) {
            log.fine(() -> "AS SPEC:" + " target_supports=" + target_supports_gssup + " target_requires=" + target_requires_gssup);
        } else {
            SAS_ContextSec sas = mech.sas_context_mech;
            for (byte[] supportedNamingMechanism: sas.supported_naming_mechanisms) {
                if (Arrays.equals(GSSUP_OID, supportedNamingMechanism) && (sas.supported_identity_types & ITTPrincipalName.value) != 0) {
                    support_gssup_delegation = true;
                    log.fine(() -> "target supports GSSUP identity delegation");
                    break;
                }
            }

            if ((sas.supported_identity_types & ITTDistinguishedName.value) != 0) {
                support_x500_delegation = true;
            }

            if (!support_gssup_delegation && !support_x500_delegation) {
                log.fine(() -> "target supports security, but not GSSUP/X500");
                return;
            }
        }

        AuthenticationInfo authInfo = getAuthenticationInfo();

        if (authInfo == null) {
            log.fine(() -> "no auth info");
            return;
        }

        String name = authInfo.getPrincipalName();
        String realm = authInfo.getRealm();
        String password = authInfo.getPassword();

        SASContextBody sasBody = new SASContextBody();

        EstablishContext establishMsg = new EstablishContext();

        // Indicate stateless CSS
        establishMsg.client_context_id = 0;

        // Make empty authorization token list
        establishMsg.authorization_token = EMPTY_AUTH_ELEMENT;

        String scopedUserName = name + "@" + realm;

        if (support_gssup_delegation) {

            establishMsg.client_authentication_token = EMPTY_BARR;

            //
            // indicate identitytoken as ITTPrincipalName
            //
            IdentityToken identityToken = new IdentityToken();
            identityToken
                    .principal_name(encapsulateByteArray(encodeGSSExportedName(scopedUserName)));
            establishMsg.identity_token = identityToken;

            log.fine(() -> "send_request, name: \"" + scopedUserName + "\"");
        } else {

            // Make GSSUP InitialContextToken
            InitialContextToken gssupToken = new InitialContextToken();
            gssupToken.username = utf8encode(scopedUserName);
            gssupToken.target_name = encodeGSSExportedName(realm);
            gssupToken.password = utf8encode(password);

            establishMsg.client_authentication_token = encodeGSSUPToken(gssupToken);

            // Indicate identity token is ITTAbsent
            IdentityToken identityToken = new IdentityToken();
            identityToken.absent(true);
            establishMsg.identity_token = identityToken;

            log.fine(() -> "send_request, name: \"" + scopedUserName
                    + "\", pw: \"" + password + "\"");
        }

        sasBody.establish_msg(establishMsg);

        ri.add_request_service_context(encodeSASContextBody(sasBody), true);
    }

    public void send_poll(ClientRequestInfo ri) {
    }

    public void receive_reply(ClientRequestInfo ri) {
        // ignore tx for local calls
        if (CallStatus.popIsLocal()) {
            return;
        }

        ServiceContext serviceContext;
        try {
            serviceContext = ri
                    .get_reply_service_context(SecurityAttributeService.value);
        }
        catch (org.omg.CORBA.BAD_PARAM ex) {
            serviceContext = null;
        }

        SASContextBody sasBody = null;

        if (serviceContext != null) {
            sasBody = decodeSASContextBody(serviceContext);

            switch (sasBody.discriminator()) {
                case MTEstablishContext.value:
                case MTMessageInContext.value:
                    // Unexpected
                    log.severe("Unexpected message of type "
                              + sasBody.discriminator());
                    break;
                case MTCompleteEstablishContext.value:
                    // Things went well
                    break;
                case MTContextError.value:
                    // Things did not go well
                    break;
            }
        }
    }

    public void receive_exception(ClientRequestInfo ri) throws ForwardRequest {
        log.fine(() -> "receive_exception");
        receive_reply(ri);
    }

    public void receive_other(ClientRequestInfo ri) throws ForwardRequest {
        log.fine(() -> "receive_other");
        receive_reply(ri);
    }

    public String name() {
        return "CSI Client Interceptor";
    }

	public void destroy() {
	}

}
