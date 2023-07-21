/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.withLifecycleListener;

import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;

import org.mule.runtime.core.api.MuleContext;

import javax.inject.Inject;

public class WithLifecycleListenerOperation {

    @Inject
    private MuleContext muleContext;

    public void leak() {
        // Leaks a Thread that will have the Execution ClassLoader as its TCCL.
        new LeakedThread().start();
    }
}
