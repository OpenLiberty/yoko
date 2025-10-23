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
package org.apache.yoko.orb.OBMessaging;

import org.apache.yoko.orb.CORBA.AnyImpl;
import org.apache.yoko.osgi.ProviderLocator;
import org.apache.yoko.util.Assert;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.UserException;
import org.omg.Messaging._ExceptionHolder;
import org.omg.CORBA.Any;

import java.lang.reflect.InvocationTargetException;

import static java.security.AccessController.doPrivileged;
import static org.apache.yoko.util.PrivilegedActions.GET_CONTEXT_CLASS_LOADER;

public class UserExceptionRaiseProxy {
    public void raise(_ExceptionHolder execptHolder)
            throws UserException {
    }

    public void raise_with_list(
            _ExceptionHolder exceptHolder,
            TypeCode[] exceptList)
            throws UserException {
        try {
            raise(exceptHolder);
        } catch (UserException ex) {
            Any any = new AnyImpl();

            Class exClass = ex.getClass();
            String className = exClass.getName();
            try {
                //
                // Get the helper class and the insert method with
                // appropriate parameter types
                //
                // get the appropriate class for the loading.
                Class c = ProviderLocator.loadClass(className + "Helper", exClass, doPrivileged(GET_CONTEXT_CLASS_LOADER));
                Class[] paramTypes = new Class[2];
                paramTypes[0] = Any.class;
                paramTypes[1] = exClass;
                java.lang.reflect.Method m = c.getMethod("insert", paramTypes);

                //
                // Build up the parameter list
                //
                Object[] parameters = new Object[2];
                parameters[0] = any;
                parameters[1] = ex;

                //
                // No object is needed since this is a static method
                // call
                //
                m.invoke(null, parameters);
            } catch (ClassNotFoundException e) {
                //
                // REVISIT:
                // This just means that we probably caught a non-CORBA
                // exception. For now, we'll just throw this again.
                //
                // throw e;
            } catch (NoSuchMethodException|IllegalAccessException|IllegalArgumentException|InvocationTargetException e) {
                throw Assert.fail(ex);
            } catch (SecurityException e) {
                //
                // REVISIT:
                // What do we do here?
                //
            }

            for (int i = 0; i < exceptList.length; ++i) {
                if (any.type().equal(exceptList[i]))
                    throw ex;
            }
        }
    }

    public void register_as_proxy_with(
            _ExceptionHolder exceptHolder) {

        ExceptionHolder_impl exImpl = (ExceptionHolder_impl) exceptHolder;

        exImpl._OB_register_raise_proxy(this);
        //
        // TODO: try/catch block???
        //
        /*
         * org.apache.yoko.orb.OBMessaging.ExceptionHolder_impl exImpl =
         * (org.apache.yoko.orb.OBMessaging.ExceptionHolder_impl)exceptHolder;
         *
         * exImpl._OB_register_raise_proxy(this);
         */
    }
}
