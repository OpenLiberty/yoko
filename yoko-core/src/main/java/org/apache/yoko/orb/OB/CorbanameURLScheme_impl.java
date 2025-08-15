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

import static org.apache.yoko.orb.OB.TypeCodeFactory.createPrimitiveTC;
import static org.apache.yoko.orb.OB.TypeCodeFactory.createSequenceTC;
import static org.apache.yoko.orb.OB.TypeCodeFactory.createStringTC;
import static org.apache.yoko.orb.OB.TypeCodeFactory.createStructTC;
import static org.apache.yoko.orb.OB.URLUtil.unescapeURL;
import static org.apache.yoko.util.MinorCodes.MinorBadAddress;
import static org.apache.yoko.util.MinorCodes.MinorBadSchemeSpecificPart;
import static org.apache.yoko.util.MinorCodes.MinorOther;
import static org.apache.yoko.util.MinorCodes.describeBadParam;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;
import static org.omg.CORBA.TCKind.tk_objref;

import org.apache.yoko.util.Assert;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Request;
import org.omg.CORBA.StructMember;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.UserException;
import org.omg.DynamicAny.DynAny;
import org.omg.DynamicAny.DynAnyFactory;
import org.omg.DynamicAny.DynAnyFactoryHelper;
import org.omg.DynamicAny.DynSequence;
import org.omg.DynamicAny.DynSequenceHelper;
import org.omg.DynamicAny.DynStruct;
import org.omg.DynamicAny.DynStructHelper;

public class CorbanameURLScheme_impl extends LocalObject implements URLScheme {
    private ORB orb_;

    private CorbalocURLScheme corbaloc_;

    // ------------------------------------------------------------------
    // CorbanameURLScheme_impl constructor
    // ------------------------------------------------------------------

    public CorbanameURLScheme_impl(ORB orb, URLRegistry registry) {
        orb_ = orb;
        URLScheme scheme = registry.find_scheme("corbaloc");
        Assert.ensure(scheme != null);
        corbaloc_ = CorbalocURLSchemeHelper.narrow(scheme);
        Assert.ensure(corbaloc_ != null);
    }

    // ------------------------------------------------------------------
    // Standard IDL to Java Mapping
    // ------------------------------------------------------------------

    public String name() {
        return "corbaname";
    }

    public org.omg.CORBA.Object parse_url(String url) {
        //
        // Get the object key
        //
        String keyStr;
        int slash = url.indexOf('/');
        int fragmentStart = url.indexOf('#');
        if (slash != -1 && fragmentStart == -1) {
            //
            // e.g., corbaname::localhost:5000/blah
            //
            keyStr = url.substring(slash + 1);
        } else if (slash == -1 || fragmentStart - 1 == slash || fragmentStart < slash) {
            //
            // e.g., corbaname::localhost:5000
            // corbaname::localhost:5000/#foo
            // corbaname::localhost:5000#foo/bar
            //
            keyStr = "NameService";
        } else {
            keyStr = url.substring(slash + 1, fragmentStart);
        }

        //
        // Get start and end of protocol address(es)
        //
        int addrStart = 10; // skip "corbaname:"
        int addrEnd;

        if (addrStart == slash) throw new BAD_PARAM(describeBadParam(MinorBadAddress) + ": no protocol address", MinorBadAddress, COMPLETED_NO);

        if (slash == -1 && fragmentStart == -1)
            addrEnd = url.length() - 1;
        else if ((slash != -1 && fragmentStart == -1) || (slash != -1 && fragmentStart != -1 && slash < fragmentStart))
            addrEnd = slash - 1;
        else
            addrEnd = fragmentStart - 1;

        //
        // Create a corbaloc URL
        //
        String corbaloc = "corbaloc:" + url.substring(addrStart, addrEnd + 1) + "/" + keyStr;

        //
        // Create object reference from the naming context
        //
        org.omg.CORBA.Object nc = corbaloc_.parse_url(corbaloc);

        // 
        // If there is no URL fragment "#.....", or the stringified
        // name is empty, then the URL refers to the naming context
        // itself
        // 
        if (fragmentStart == -1 || url.substring(fragmentStart).length() == 0) return nc;

        //
        // Make a DII invocation on the Naming Service to resolve the
        // specified context
        //
        Exception failureCause;
        try {
            //
            // Create typecodes for Name and NameComponent
            //
            StructMember[] contents = new StructMember[2];
            contents[0] = new StructMember();
            contents[0].name = "id";
            contents[0].type = createStringTC(0);
            contents[1] = new StructMember();
            contents[1].name = "kind";
            contents[1].type = createStringTC(0);
            TypeCode tcNameComponent = createStructTC("IDL:omg.org/CosNaming/NameComponent:1.0", "NameComponent", contents);

            TypeCode tcName = createSequenceTC(0, tcNameComponent);

            //
            // Parse path (remove URL escapes first) and create
            // NameComponent sequence
            //
            String fragment = unescapeURL(url.substring(fragmentStart + 1));
            CORBANameParser parser = new CORBANameParser(fragment);
            if (!parser.isValid()) throw new BAD_PARAM(describeBadParam(MinorBadSchemeSpecificPart) + ": invalid stringified name \"" + fragment + "\"", MinorBadSchemeSpecificPart, COMPLETED_NO);

            String[] content = parser.getContents();
            Assert.ensure((content.length % 2) == 0);

            org.omg.CORBA.Object factoryObj = orb_.resolve_initial_references("DynAnyFactory");
            DynAnyFactory dynAnyFactory = DynAnyFactoryHelper.narrow(factoryObj);

            Any[] as = new Any[content.length / 2];
            for (int i = 0; i < content.length; i += 2) {
                //
                // Create the DynStruct containing the id and kind fields
                //
                DynAny dynAny = dynAnyFactory.create_dyn_any_from_type_code(tcNameComponent);
                DynStruct name = DynStructHelper.narrow(dynAny);
                name.insert_string(content[i]);
                name.next();
                name.insert_string(content[i + 1]);

                Any nany = name.to_any();
                name.destroy();

                as[i / 2] = nany;
            }

            //
            // Create the Name
            //
            DynAny dynAny = dynAnyFactory.create_dyn_any_from_type_code(tcName);
            DynSequence seq = DynSequenceHelper.narrow(dynAny);
            seq.set_length(as.length);
            seq.set_elements(as);
            Any any = seq.to_any();
            seq.destroy();

            //
            // Create the DII request
            //
            Request request = nc._request("resolve");

            //
            // Copy in the arguments
            //
            Any arg = request.add_in_arg();
            arg.read_value(any.create_input_stream(), any.type());

            request.set_return_type(createPrimitiveTC(tk_objref));

            //
            // Invoke the request
            //
            request.invoke();

            //
            // Return the result if there was no exception
            //
            failureCause = request.env().exception();
            if (failureCause == null) return request.return_value().extract_Object();
            
        } catch (SystemException ex) {
            failureCause = ex;
            // Fall through
        } catch (UserException ex) {
            failureCause = ex;
            // Fall through
        }

        final BAD_PARAM bp = new BAD_PARAM(describeBadParam(MinorOther) + ": corbaname evaluation error:" + failureCause.getMessage(), MinorOther, COMPLETED_NO);
        throw (BAD_PARAM)bp.initCause(failureCause);
    }

    public void destroy() {
        orb_ = null;
    }
}
