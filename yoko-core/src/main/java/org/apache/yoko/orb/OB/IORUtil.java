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

import org.apache.yoko.orb.CORBA.YokoInputStream;
import org.omg.CONV_FRAME.CodeSetComponentInfo;
import org.omg.CONV_FRAME.CodeSetComponentInfoHelper;
import org.omg.CSI.ITTAbsent;
import org.omg.CSI.ITTAnonymous;
import org.omg.CSI.ITTDistinguishedName;
import org.omg.CSI.ITTPrincipalName;
import org.omg.CSI.ITTX509CertChain;
import org.omg.CSIIOP.CompositeDelegation;
import org.omg.CSIIOP.CompoundSecMech;
import org.omg.CSIIOP.CompoundSecMechList;
import org.omg.CSIIOP.CompoundSecMechListHelper;
import org.omg.CSIIOP.Confidentiality;
import org.omg.CSIIOP.DelegationByClient;
import org.omg.CSIIOP.DetectMisordering;
import org.omg.CSIIOP.DetectReplay;
import org.omg.CSIIOP.EstablishTrustInClient;
import org.omg.CSIIOP.EstablishTrustInTarget;
import org.omg.CSIIOP.IdentityAssertion;
import org.omg.CSIIOP.Integrity;
import org.omg.CSIIOP.NoDelegation;
import org.omg.CSIIOP.NoProtection;
import org.omg.CSIIOP.SECIOP_SEC_TRANS;
import org.omg.CSIIOP.SECIOP_SEC_TRANSHelper;
import org.omg.CSIIOP.ServiceConfiguration;
import org.omg.CSIIOP.SimpleDelegation;
import org.omg.CSIIOP.TAG_NULL_TAG;
import org.omg.CSIIOP.TAG_SECIOP_SEC_TRANS;
import org.omg.CSIIOP.TAG_TLS_SEC_TRANS;
import org.omg.CSIIOP.TLS_SEC_TRANS;
import org.omg.CSIIOP.TLS_SEC_TRANSHelper;
import org.omg.CSIIOP.TransportAddress;
import org.omg.CosTSInteroperation.TAG_INV_POLICY;
import org.omg.IOP.TAG_ALTERNATE_IIOP_ADDRESS;
import org.omg.IOP.TAG_ASSOCIATION_OPTIONS;
import org.omg.IOP.TAG_CODE_SETS;
import org.omg.IOP.TAG_COMPLETE_OBJECT_KEY;
import org.omg.IOP.TAG_CSI_ECMA_Hybrid_SEC_MECH;
import org.omg.IOP.TAG_CSI_ECMA_Public_SEC_MECH;
import org.omg.IOP.TAG_CSI_ECMA_Secret_SEC_MECH;
import org.omg.IOP.TAG_CSI_SEC_MECH_LIST;
import org.omg.IOP.TAG_DCE_BINDING_NAME;
import org.omg.IOP.TAG_DCE_NO_PIPES;
import org.omg.IOP.TAG_DCE_SEC_MECH;
import org.omg.IOP.TAG_DCE_STRING_BINDING;
import org.omg.IOP.TAG_ENDPOINT_ID_POSITION;
import org.omg.IOP.TAG_GENERIC_SEC_MECH;
import org.omg.IOP.TAG_JAVA_CODEBASE;
import org.omg.IOP.TAG_KerberosV5_SEC_MECH;
import org.omg.IOP.TAG_LOCATION_POLICY;
import org.omg.IOP.TAG_ORB_TYPE;
import org.omg.IOP.TAG_OTS_POLICY;
import org.omg.IOP.TAG_POLICIES;
import org.omg.IOP.TAG_SEC_NAME;
import org.omg.IOP.TAG_SPKM_1_SEC_MECH;
import org.omg.IOP.TAG_SPKM_2_SEC_MECH;
import org.omg.IOP.TAG_SSL_SEC_TRANS;
import org.omg.IOP.TaggedComponent;

