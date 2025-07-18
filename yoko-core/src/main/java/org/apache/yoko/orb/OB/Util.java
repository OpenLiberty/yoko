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

import org.apache.yoko.orb.IOP.ServiceContexts;
import org.apache.yoko.orb.exceptions.Transients;
import org.apache.yoko.osgi.ProviderLocator;
import org.apache.yoko.util.Assert;
import org.apache.yoko.util.MinorCodes;
import org.apache.yoko.util.TriFunction;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_CONTEXT;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.BAD_QOS;
import org.omg.CORBA.BAD_TYPECODE;
import org.omg.CORBA.CODESET_INCOMPATIBLE;
import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.DATA_CONVERSION;
import org.omg.CORBA.FREE_MEM;
import org.omg.CORBA.IMP_LIMIT;
import org.omg.CORBA.INITIALIZE;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.INTF_REPOS;
import org.omg.CORBA.INVALID_TRANSACTION;
import org.omg.CORBA.INV_FLAG;
import org.omg.CORBA.INV_IDENT;
import org.omg.CORBA.INV_OBJREF;
import org.omg.CORBA.INV_POLICY;
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.NO_MEMORY;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.NO_RESOURCES;
import org.omg.CORBA.NO_RESPONSE;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.OBJ_ADAPTER;
import org.omg.CORBA.PERSIST_STORE;
import org.omg.CORBA.REBIND;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.TIMEOUT;
import org.omg.CORBA.TRANSACTION_MODE;
import org.omg.CORBA.TRANSACTION_REQUIRED;
import org.omg.CORBA.TRANSACTION_ROLLEDBACK;
import org.omg.CORBA.TRANSACTION_UNAVAILABLE;
import org.omg.CORBA.UNKNOWN;
import org.omg.CORBA.UserException;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;
import org.omg.IOP.SendingContextRunTime;
import org.omg.IOP.ServiceContext;
import org.omg.SendingContext.CodeBase;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.security.AccessController.doPrivileged;
import static java.util.logging.Logger.getLogger;
import static org.apache.yoko.util.Collectors.toUnmodifiableMap;
import static org.apache.yoko.util.PrivilegedActions.GET_CONTEXT_CLASS_LOADER;

public final class Util {
    static final Logger logger = getLogger(Util.class.getName());
    // Print octets to stream
    public static void printOctets(PrintStream out, byte[] oct, int offset, int length) {
        final int inc = 8;

        for (int i = offset; i < offset + length; i += inc) {
            for (int j = i; j - i < inc; j++) {
                if (j < offset + length) {
                    int n = (int) oct[j];
                    if (n < 0)
                        n += 256;
                    String s;
                    if (n < 10)
                        s = "  " + n;
                    else if (n < 100)
                        s = " " + n;
                    else
                        s = "" + n;
                    out.print(s + " ");
                } else
                    out.print("    ");
            }

            out.print('"');

            for (int j = i; j < offset + length && j - i < inc; j++) {
                if (oct[j] >= (byte) 32 && oct[j] < (byte) 127)
                    out.print((char) oct[j]);
                else
                    out.print('.');
            }

            out.println('"');
        }
    }

    // Copy a system exception
    public static SystemException copy(SystemException ex) {
        SystemException result;
        try {
            Class c = ex.getClass();
            Class[] paramTypes = { String.class };
            Constructor constr = c.getConstructor(paramTypes);
            Object[] initArgs = { ex.getMessage() };
            result = (SystemException) constr.newInstance(initArgs);
            result.minor = ex.minor;
            result.completed = ex.completed;
            result.initCause(ex);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalArgumentException | IllegalAccessException | InstantiationException e) {
            throw Assert.fail(ex);
        }
        return result;
    }

    /**
     * Unmarshal a system exception.
     * Renamed to remove the words 'unmarshal' and 'exception'.
     * The old name misled people that something went wrong during unmarshalling.
     */
    public static SystemException readSysEx(InputStream in) {
        final String id = in.read_string();
        final int minor = in.read_ulong();
        final CompletionStatus status = CompletionStatus.from_int(in.read_ulong());
        return SysEx.fromId(id).factory.apply(minor, status);
    }

    // Marshal a system exception
    public static void marshalSystemException(OutputStream out, SystemException ex) {
        out.write_string(getExceptionId(ex));
        out.write_ulong(ex.minor);
        out.write_ulong(ex.completed.value());
    }

