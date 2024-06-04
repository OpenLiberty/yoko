/*
 * Copyright 2023 IBM Corporation and others.
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
package test.common;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextPackage.InvalidName;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

public enum RefFiles {
    ;

    public static void writeRef(ORB orb, PrintWriter out, org.omg.CORBA.Object obj, NamingContextExt context, NameComponent[] name) throws InvalidName {
        out.println("ref:");
        out.println(orb.object_to_string(obj));
        out.println(context.to_string(name));
    }

    public static String[] readRef(BufferedReader reader, String[] refStrings) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new RuntimeException("Unknown Server error");
        } else if (!line.equals("ref:")) {
            try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
                pw.println("Server error:");
                do {
                    pw.print('\t');
                    pw.println(line);
                } while ((line = reader.readLine()) != null);
                pw.flush();
                throw new RuntimeException(sw.toString());
            }
        }
        refStrings[0] = reader.readLine();
        refStrings[1] = reader.readLine();
        return refStrings;
    }

    public static BufferedReader openFileReader(final String refFile) throws FileNotFoundException {
        return new BufferedReader(new FileReader(refFile)) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    Files.delete(Paths.get(refFile));
                }
            }
        };
    }
}
