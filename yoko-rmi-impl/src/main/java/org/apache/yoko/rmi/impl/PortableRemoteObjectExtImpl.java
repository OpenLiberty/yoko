/*==============================================================================
 * Copyright 2021 IBM Corporation and others.
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
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
 *=============================================================================*/
package org.apache.yoko.rmi.impl;

import org.apache.yoko.rmi.api.PortableRemoteObjectExtDelegate;
import org.apache.yoko.util.PrivilegedActions;
import org.omg.CORBA.ORB;

import java.security.AccessController;
import java.util.WeakHashMap;

import static java.security.AccessController.doPrivileged;
import static org.apache.yoko.util.PrivilegedActions.GET_CONTEXT_CLASS_LOADER;
import static org.apache.yoko.util.PrivilegedActions.GET_SYSPROPS;

public final class PortableRemoteObjectExtImpl implements PortableRemoteObjectExtDelegate {
    private enum Holder {
        ;
        private static final ORB DEFAULT_ORB = ORB.init(new String[0], doPrivileged(GET_SYSPROPS));
    }

    private static int nextId = 0;
    private static final WeakHashMap<ClassLoader, RMIState> statePerLoader = new WeakHashMap<>();
    private static final RMIState nullLoaderRMIState = createRmiState();

    private static RMIState createRmiState() {
        return new RMIState(Holder.DEFAULT_ORB, "rmi" + nextId++);
    }

    public RMIState getCurrentState() {
        ClassLoader loader = doPrivileged(GET_CONTEXT_CLASS_LOADER);
        if (null == loader) return nullLoaderRMIState;
        synchronized (statePerLoader) {
            RMIState result = statePerLoader.get(loader);
            if (null != result) return result;
            result = createRmiState();
            statePerLoader.put(loader, result);
            return result;
        }
    }
}

