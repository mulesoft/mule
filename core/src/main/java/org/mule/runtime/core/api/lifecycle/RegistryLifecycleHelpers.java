/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.lifecycle;

/**
 * This interface defines additional Registry Lifecycle methods to enable extenral objects to have there
 * lifecycle managed by the registry.
 *
 * @since 3.0
 */
public interface RegistryLifecycleHelpers
{
    /**
     * Applies lifecycle phase to an object independent of the current lifecycle phase. All phases between the current
     * phase and the 'endPhase' will be executed.
     * @param object the object to apply lifecycle to
     * @param fromPhase the lifecycle phase the object is currently in. Must not be null.
     * @param toPhase the lifecycle phase to transition the object to. Must not be null.
     * @throws org.mule.api.lifecycle.LifecycleException if there is an exception while invoking lifecycle on the object
     */
    void applyPhase(Object object, String fromPhase, String toPhase) throws LifecycleException;

    /**
     * Successively applies all completed lifecycle phase to an object.
     *
     * @param object the object to which the lifecycle should be applied
     * @throws org.mule.api.lifecycle.LifecycleException if there is an error while applying lifecycle to the object
     */
    void applyCompletedPhases(Object object) throws LifecycleException;
}
