/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.internal;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Chain;

/**
 * This class is only used to declare the operations, actual behavior is at
 * {@link org.mule.tests.api.LifecycleTrackerEnricher.LifecycleTrackerComponentExecutorDecorator#execute(ExecutionContext, CompletableComponentExecutor.ExecutorCallback)}
 */
public class LifecycleTrackerOperations {

    public void lifecycleTrackerCheck(String name) {
    }

    public void lifecycleTracker(String name) {
    }

    @MediaType(value = ANY)
    public void lifecycleTrackerScope(String name,
                                      Chain operations,
                                      CompletionCallback<Object, Object> callback) {
        operations.process(callback::success, (error, previous) -> callback.error(error));
    }
}