    enum SysEx {
        BAD_CONTEXT(BAD_CONTEXT::new),
        BAD_INV_ORDER(BAD_INV_ORDER::new, MinorCodes::describeBadInvOrder),
        BAD_OPERATION(BAD_OPERATION::new),
        BAD_PARAM(BAD_PARAM::new, MinorCodes::describeBadParam),
        BAD_QOS(BAD_QOS::new),
        BAD_TYPECODE(BAD_TYPECODE::new),
        CODESET_INCOMPATIBLE(CODESET_INCOMPATIBLE::new),
        COMM_FAILURE(COMM_FAILURE::new, MinorCodes::describeCommFailure),
        DATA_CONVERSION(DATA_CONVERSION::new),
        FREE_MEM(FREE_MEM::new),
        IMP_LIMIT(IMP_LIMIT::new, MinorCodes::describeImpLimit),
        INITIALIZE(INITIALIZE::new, MinorCodes::describeInitialize),
        INTERNAL(INTERNAL::new),
        INTF_REPOS(INTF_REPOS::new, MinorCodes::describeIntfRepos),
        INV_FLAG(INV_FLAG::new),
        INV_IDENT(INV_IDENT::new),
        INV_OBJREF(INV_OBJREF::new),
        INV_POLICY(INV_POLICY::new, MinorCodes::describeInvPolicy),
        INVALID_TRANSACTION(INVALID_TRANSACTION::new),
        MARSHAL(MARSHAL::new, MinorCodes::describeMarshal),
        NO_IMPLEMENT(NO_IMPLEMENT::new, MinorCodes::describeNoImplement),
        NO_MEMORY(NO_MEMORY::new, MinorCodes::describeNoMemory),
        NO_PERMISSION(NO_PERMISSION::new),
        NO_RESOURCES(NO_RESOURCES::new, MinorCodes::describeNoResources),
        NO_RESPONSE(NO_RESPONSE::new),
        OBJ_ADAPTER(OBJ_ADAPTER::new),
        OBJECT_NOT_EXIST(OBJECT_NOT_EXIST::new, MinorCodes::describeObjectNotExist),
        PERSIST_STORE(PERSIST_STORE::new),
        REBIND(REBIND::new),
        TIMEOUT(TIMEOUT::new),
        TRANSACTION_MODE(TRANSACTION_MODE::new),
        TRANSACTION_REQUIRED(TRANSACTION_REQUIRED::new),
        TRANSACTION_ROLLEDBACK(TRANSACTION_ROLLEDBACK::new),
        TRANSACTION_UNAVAILABLE(TRANSACTION_UNAVAILABLE::new),
        TRANSIENT(Transients::create),
        UNKNOWN(UNKNOWN::new, MinorCodes::describeUnknown);
        public static final String ID_PREFIX = "IDL:omg.org/CORBA/";
        public static final String ID_SUFFIX = ":1.0";
        final String id = ID_PREFIX + name() + ID_SUFFIX;
        private static final Map<String, SysEx> INDEX = Stream.of(values()).collect(toUnmodifiableMap(HashMap::new, se -> se.id));

        private interface Constructor extends BiFunction<Integer, CompletionStatus, SystemException>{}
        private interface ReasonConstructor extends TriFunction<String, Integer, CompletionStatus, SystemException>{
            default SysEx.Constructor addDescriber(ReasonDescriber describer) {
                return (minorCode, completionStatus) -> this.apply(describer.describe(minorCode), minorCode, completionStatus);
            }
        }
        private interface ReasonDescriber { String describe(int minorCode); }
        final Constructor factory;

        SysEx(Constructor factory) { this.factory = factory; }
        SysEx(ReasonConstructor factory, ReasonDescriber desc) { this(factory.addDescriber(desc)); }

        static SysEx valueOf(SystemException e) {
            try {
                return valueOf(e.getClass().getSimpleName());
            } catch (Exception t) {
                return UNKNOWN;
            }
        }

        static SysEx fromId(String id) {
            SysEx result = Optional.of(id).map(INDEX::get).orElse(UNKNOWN);
            if (!result.id.equals(id)) logger.warning("Using " + result + " for unrecognised system exception id: " + id);
            return result;
        }

        static boolean isValidId(String id) {return INDEX.containsKey(id);}
    }

    /** Determine if the repository ID represents a system exception */
    public static boolean isSystemException(String id) {
        return SysEx.isValidId(id);
    }

    /** Determine the repository ID of an exception */
    public static String getExceptionId(Exception ex) {
        if (ex instanceof SystemException) {
            return SysEx.valueOf((SystemException) ex).id;
        } else if (ex instanceof UserException) {
            Class exClass = ex.getClass();
            String className = exClass.getName();
            String id = null;
            try {

                Class c = ProviderLocator.loadClass(className + "Helper", exClass, null);
                Method m = c.getMethod("id");
                id = (String) m.invoke(null, new Object[0]);
            } catch (ClassNotFoundException | SecurityException ignored) {
            } catch (NoSuchMethodException | InvocationTargetException | IllegalArgumentException | IllegalAccessException e) {
                throw Assert.fail(ex);
            }

            //
            // TODO: Is this correct?
            //
            if (id == null)
                return "IDL:omg.org/CORBA/UserException:1.0";
            else
                return id;
        } else {
            throw Assert.fail(ex);
        }
    }

    public static Any insertException(Any any, Exception ex) {
        // Find the helper class for the exception and use it to insert the exception into the any
        try {
            Class exClass = ex.getClass();
            String helper = exClass.getName() + "Helper";
            // get the appropriate class for the loading.
            Class c = ProviderLocator.loadClass(helper, exClass, doPrivileged(GET_CONTEXT_CLASS_LOADER));
            final Class[] paramTypes = { Any.class, exClass };
            Method m = c.getMethod("insert", paramTypes);
            final Object[] args = { any, ex };
            m.invoke(null, args);
        } catch (ClassNotFoundException | SecurityException ignored) {
        } catch (NoSuchMethodException | InvocationTargetException | IllegalArgumentException | IllegalAccessException e) {
            throw Assert.fail(ex);
        }
        return any;
    }

    public static CodeBase getSendingContextRuntime(ORBInstance orbInstance_, ServiceContexts contexts) {
        ServiceContext serviceContext = contexts.get(SendingContextRunTime.value);
        return serviceContext == null ? null : new CodeBaseProxy(orbInstance_, serviceContext);
    }
}
