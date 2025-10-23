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

import static org.apache.yoko.util.Hex.formatHexPara;

import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.yoko.orb.CORBA.YokoInputStream;
import org.apache.yoko.orb.OCI.ConFactory;
import org.apache.yoko.orb.OCI.ConFactoryRegistry;
import org.apache.yoko.orb.OCI.ConFactoryRegistryHelper;
import org.apache.yoko.util.Assert;
import org.apache.yoko.util.HexConverter;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.ORB;
import org.omg.CORBA.UserException;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.IOP.IOR;
import org.omg.IOP.IORHelper;
import org.omg.IOP.TAG_MULTIPLE_COMPONENTS;
import org.omg.IOP.TaggedComponent;
import org.omg.IOP.TaggedComponentHelper;
import org.omg.IOP.TaggedProfile;

public class IORDump {

    public static String describeIor(ORB orb, IOR ior) {
        try {
            return describeIor(new StringBuilder(), orb, ior).toString();
        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println(t);
            t.printStackTrace(pw);
            return sw.toString();
        }
    }

    private static StringBuilder describeIor(StringBuilder sb, ORB orb, IOR ior) {
        sb.append("type_id: ").append(ior.type_id).append('\n');

        ConFactoryRegistry conFactoryRegistry = null;
        try {
            org.omg.CORBA.Object obj = orb.resolve_initial_references("OCIConFactoryRegistry");
            conFactoryRegistry = ConFactoryRegistryHelper.narrow(obj);
        } catch (InvalidName ex) {
            throw Assert.fail(ex);
        }

        ConFactory[] factories = conFactoryRegistry.get_factories();

        int count = 1;
        for (TaggedProfile p: ior.profiles) {
            describeProfile(sb, factories, p, count++);
        }
        return sb;
    }

    private static void describeProfile(StringBuilder sb, ConFactory[] factories, TaggedProfile p, int index) {
        sb.append("Profile #").append(index).append(": ");
        if (p.tag == TAG_MULTIPLE_COMPONENTS.value) {
            sb.append("multiple components");

            YokoInputStream in = new YokoInputStream(p.profile_data);
            in._OB_readEndian();

            int cnt = in.read_ulong();
            if (cnt == 0) {
                sb.append('\n');
            } else {
                for (int j = 0; j < cnt; j++) {
                    TaggedComponent comp = TaggedComponentHelper.read(in);
                    IORUtil.describe_component(comp, sb);
                }
            }
        } else {
            Optional<ConFactory> factory = Arrays.stream(factories)
                    .filter(f -> f.tag() == p.tag)
                    .findAny();
            if (factory.isPresent()) {
                ConFactory f = factory.get();
                sb.append(f.id()).append('\n');
                String desc = f.describe_profile(p);
                sb.append(desc);
            } else {
                sb.append("unknown profile tag ").append(p.tag).append('\n');
                sb.append("profile_data: (").append(p.profile_data.length ).append(")\n");
                formatHexPara(p.profile_data, sb);
            }
        }
    }

    private static String describeIorString(ORB orb, String ref, boolean describeByteOrder) {
        if (!ref.startsWith("IOR:")) return "IOR is invalid\n";
        byte[] data = HexConverter.asciiToOctets(ref, 4);
        YokoInputStream in = new YokoInputStream(data);

        StringBuilder sb = new StringBuilder();
        // If this IOR is not encoded to string by this VM, the byte order might be of interest
        if (describeByteOrder) {
            sb.append("byteorder: ");
            boolean endian = in.read_boolean();
            in._OB_swap(endian);
            sb.append((endian ? "little" : "big") + " endian\n");
        } else {
            in._OB_readEndian();
        }

        describeIor(sb, orb, IORHelper.read(in));
        return sb.toString();
    }

    private static void usage() {
        System.err.println("Usage:");
        System.err.println("org.apache.yoko.orb.OB.IORDump [options] [-f FILE ... | IOR ...]\n"
                        + "\n"
                        + "Options:\n"
                        + "-h, --help          Show this message.\n"
                        + "-v, --version       Show Yoko version.\n"
                        + "-f                  Read IORs from files instead of from the\n"
                        + "                    command line.");
    }

    private static int run(ORB orb, String[] args) throws UserException {
        // Get options
        boolean files = false;
        int i;
        for (i = 0; i < args.length && args[i].charAt(0) == '-'; i++) {
            switch (args[i]) {
                case "--help" :
                case "-h" :
                    usage();
                    return 0;
                case "--version" :
                case "-v" :
                    System.out.println("Yoko " + Version.getVersion());
                    return 0;
                case "-f" :
                    files = true;
                    break;
                default :
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

        AtomicInteger count = new AtomicInteger(1);
        Stream<String> remainingArgs = Arrays.stream(args).skip(i);
        if (files) remainingArgs.map(Paths::get).flatMap(IORDump::lines).forEach(line -> printIorString(orb, line, count.getAndIncrement()));
        else remainingArgs.forEach(arg -> printIorString(orb, arg, count.getAndIncrement()));

        return 0;
    }

    static Stream<String> lines(Path p) {
        try {
            return Files.lines(p);
        } catch (IOException e) {
            System.err.println("IORDump: can't open '" + p + "': " + e);
            throw new IOError(e);
        }
    }

    private static void printIorString(ORB orb, String ior, int index) {
        if (index > 1) System.out.println();
        System.out.println("IOR #" + index + ':');
        try {
            // The byte order can only be preserved for IOR: URLs
            if (ior.startsWith("IOR:"))
                System.out.println(describeIorString(orb, ior, true));
            else {
                // Let string_to_object do the dirty work
                org.omg.CORBA.Object obj = orb.string_to_object(ior);
                String s = orb.object_to_string(obj);
                System.out.println(describeIorString(orb, s, false));
            }
        } catch (BAD_PARAM ex) {
            System.err.println("IOR is invalid: " + ior);
        }
    }

    public static void main(String... args) {
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
