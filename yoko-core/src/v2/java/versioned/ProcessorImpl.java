/*
 * =============================================================================
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */
package versioned;

import testify.bus.Bus;

/**
 * This class provides no new function.
 * It allows a test to load the processor from a child loader
 * thereby providing an invocation context for any remote method calls.
 */
public class ProcessorImpl extends acme.ProcessorImpl {
    public ProcessorImpl(Bus bus) { super(bus); }
}
