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
package org.apache.yoko.orb.OB;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.BAD_PARAM;

import java.net.URI;

import static org.apache.yoko.util.MinorCodes.MinorBadSchemeName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class URLRegistryTest {
    
    private URLRegistry_impl registry;
    private URLScheme mockScheme;
    private org.omg.CORBA.Object mockCorbaObject;
    
    @BeforeEach
    void setUp() throws Exception {
        // Create the registry
        registry = new URLRegistry_impl();
        
        // Create mock URLScheme for corbaname
        mockScheme = mock(URLScheme.class);
        when(mockScheme.name()).thenReturn("corbaname");
        
        // Create mock CORBA Object to return
        mockCorbaObject = mock(org.omg.CORBA.Object.class);
        when(mockScheme.parse(any(URI.class))).thenReturn(mockCorbaObject);
        
        // Register the mock scheme
        registry.addScheme(mockScheme);
    }
    
    @Test
    void testParseUrlWithUnknownScheme() {
        String url = "unknown:scheme:test";
        BAD_PARAM exception = assertThrows(BAD_PARAM.class, () -> registry.parseUrl(url));
        assertEquals(MinorBadSchemeName, exception.minor);
    }
    
    @Test
    void testParseUrlWithKnownCorbanameScheme() throws Exception {
        String url = "corbaname:iiop:localhost:2809/NameService#test";
        
        org.omg.CORBA.Object result = registry.parseUrl(url);
        
        assertNotNull(result);
        assertSame(mockCorbaObject, result);
        verify(mockScheme).parse(any(URI.class));
    }
    
    @Test
    void testParseUrlWithLiteralBackslashes() throws Exception {
        String url = "corbaname:iiop:localhost:2809/NameService#name\\with\\backslash";
        
        org.omg.CORBA.Object result = registry.parseUrl(url);
        
        assertNotNull(result);
        assertSame(mockCorbaObject, result);
        
        // Verify the scheme was called and capture the URI argument
        verify(mockScheme).parse(argThat(uri -> {
            // Verify the fragment contains the backslashes
            String fragment = uri.getFragment();
            assertNotNull(fragment, "Fragment should not be null");
            assertTrue(fragment.contains("\\"), "Fragment should contain backslashes");
            return true;
        }));
    }
}
