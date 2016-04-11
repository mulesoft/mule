/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.lifecycle;

/**
 * Allows intercepting the application of {@link LifecyclePhase lifecycle phases}
 * over each target object.
 *
 * @since 3.8
 */
public interface LifecycleInterceptor
{

    /**
     * Invoked before the given {@code phase} is applied over the {@code object}.
     * <p>
     * This method's return value also indicates if the lifecycle should in fact
     * be applied over the {@code object} or if it should be skipped. Note that
     * {@code this} interceptor is not responsible from actually preventing the
     * phase from being applied. It's the invoker's responsibility to skip the {@code object}
     * if the interceptor indicates so.
     *
     * @param phase  the phase being applied
     * @param object the target object
     * @return whether the {@code phase} should be applied or cancelled for the given {@code object}
     */
    boolean beforeLifecycle(LifecyclePhase phase, Object object);

    /**
     * Invoked after the given {@code phase} was applied over the {@code object}.
     *
     * @param phase  the phase that was applied
     * @param object the target object
     */
    void afterLifecycle(LifecyclePhase phase, Object object);

    /**
     * Invoked when the given {@code phase} finished processing all the eligible
     * target objects, including those for which {@link #beforeLifecycle(LifecyclePhase, Object)}
     * return {@code false}
     *
     * @param phase the phase that was applied
     */
    void onPhaseCompleted(LifecyclePhase phase);
}
