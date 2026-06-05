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

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.logging.Logger;

/**
 * Wrapper for java.lang.reflect.RecordComponent (Java 16+).
 * Provides backward compatibility with Java 8-15 by extracting
 * component information via reflection.
 */
public class RecordComponentInfo {
   
    private final String name;
    private final Class<?> type;
    private final Type genericType;
    private final Method accessor;
   
    private static final Logger logger = Logger.getLogger(RecordComponentInfo.class.getName());
    /**
     * Creates wrapper from RecordComponent object using reflection.
     * 
     * @param recordComponent The RecordComponent object (Java 16+)
     * @throws ReflectiveOperationException if unable to extract info
     */
    RecordComponentInfo(Object recordComponent) throws ReflectiveOperationException {
        Class<?> rcClass = recordComponent.getClass();
        
        // Extract name
        Method getNameMethod = rcClass.getMethod("getName");
        this.name = (String) getNameMethod.invoke(recordComponent);
        
        // Extract type
        Method getTypeMethod = rcClass.getMethod("getType");
        this.type = (Class<?>) getTypeMethod.invoke(recordComponent);
        
        // Extract generic type
        Method getGenericTypeMethod = rcClass.getMethod("getGenericType");
        this.genericType = (Type) getGenericTypeMethod.invoke(recordComponent);
        
        // Extract accessor method
        Method getAccessorMethod = rcClass.getMethod("getAccessor");
        this.accessor = (Method) getAccessorMethod.invoke(recordComponent);
    }
    
    public String getName() {
        return name;
    }
    
    public Class<?> getType() {
        return type;
    }
    
    public Type getGenericType() {
        return genericType;
    }
    
    public Method getAccessor() {
        return accessor;
    }
    
    @Override
    public String toString() {
        return type.getName() + " " + name;
    }
    




}
