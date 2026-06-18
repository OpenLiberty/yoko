/*
 * Copyright 2026 IBM Corporation and others.
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

package org.apache.yoko.rmi.impl;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.apache.yoko.logging.VerboseLogging.MARSHAL_LOG;
import static org.apache.yoko.logging.VerboseLogging.MARSHAL_IN_LOG;
import static org.apache.yoko.logging.VerboseLogging.MARSHAL_OUT_LOG;

/**
 * Utility class for Java record serialization support.
 * Encapsulates all record-specific logic to minimize changes to ValueDescriptor.
 * Uses reflection to work with Java 16+ records while maintaining backward
 * compatibility with Java 8-15.
 */
public class RecordSupportUtils {

    
    // Static reflection-based detection
    private static final Method IS_RECORD_METHOD;
    private static final Method GET_RECORD_COMPONENTS_METHOD;
    
    static {
        Method isRecord = null;
        Method getComponents = null;
        try {
            isRecord = Class.class.getMethod("isRecord");
            getComponents = Class.class.getMethod("getRecordComponents");
        } catch (NoSuchMethodException e) {
            // Running on Java < 16, records not supported
        }
        IS_RECORD_METHOD = isRecord;
        GET_RECORD_COMPONENTS_METHOD = getComponents;
    }
    
    
    // Instance fields for specific record class
    private final Class<?> recordClass;
    private final RecordComponentInfo[] components;
    private final Constructor<?> canonicalConstructor;
    
    /**
     * Private constructor - use forClass() factory method.
     */
    private RecordSupportUtils(Class<?> recordClass) throws ReflectiveOperationException {
        this.recordClass = recordClass;
        this.components = extractComponents(recordClass);
        this.canonicalConstructor = findCanonicalConstructor(recordClass, components);
        this.canonicalConstructor.setAccessible(true);
    }

   /**
     * Check if a class is a Java record.
     * 
     * @param clazz The class to check
     * @return true if record (Java 16+), false otherwise
     */
    public static boolean isRecord(Class<?> clazz) {
        if (IS_RECORD_METHOD == null) {
            return false;
        }
        
        try {
            return (Boolean) IS_RECORD_METHOD.invoke(clazz);
        } catch (Exception e) {
            MARSHAL_LOG.warning("Failed to check if class is record: " + clazz.getName());
            return false;
        }
    }
    
    /**
     * Create RecordSupportUtils for a record class.
     * 
     * @param clazz The class to check
     * @return RecordSupportUtils instance, or null if not a record
     * @throws IllegalArgumentException if record but cannot be processed
     */
    public static RecordSupportUtils forClass(Class<?> clazz) {
        if (!isRecord(clazz)) {
            return null;
        }
        
        try {
            return new RecordSupportUtils(clazz);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(
                "Failed to create RecordSupportUtils for: " + clazz.getName(), e);
        }
    }
      
    /**
     * Extract record components using reflection.
     */
    private static RecordComponentInfo[] extractComponents(Class<?> clazz) 
            throws ReflectiveOperationException {
        if (GET_RECORD_COMPONENTS_METHOD == null) {
            return new RecordComponentInfo[0];
        }
        
        Object[] components = (Object[]) GET_RECORD_COMPONENTS_METHOD.invoke(clazz);
        if (components == null) {
            return new RecordComponentInfo[0];
        }
        
        RecordComponentInfo[] result = new RecordComponentInfo[components.length];
        for (int i = 0; i < components.length; i++) {
            result[i] = new RecordComponentInfo(components[i]);
        }
        return result;
    }

    /**
     * Find canonical constructor matching record components.
     */
    private static Constructor<?> findCanonicalConstructor(Class<?> clazz,
                                                           RecordComponentInfo[] components)
            throws NoSuchMethodException {
        Class<?>[] paramTypes = new Class<?>[components.length];
        for (int i = 0; i < components.length; i++) {
            paramTypes[i] = components[i].getType();
        }
        return clazz.getDeclaredConstructor(paramTypes);
    }

    /**
     * Validate record follows serialization rules.
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        
        // 1. Records cannot have writeObject method
        try {
            Method writeObj = recordClass.getDeclaredMethod("writeObject",
                java.io.ObjectOutputStream.class);
            throw new IllegalArgumentException(
                "Record cannot define writeObject: " + recordClass.getName());
        } catch (NoSuchMethodException e) {
            // Expected - records should not have this method
        }
        
        // 2. Records cannot have readObject method
        try {
            Method readObj = recordClass.getDeclaredMethod("readObject",
                java.io.ObjectInputStream.class);
            throw new IllegalArgumentException(
                "Record cannot define readObject: " + recordClass.getName());
        } catch (NoSuchMethodException e) {
            // Expected - records should not have this method
        }
        
        // 3. Records cannot have serialPersistentFields
        try {
            Field spf = recordClass.getDeclaredField("serialPersistentFields");
            throw new IllegalArgumentException(
                "Record cannot define serialPersistentFields: " + recordClass.getName());
        } catch (NoSuchFieldException e) {
            // Expected - records should not have this field
        }
    }


    /**
     * Write record components to stream.
     * 
     * @param writer The ObjectWriter to write to
     * @param record The record instance to serialize
     * @throws IOException if writing fails
     */
    public void writeComponents(ObjectWriter writer, Object record) throws IOException {

        MARSHAL_OUT_LOG.finer("Writing record components for " + recordClass.getName());

        try {
            for (RecordComponentInfo component : components) {
                MARSHAL_OUT_LOG.finer("Writing record component: " + component.getName());
                Method accessor = component.getAccessor();
                Object value = accessor.invoke(record);
                writer.writeObject(value);
            }
        } catch (Exception e) {
            throw new IOException("Failed to write record components for: " + 
                recordClass.getName(), e);
        }
    }


     
    /**
     * Read components and construct record instance.
     * 
     * @param reader The ObjectReader to read from
     * @return The constructed record instance
     * @throws IOException if reading or construction fails
     */
    public Object readAndConstruct(ObjectReader reader) throws IOException {

        MARSHAL_IN_LOG.finer("Reading record components for " + recordClass.getName());

        try {
            Object[] args = new Object[components.length];
            for (int i = 0; i < components.length; i++) {
                MARSHAL_IN_LOG.finer("Reading record component: " + components[i].getName() + " of type: " + components[i].getType());
                args[i] = reader.readObject();
            }
            return canonicalConstructor.newInstance(args);
        } catch (Exception e) {
            throw new IOException("Failed to construct record instance for: " + 
                recordClass.getName(), e);
        }
    }
    
    /**
     * Get record components.
     */
    public RecordComponentInfo[] getComponents() {
        return components;
    }
    
    /**
     * Get canonical constructor.
     */
    public Constructor<?> getCanonicalConstructor() {
        return canonicalConstructor;
    }
    
}