import static java.lang.Integer.toHexString;
import static org.apache.yoko.util.Hex.formatHexLine;
import static org.apache.yoko.util.Hex.formatHexPara;

import java.util.function.BiConsumer;

public final class IORUtil {
    private static void describeCSISecMechList(TaggedComponent component, StringBuilder sb) {
        YokoInputStream in = new YokoInputStream(component.component_data);
        in._OB_readEndian();
        CompoundSecMechList info = CompoundSecMechListHelper.read(in);
        
        sb.append("CSI Security Mechanism List Components:\n"); 
        sb.append("    stateful: " + info.stateful + "\n"); 
        sb.append("    mechanism_list:\n"); 
        for (CompoundSecMech mech: info.mechanism_list) {
            sb.append("        target_requires: "); describeTransportFlags(mech.target_requires, sb); sb.append("\n"); 
            if (mech.transport_mech != null) {
                if (mech.transport_mech.tag == TAG_NULL_TAG.value) {
                    sb.append("            Null Transport\n"); 
                } else if (mech.transport_mech.tag == TAG_TLS_SEC_TRANS.value) {
                    describeTLS_SEC_TRANS(mech.transport_mech, sb); 
                } else if (mech.transport_mech.tag == TAG_SECIOP_SEC_TRANS.value) {
                    describeSECIOP_SEC_TRANS(mech.transport_mech, sb); 
                }
            }
            
            if (mech.as_context_mech != null) {
                sb.append("            as_context_mech:\n"); 
                sb.append("                supports: "); describeTransportFlags(mech.as_context_mech.target_supports, sb); sb.append("\n"); 
                sb.append("                requires: "); describeTransportFlags(mech.as_context_mech.target_requires, sb); sb.append("\n"); 
                sb.append("                client_authentication_mech: "); formatHexLine(mech.as_context_mech.client_authentication_mech, sb); sb.append("\n");
                sb.append("                target_name: "); formatHexLine(mech.as_context_mech.target_name, sb); sb.append("\n");
            }
            
            if (mech.sas_context_mech != null) {
                sb.append("            sas_context_mech:\n"); 
                sb.append("                supports: "); describeTransportFlags(mech.sas_context_mech.target_supports, sb); sb.append("\n"); 
                sb.append("                requires: "); describeTransportFlags(mech.sas_context_mech.target_requires, sb); sb.append("\n"); 
                sb.append("                privilege_authorities:\n");
                for (ServiceConfiguration auth: mech.sas_context_mech.privilege_authorities) {
                    sb.append("                    syntax: " + auth.syntax + "\n"); 
                    sb.append("                    name: "); formatHexLine(auth.name, sb); sb.append("\n");
                }
                sb.append("                supported_naming_mechanisms:\n");
                for (byte[] namingMech: mech.sas_context_mech.supported_naming_mechanisms) {
                    sb.append("                    "); formatHexLine(namingMech, sb); sb.append("\n");
                }
                sb.append("                supported_identity_type: "); describeIdentityToken(mech.sas_context_mech.supported_identity_types, sb); sb.append("\n");
            }
        }
    }

    private static void describeTransportFlags(int flag, StringBuilder sb) {
        if ((NoProtection.value & flag) != 0) sb.append("NoProtection ");
        if ((Integrity.value & flag) != 0) sb.append("Integrity ");
        if ((Confidentiality.value & flag) != 0) sb.append("Confidentiality ");
        if ((DetectReplay.value & flag) != 0) sb.append("DetectReplay ");
        if ((DetectMisordering.value & flag) != 0) sb.append("DetectMisordering ");
        if ((EstablishTrustInTarget.value & flag) != 0) sb.append("EstablishTrustInTarget ");
        if ((EstablishTrustInClient.value & flag) != 0) sb.append("EstablishTrustInClient ");
        if ((NoDelegation.value & flag) != 0) sb.append("NoDelegation ");
        if ((SimpleDelegation.value & flag) != 0) sb.append("SimpleDelegation ");
        if ((CompositeDelegation.value & flag) != 0) sb.append("CompositeDelegation ");
        if ((IdentityAssertion.value & flag) != 0) sb.append("IdentityAssertion ");
        if ((DelegationByClient.value & flag) != 0) sb.append("DelegationByClient ");
    }

