/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
