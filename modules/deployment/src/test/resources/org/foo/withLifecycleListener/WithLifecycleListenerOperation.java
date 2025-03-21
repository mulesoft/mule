/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo.withLifecycleListener;

import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;

import org.mule.runtime.core.api.MuleContext;

import jakarta.inject.Inject;

public class WithLifecycleListenerOperation {

    @Inject
    private MuleContext muleContext;

    public void leak() {
        // Leaks a Thread that will have the Execution ClassLoader as its TCCL.
        new LeakedThread().start();
    }
}