    private static void describeIdentityToken(int flag, StringBuilder sb) {
        if (flag == ITTAbsent.value) {
            sb.append("Absent"); 
            return;
        }
        if ((ITTAnonymous.value & flag) != 0) sb.append("Anonymous ");
        if ((ITTPrincipalName.value & flag) != 0) sb.append("PrincipalName ");
        if ((ITTX509CertChain.value & flag) != 0) sb.append("X509CertChain ");
        if ((ITTDistinguishedName.value & flag) != 0) sb.append("DistinguishedName ");
    }

    private static void describeTLS_SEC_TRANS(TaggedComponent component, StringBuilder sb) {
        YokoInputStream in = new YokoInputStream(component.component_data);
        in._OB_readEndian();
        TLS_SEC_TRANS info = TLS_SEC_TRANSHelper.read(in);
        sb.append("        TLS_SEC_TRANS component:\n"); 
        sb.append("            target_supports: "); describeTransportFlags(info.target_supports, sb); sb.append("\n"); 
        sb.append("            target_requires: "); describeTransportFlags(info.target_requires, sb); sb.append("\n"); 
        sb.append("            addresses:\n"); 
        for (TransportAddress address: info.addresses) {
            sb.append("                host_name: ").append(address.host_name).append("\n"); 
            sb.append("                port: ").append(address.port).append("\n"); 
        }
    }

    private static void describeSECIOP_SEC_TRANS(TaggedComponent component, StringBuilder sb) {
        YokoInputStream in = new YokoInputStream(component.component_data);
        in._OB_readEndian();
        SECIOP_SEC_TRANS info = SECIOP_SEC_TRANSHelper.read(in);
        sb.append("        SECIOP_SEC_TRANS component:\n"); 
        sb.append("            target_supports: "); describeTransportFlags(info.target_supports, sb); sb.append("\n"); 
        sb.append("            target_requires: "); describeTransportFlags(info.target_requires, sb); sb.append("\n"); 
        sb.append("            mech_oid: "); formatHexLine(info.mech_oid, sb); sb.append("\n");
        sb.append("            target_name: "); formatHexLine(info.target_name, sb); sb.append("\n");
        sb.append("            addresses:\n"); 
        for (TransportAddress address: info.addresses) {
            sb.append("                host_name: ").append(address.host_name).append("\n"); 
            sb.append("                port: ").append(address.port).append("\n"); 
        }
    }

    private static void describeCodeSets(TaggedComponent component, StringBuilder sb) {
        YokoInputStream in = new YokoInputStream(component.component_data);
        in._OB_readEndian();
        CodeSetComponentInfo info = CodeSetComponentInfoHelper.read(in);
        CodeSetInfo charInfo;

        sb.append("Native char codeset: \n");
        charInfo = CodeSetInfo.forRegistryId(info.ForCharData.native_code_set);
        if (charInfo != null)
            sb.append("  \"").append(charInfo.description).append("\"\n");
        else if (info.ForCharData.native_code_set == 0)
            sb.append("  [No codeset information]\n");
        else
            sb.append("  [Unknown codeset id: ").append(info.ForCharData.native_code_set).append("]\n");

        for (int i = 0; i < info.ForCharData.conversion_code_sets.length; i++) {
            charInfo = CodeSetInfo.forRegistryId(info.ForCharData.conversion_code_sets[i]);
            if (i == 0)
                sb.append("Char conversion codesets:\n");
            if (charInfo != null)
                sb.append("  \"").append(charInfo.description).append("\"\n");
            else
                sb.append("  [Unknown codeset id: ").append(info.ForCharData.conversion_code_sets[i]).append("]\n");
        }

        //
        // Print wchar codeset information
        //
        sb.append("Native wchar codeset: \n");
        charInfo = CodeSetInfo.forRegistryId(info.ForWcharData.native_code_set);
        if (charInfo != null)
            sb.append("  \"").append(charInfo.description).append("\"\n");
        else if (info.ForWcharData.native_code_set == 0)
            sb.append("  [No codeset information]\n");
        else
            sb.append("  [Unknown codeset id: ").append(info.ForWcharData.native_code_set).append("]\n");

        for (int i = 0; i < info.ForWcharData.conversion_code_sets.length; i++) {
            if (i == 0)
                sb.append("Wchar conversion codesets:\n");

            charInfo = CodeSetInfo
                    .forRegistryId(info.ForWcharData.conversion_code_sets[i]);
            if (charInfo != null)
                sb.append("  \"").append(charInfo.description).append("\"\n");
            else
                sb.append("  [Unknown codeset id: ").append(info.ForWcharData.conversion_code_sets[i]).append("]\n");
        }
    }

