/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.lifecycle;

import org.mule.api.MuleException;
import org.mule.api.registry.Registry;

import java.util.List;

/**
 * The LifecycleManager is responsible for managing the different lifecycle phases of the server and managing the
 * transitions between lifecycle phases.
 */
public interface LifecycleManager
{
    /**
     * The lifecycle pairs that will be invoked by this lifecycle manager
     * @return The lifecycle pairs that will be invoked by this lifecycle manager
     *
     * @since 3.0
     */
    List<LifecyclePair> getLifecyclePairs();

    /**
     * The lifecycle pairs that will be invoked by this lifecycle manager
     * The order the list will be the order in which  the life cycle phases will be invoked
     * the {@link org.mule.api.lifecycle.LifecyclePair#getBegin()} phases will be called in order, the
     * {@link LifecyclePair#getEnd()} will be called in opposite order. i.e. call initialise first and dispose last.
     * @param lifecyclePairs The lifecycle pairs that will be invoked by this lifecycle manager
     *
     * @since 3.0
     */
    void setLifecyclePairs(List<LifecyclePair> lifecyclePairs);

    /**
     * Register a lifecycle pair that will be invoked by this lifecycle manager
     * The order in which the lifecycle pairs are registered will be the order in which  the life cycle phases will be invoked
     * the {@link org.mule.api.lifecycle.LifecyclePair#getBegin()} phases will be called in order, the
     * {@link LifecyclePair#getEnd()} will be called in opposite order. i.e. call initialise first and dispose last.
     * @param lifecyclePair a lifecycle pair that will be invoked by this lifecycle manager
     *
     * @since 3.0
     */
    void registerLifecycle(LifecyclePair lifecyclePair);

    /**
     * Applies lifecycle phase to a collection of objects.
     */
    void fireLifecycle(Registry registry, String phase) throws MuleException;

    /**
     * Applies lifecycle phase to an object independent of the current lifecycle phase. All phases between the current
     * phase and the 'endPhase' will be executed.
     * @param endPhase the final phase to execute on the object.  All phases inbetween current and end will be executed
     */
    void applyPhase(Object object, String phase) throws MuleException;


    /**
     * The current phase for the lifecycle manager.  While in transition this will reflect the last completed phase not
     * the currently executing phase, use {@link #getExecutingPhase()} to get the phase being executed.
     * @return The current completed phase for the lifecycle manager
     */
    String getCurrentPhase();

    /**
     * Returns the lifecycle phase being executed. This will be null if the lifecycle is not in transition
     * @return the lifecycle phase being executed
     */
    String getExecutingPhase();

    /**
     * Reset the lifecycle manager state back to 'not in lifecycle' phase
     */
    void reset();

    boolean isPhaseComplete(String phaseName);

    /**
     * Successively applies all completed lifecycle phase to an object.
     */
    void applyCompletedPhases(Object object) throws MuleException;

    void checkPhase(String name) throws IllegalStateException;

    /**
     * Provides access to a state machine for this lifecycle manager.  components in the registry can use this to assert lifecycle
     * rather than managing thier own lifecycle state
     *
     * @return A state machine for this lifecycle manager
     *
     * @since 3.0
     */
    LifecycleState getState();
}
