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
package org.apache.yoko.rmi.util;

import org.apache.yoko.rmi.impl.InputStreamWithOffsets;
import org.omg.CORBA.portable.InputStream;

import java.io.InvalidClassException;
import java.security.PrivilegedExceptionAction;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.security.AccessController.doPrivileged;

/**
 * This is a helper class to access the JDK's serial filter.
 * <ul>
 *     <li>In Java 7, there isn't an implementation at all.</li>
 *     <li>In Java 8, there is a sun.misc implementation.</li>
 *     <li>In Java 9 onwards, there is a java.io implementation.</li>
 * </ul>
 *
 * This utility class hides all of the details and makes the filter object, if any, callable via its one public method.
 */
public enum SerialFilterHelper {
    ;
    private static final Logger logger = Logger.getLogger(SerialFilterHelper.class.getName());
    private static final FilterAdapter HELPER;

    static {
        FilterAdapter filterAdapter;
        try {
            filterAdapter = new JavaIoFilterAdapter();
        } catch (Throwable t1) {
            try {
                filterAdapter = doPrivileged((PrivilegedExceptionAction<FilterAdapter>)SunMiscFilterAdapter::new);
            } catch (Throwable t2) {
                filterAdapter = new NullFilterAdapter();
            }
        }
        HELPER = filterAdapter;
    }

    public static void checkInput(Class<?> serialClass, long arrayLength, long depth, long position) {
        try {
            HELPER.checkInput(serialClass, arrayLength, depth, position);
        } catch (InvalidClassException ice) {
            throw new RuntimeException(ice);
        }
    }

    private static long position(InputStream in) { return (in instanceof InputStreamWithOffsets) ? ((InputStreamWithOffsets) in).position() : 0; }

    public static void checkArrayInput(Class<?> type, long arraylength, InputStream in) {
        // depth previously checked for this object from ValueHandlerImpl.readValue0
        checkInput(type, arraylength, 1, position(in));
    }

    public static void checkInput(Class<?> type, long depth, InputStream in) { checkInput(type, -1, depth, position(in)); }

    private abstract static class FilterAdapter {
        {
            if (logger.isLoggable(Level.FINER)) logger.finer(this.getClass().getName() + " created");
        }
        abstract void checkInput(Class<?> serialClass, long arrayLength, long depth, long streamBytes) throws InvalidClassException;
    }

    private static final class NullFilterAdapter extends FilterAdapter {
        @Override
        public void checkInput(Class<?> serialClass, long arrayLength, long depth, long streamBytes) {}
    }

    abstract static class BaseFilterAdapter<FILTER, INFO extends BaseInfo, STATUS extends Enum<STATUS>> extends FilterAdapter {
        final FILTER filter;
        private final STATUS rejected;

        BaseFilterAdapter(FILTER filter, STATUS rejected) {
            Objects.requireNonNull(filter);
            Objects.requireNonNull(rejected);
            this.filter = filter;
            this.rejected = rejected;
        }

        @Override
        public final void checkInput(Class<?> serialClass, long arrayLength, long depth, long streamBytes) throws InvalidClassException {
            final long references = 1; // we cannot know how many other references there will be later in the stream
            if (logger.isLoggable(Level.FINER)) {
                logger.finer(String.format("Invoking SerialFilter for Class : %s=\"%s\", %s=%d, %s=%d, %s=%d, %s=%d",
                        "serialClass", serialClass,
                        "arrayLength", arrayLength,
                        "depth", depth,
                        "references", references,
                        "streamBytes", streamBytes));
            }

            INFO info = makeInfo(serialClass, arrayLength, depth, references, streamBytes);
            STATUS status;
            try {
                status = checkInput(info);
            } catch (Exception e) {
                throw (InvalidClassException)new InvalidClassException("Rejected by serialFilter: " + serialClass.getName()).initCause(e);
            }
            if (status == null || status == rejected) {
                throw new InvalidClassException("Rejected by serialFilter: " + serialClass.getName());
            }
        }

        abstract STATUS checkInput(INFO info);
        @SuppressWarnings("SameParameterValue")
        abstract INFO makeInfo(Class<?> serialClass, long arrayLength, long depth, long references, long streamBytes);

    }

    static abstract class BaseInfo {
        private final Class<?> serialClass;
        private final long arrayLength;
        private final long depth;
        private final long references;
        private final long streamBytes;

        public BaseInfo(Class<?> serialClass, long arrayLength, long depth, long references, long streamBytes) {
            this.serialClass = serialClass;
            this.arrayLength = arrayLength;
            this.depth = depth;
            this.references = references;
            this.streamBytes = streamBytes;
        }

        public Class<?> serialClass() { return serialClass; }
        public long arrayLength() { return arrayLength; }
        public long depth() { return depth; }
        public long references() { return references; }
        public long streamBytes() { return streamBytes; }
    }
}
