/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.foo.withLifecycleListener;

import org.mule.runtime.core.api.MuleContext;

import javax.inject.Inject;

public class WithLifecycleListenerOperation {

    @Inject
    private MuleContext muleContext;

    public void leak() {
        // Leaks both the execution ClassLoader and the Extension's.
        new LeakingThread(muleContext.getExecutionClassLoader()).start();
        new LeakingThread(this.getClass().getClassLoader()).start();
    }
}