    private static void describeGenericComponent(TaggedComponent component, String name, StringBuilder sb) {
        sb.append("Component: ").append(name).append('\n');
        sb.append("Component data: (").append(component.component_data.length).append(")\n");
        formatHexPara(component.component_data, 0, component.component_data.length, sb).append('\n');
    }

    private static void describeJavaCodebase(TaggedComponent component, StringBuilder sb) {
        YokoInputStream in = new YokoInputStream(component.component_data);
        in._OB_readEndian();
        String codebase = in.read_string();
        sb.append("Component: TAG_JAVA_CODEBASE = '").append(codebase).append("'\n");
    }

    private static void describeAlternateIiop(TaggedComponent component, StringBuilder sb) {
        YokoInputStream in = new YokoInputStream(component.component_data);
        in._OB_readEndian();
        String host = in.read_string();
        int port = 0xFFFF & in.read_ushort();
        sb.append("Alternate IIOP address:\n");
        sb.append("  host: ").append(host).append('\n');
        sb.append("  port: ").append(port).append('\n');
    }

    private static void describeOrbType(TaggedComponent component, StringBuilder sb) {
        YokoInputStream in = new YokoInputStream(component.component_data);
        in._OB_readEndian();
        sb.append("Component: TAG_ORB_TYPE = 0x").append(toHexString(in.read_ulong())).append('\n');
    }

    private static void describeUnknown(TaggedComponent component, StringBuilder sb) {
        describeGenericComponent(component, String.format("unknown (tag = 0x%x)", component.tag), sb);
    }

    private enum TagComp {
        _TAG_POLICIES,
        _TAG_ASSOCIATION_OPTIONS,
        _TAG_SEC_NAME,
        _TAG_SPKM_1_SEC_MECH,
        _TAG_SPKM_2_SEC_MECH,
        _TAG_KerberosV5_SEC_MECH,
        _TAG_CSI_ECMA_Secret_SEC_MECH,
        _TAG_CSI_ECMA_Hybrid_SEC_MECH,
        _TAG_OTS_POLICY,
        _TAG_INV_POLICY,
        _TAG_SECIOP_SEC_TRANS,
        _TAG_NULL_TAG,
        _TAG_TLS_SEC_TRANS,
        _TAG_SSL_SEC_TRANS,
        _TAG_CSI_ECMA_Public_SEC_MECH,
        _TAG_GENERIC_SEC_MECH,
        _TAG_COMPLETE_OBJECT_KEY,
        _TAG_ENDPOINT_ID_POSITION,
        _TAG_LOCATION_POLICY,
        _TAG_DCE_STRING_BINDING,
        _TAG_DCE_BINDING_NAME,
        _TAG_DCE_NO_PIPES,
        _TAG_DCE_SEC_MECH,
        _TAG_CODE_SETS(IORUtil::describeCodeSets),
        _TAG_ORB_TYPE(IORUtil::describeOrbType),
        _TAG_ALTERNATE_IIOP_ADDRESS(IORUtil::describeAlternateIiop),
        _TAG_CSI_SEC_MECH_LIST(IORUtil::describeCSISecMechList),
        _TAG_JAVA_CODEBASE(IORUtil::describeJavaCodebase),
        _UNKNOWN(IORUtil::describeUnknown);
        final BiConsumer<TaggedComponent, StringBuilder> describer;
        TagComp(BiConsumer<TaggedComponent, StringBuilder> describer) { this.describer = describer; }
        TagComp() { this(null); }
        void describe(TaggedComponent component, StringBuilder sb) {
            if (null == describer) describeGenericComponent(component, this.toString(), sb);
            else describer.accept(component, sb);
        }
        @Override
        public String toString() {
            return super.toString().substring(1);
        }
        
