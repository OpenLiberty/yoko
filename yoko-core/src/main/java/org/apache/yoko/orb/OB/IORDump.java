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
package org.apache.yoko.orb.OB;

import org.apache.yoko.orb.CORBA.InputStream;
import org.apache.yoko.orb.OCI.ConFactory;
import org.apache.yoko.orb.OCI.ConFactoryRegistry;
import org.apache.yoko.orb.OCI.ConFactoryRegistryHelper;
import org.apache.yoko.util.Assert;
import org.apache.yoko.util.HexConverter;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CORBA.UserException;
import org.omg.IOP.IOR;
import org.omg.IOP.IORHelper;
import org.omg.IOP.TAG_MULTIPLE_COMPONENTS;
import org.omg.IOP.TaggedComponent;
import org.omg.IOP.TaggedComponentHelper;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import static org.apache.yoko.util.Hex.formatHexPara;

public class IORDump {

    public static String PrintObjref(ORB orb, IOR ior) {
        StringBuilder sb = new StringBuilder();
        PrintObjref(orb, sb, ior);
        return sb.toString();
    }

    static public void PrintObjref(ORB orb, StringBuilder sb, IOR ior) {
        sb.append("type_id: ").append(ior.type_id).append('\n');

        ConFactoryRegistry conFactoryRegistry = null;
        try {
            org.omg.CORBA.Object obj = orb.resolve_initial_references("OCIConFactoryRegistry");
            conFactoryRegistry = ConFactoryRegistryHelper.narrow(obj);
        } catch (InvalidName ex) {
            throw Assert.fail(ex);
        }
        ConFactory[] factories = conFactoryRegistry.get_factories();

        for (int i = 0; i < ior.profiles.length; i++) {
            sb.append("Profile #" + (i + 1) + ": ");
            if (ior.profiles[i].tag == TAG_MULTIPLE_COMPONENTS.value) {
                sb.append("multiple components");

                InputStream in = new InputStream(ior.profiles[i].profile_data);
                in._OB_readEndian();

                int cnt = in.read_ulong();
                if (cnt == 0) sb.append('\n');
                else {
                    for (int j = 0; j < cnt; j++) {
                        TaggedComponent comp = TaggedComponentHelper.read(in);
                        IORUtil.describe_component(comp, sb);
                    }
                }
            } else {
                int j;
                for (j = 0; j < factories.length; j++) {
                    if (factories[j].tag() == ior.profiles[i].tag) {
                        sb.append(factories[j].id()).append('\n');
                        String desc = factories[j].describe_profile(ior.profiles[i]);
                        sb.append(desc);
                        break;
                    }
                }

                if (j >= factories.length) {
                    sb.append("unknown profile tag ").append(ior.profiles[i].tag).append('\n');
                    sb.append("profile_data: (").append(ior.profiles[i].profile_data.length ).append(")\n");
                    formatHexPara(ior.profiles[i].profile_data, sb);
                }
            }
        }
    }

    static public void DumpIOR(ORB orb, String ref, boolean hasEndian) {
        StringBuilder sb = new StringBuilder();
        DumpIOR(orb, ref, hasEndian, sb);
        PrintWriter pw = new PrintWriter(System.out);
        pw.write(sb.toString());
        pw.flush();
    }

    static public String DumpIORToString(ORB orb, String ref, boolean hasEndian) {
        StringBuilder sb = new StringBuilder();
        DumpIOR(orb, ref, hasEndian, sb);
        return sb.toString();
    }

    static public void DumpIOR(ORB orb, String ref, boolean hasEndian, StringBuilder sb) {
        if (!ref.startsWith("IOR:")) {
            sb.append("IOR is invalid\n");
            return;
        }

        byte[] data = HexConverter.asciiToOctets(ref, 4);
        InputStream in = new InputStream(data);

        boolean endian = in.read_boolean();
        in._OB_swap(endian);

        IOR ior = IORHelper.read(in);

        sb.append("byteorder: ");
        if (hasEndian) sb.append((endian ? "little" : "big") + " endian\n");
        else sb.append("n/a\n");

        PrintObjref(orb, sb, ior);

    }

