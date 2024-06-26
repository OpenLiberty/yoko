/*
 * Copyright 2010 IBM Corporation and others.
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
package org.apache.yoko.rmi.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author Jeppe Sommer (jso@eos.dk)
 */
public class StreamUtil {
    final static Logger logger = Logger.getLogger(StreamUtil.class.getName());

    static final int BUF_SIZE = 4096;

    public static void copyStream(InputStream is, OutputStream os)
            throws IOException {

        byte[] buf = new byte[BUF_SIZE];

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("copyStream(" + is + ", " + os);
        }

        try {
            int count;

            while ((count = is.read(buf)) != -1) {
                // log.debug("copyStream, copying " + count + " bytes");
                os.write(buf, 0, count);
            }

        } finally {
        }
    }

    public static void copyStream(Reader reader, Writer writer)
            throws IOException {

        char[] buf = new char[BUF_SIZE];

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("copyStream(" + reader + ", " + writer);
        }

        try {
            int count;

            while ((count = reader.read(buf)) != -1) {
                // log.debug("copyStream, copying " + count + " bytes");
                writer.write(buf, 0, count);
            }

        } finally {
        }
    }
}