        static TagComp findById(int id) {
            switch (id) {
                case TAG_POLICIES.value: return _TAG_POLICIES;
                case TAG_ASSOCIATION_OPTIONS.value: return _TAG_ASSOCIATION_OPTIONS;
                case TAG_SEC_NAME.value: return _TAG_SEC_NAME;
                case TAG_SPKM_1_SEC_MECH.value: return _TAG_SPKM_1_SEC_MECH;
                case TAG_SPKM_2_SEC_MECH.value: return _TAG_SPKM_2_SEC_MECH;
                case TAG_KerberosV5_SEC_MECH.value: return _TAG_KerberosV5_SEC_MECH;
                case TAG_CSI_ECMA_Secret_SEC_MECH.value: return _TAG_CSI_ECMA_Secret_SEC_MECH;
                case TAG_CSI_ECMA_Hybrid_SEC_MECH.value: return _TAG_CSI_ECMA_Hybrid_SEC_MECH;
                case TAG_OTS_POLICY.value: return _TAG_OTS_POLICY;
                case TAG_INV_POLICY.value: return _TAG_INV_POLICY;
                case TAG_SECIOP_SEC_TRANS.value: return _TAG_SECIOP_SEC_TRANS;
                case TAG_NULL_TAG.value: return _TAG_NULL_TAG;
                case TAG_TLS_SEC_TRANS.value: return _TAG_TLS_SEC_TRANS;
                case TAG_SSL_SEC_TRANS.value: return _TAG_SSL_SEC_TRANS;
                case TAG_CSI_ECMA_Public_SEC_MECH.value: return _TAG_CSI_ECMA_Public_SEC_MECH;
                case TAG_GENERIC_SEC_MECH.value: return _TAG_GENERIC_SEC_MECH;
                case TAG_COMPLETE_OBJECT_KEY.value: return _TAG_COMPLETE_OBJECT_KEY;
                case TAG_ENDPOINT_ID_POSITION.value: return _TAG_ENDPOINT_ID_POSITION;
                case TAG_LOCATION_POLICY.value: return _TAG_LOCATION_POLICY;
                case TAG_DCE_STRING_BINDING.value: return _TAG_DCE_STRING_BINDING;
                case TAG_DCE_BINDING_NAME.value: return _TAG_DCE_BINDING_NAME;
                case TAG_DCE_NO_PIPES.value: return _TAG_DCE_NO_PIPES;
                case TAG_DCE_SEC_MECH.value: return _TAG_DCE_SEC_MECH;
                case TAG_CODE_SETS.value: return _TAG_CODE_SETS;
                case TAG_ORB_TYPE.value: return _TAG_ORB_TYPE;
                case TAG_ALTERNATE_IIOP_ADDRESS.value: return _TAG_ALTERNATE_IIOP_ADDRESS;
                case TAG_CSI_SEC_MECH_LIST.value: return _TAG_CSI_SEC_MECH_LIST;
                case TAG_JAVA_CODEBASE.value: return _TAG_JAVA_CODEBASE;
                default: return _UNKNOWN;
            }
        }
    }

    public static void describe_component(TaggedComponent component, StringBuilder sb) {
        TagComp.findById(component.tag).describe(component, sb);
    }
}

