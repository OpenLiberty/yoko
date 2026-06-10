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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Type-specific TypeCode implementations for the Yoko ORB.
 *
 * <p>This package provides a hierarchy of TypeCode implementations, each specialized
 * for a specific CORBA type kind. This design improves type safety, reduces memory
 * footprint, and makes the code more maintainable compared to the monolithic
 * TypeCodeImpl approach.
 *
 * <h2>Class Hierarchy</h2>
 * <ul>
 *   <li>{@link org.apache.yoko.orb.CORBA.typecode.YokoTypeCode} - Abstract base class providing common functionality</li>
 *   <li>{@link org.apache.yoko.orb.CORBA.typecode.PrimitiveTypeCode} - For primitive types (tk_null, tk_void, tk_short, etc.)</li>
 *   <li>{@link org.apache.yoko.orb.CORBA.typecode.StringTypeCode} - For tk_string and tk_wstring</li>
 *   <li>{@link org.apache.yoko.orb.CORBA.typecode.FixedTypeCode} - For tk_fixed</li>
 *   <li>{@link org.apache.yoko.orb.CORBA.typecode.ObjectRefTypeCode} - For tk_objref, tk_abstract_interface, tk_local_interface</li>
 *   <li>{@link org.apache.yoko.orb.CORBA.typecode.EnumTypeCode} - For tk_enum</li>
 *   <li>{@link org.apache.yoko.orb.CORBA.typecode.StructTypeCode} - For tk_struct</li>
 *   <li>{@link org.apache.yoko.orb.CORBA.typecode.UnionTypeCode} - For tk_union</li>
 *   <li>{@link org.apache.yoko.orb.CORBA.typecode.ExceptionTypeCode} - For tk_except</li>
 *   <li>{@link org.apache.yoko.orb.CORBA.typecode.SequenceTypeCode} - For tk_sequence</li>
 *   <li>{@link org.apache.yoko.orb.CORBA.typecode.ArrayTypeCode} - For tk_array</li>
 *   <li>{@link org.apache.yoko.orb.CORBA.typecode.AliasTypeCode} - For tk_alias</li>
 *   <li>{@link org.apache.yoko.orb.CORBA.typecode.ValueTypeCode} - For tk_value</li>
 *   <li>{@link org.apache.yoko.orb.CORBA.typecode.ValueBoxTypeCode} - For tk_value_box</li>
 *   <li>{@link org.apache.yoko.orb.CORBA.typecode.NativeTypeCode} - For tk_native</li>
 *   <li>{@link org.apache.yoko.orb.CORBA.typecode.RecursiveTypeCode} - For recursive type placeholders</li>
 * </ul>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Type Safety:</b> Each TypeCode kind has its own class with only the relevant fields and methods</li>
 *   <li><b>Memory Efficiency:</b> No unused fields for each type</li>
 *   <li><b>Immutability:</b> All TypeCode instances are immutable (thread-safe)</li>
 *   <li><b>Recursion Support:</b> Proper handling of recursive types through RecursiveTypeCode</li>
 *   <li><b>Equivalence:</b> Correct implementation of equal() and equivalent() semantics</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>TypeCode instances should be created through the {@link org.apache.yoko.orb.OB.TypeCodeFactory}
 * which provides factory methods for each type kind. Direct instantiation of TypeCode classes
 * is discouraged.
 *
 * <h2>Equality vs Equivalence</h2>
 * <ul>
 *   <li><b>equal():</b> Strict equality including names and IDs</li>
 *   <li><b>equivalent():</b> Structural equivalence, ignoring names but comparing IDs when present</li>
 * </ul>
 *
 * @see org.apache.yoko.orb.OB.TypeCodeFactory
 * @see org.omg.CORBA.TypeCode
 */
package org.apache.yoko.orb.CORBA.typecode;