    static void usage() {
        System.err.println("Usage:");
        System.err.println("org.apache.yoko.orb.OB.IORDump [options] [-f FILE ... | IOR ...]\n"
                        + "\n"
                        + "Options:\n"
                        + "-h, --help          Show this message.\n"
                        + "-v, --version       Show Yoko version.\n"
                        + "-f                  Read IORs from files instead of from the\n"
                        + "                    command line.");
    }

    public static int run(ORB orb, String[] args) throws UserException {
        // Get options
        boolean files = false;
        int i;
        for (i = 0; i < args.length && args[i].charAt(0) == '-'; i++) {
            if (args[i].equals("--help") || args[i].equals("-h")) {
                usage();
                return 0;
            } else if (args[i].equals("--version") || args[i].equals("-v")) {
                System.out.println("Yoko " + Version.getVersion());
                return 0;
            } else if (args[i].equals("-f")) {
                files = true;
            } else {
                System.err.println("IORDump: unknown option `" + args[i] + "'");
                usage();
                return 1;
            }
        }

        if (i == args.length) {
            System.err.println("IORDump: no IORs");
            System.err.println();
            usage();
            return 1;
        }

        if (!files) {
            if (!args[i].startsWith("IOR:") && !args[i].startsWith("corbaloc:")
                    && !args[i].startsWith("corbaname:")
                    && !args[i].startsWith("file:")
                    && !args[i].startsWith("relfile:")) {
                System.err.println("[No valid IOR found on the command "
                        + "line, assuming -f]");
                files = true;
            }
        }

        if (!files) {
            // Dump all IORs given as arguments
            int count = 0;
            for (; i < args.length; i++) {
                if (count > 0)
                    System.out.println();
                System.out.println("IOR #" + (++count) + ':');

                try {
                    // The byte order can only be preserved for IOR: URLs
                    if (args[i].startsWith("IOR:"))
                        DumpIOR(orb, args[i], true);
                    else {
                        // Let string_to_object do the dirty work
                        org.omg.CORBA.Object obj = orb.string_to_object(args[i]);
                        String s = orb.object_to_string(obj);
                        DumpIOR(orb, s, false);
                    }
                } catch (BAD_PARAM ex) {
                    System.err.println("IOR is invalid");
                }
            }
        } else {
            // Dump all IORs from the specified files
            int count = 0;
            for (; i < args.length; i++) {
                try {
                    FileReader fin = new FileReader(args[i]);
                    BufferedReader in = new BufferedReader(fin);
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.length() > 0) {
                            if (count > 0)
                                System.out.println();
                            System.out.println("IOR #" + (++count) + ':');
                            // The byte order can only be preserved for
                            // IOR: URLs
                            if (line.startsWith("IOR:"))
                                DumpIOR(orb, line, true);
                            else {
                                // Let string_to_object do the dirty work
                                org.omg.CORBA.Object obj = orb.string_to_object(line);
                                String s = orb.object_to_string(obj);
                                DumpIOR(orb, s, false);
                            }
                        }
                    }
                } catch (FileNotFoundException ex) {
                    System.err.println("IORDump: can't open `" + args[i]
                            + "': " + ex);
                    return 1;
                } catch (IOException ex) {
                    System.err.println("IORDump: can't read `" + args[i]
                            + "': " + ex);
                    return 1;
                }
            }
        }

        return 0;
    }

    public static void main(String[] args) {
        Properties props = new Properties();
        props.putAll(System.getProperties());
        props.put("org.omg.CORBA.ORBClass", "org.apache.yoko.orb.CORBA.ORB");
        props.put("org.omg.CORBA.ORBSingletonClass", "org.apache.yoko.orb.CORBA.ORBSingleton");

        int status;
        ORB orb = null;

        try {
            args = org.apache.yoko.orb.CORBA.ORB.ParseArgs(args, props, null);
            orb = ORB.init(args, props);
            status = run(orb, args);
        } catch (Exception ex) {
            ex.printStackTrace();
            status = 1;
        }

        if (orb != null) {
            try {
                orb.destroy();
            } catch (Exception ex) {
                ex.printStackTrace();
                status = 1;
            }
        }

        System.exit(status);
    }
}
