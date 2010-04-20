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
     * @param phase that phase to execute next
     * @throws LifecycleException if the phase is not a valid transition of does not exist on this lifecycle manager
     */
    void fireLifecycle(String phase) throws LifecycleException;

    /**
     * Applies lifecycle phase to an object independent of the current lifecycle phase. All phases between the current
     * phase and the 'endPhase' will be executed.
     * @param object the object to apply lifecycle to
     * @param phase the lifecycle phase to execute
     * @throws LifecycleException if there is an exection while invoking lifecycle on the object
     */
    void applyPhase(Object object, String phase) throws LifecycleException;


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

    /**
     * Checks that a phase has completed
     * @param phaseName the name of the pahse to check for
     * @return true if that phase has completed, false if the phase has not completed, or currently processing or does not exist
     */
    boolean isPhaseComplete(String phaseName);

    /**
     * Successively applies all completed lifecycle phase to an object.
     *
     * @param object the object to which the lifecycle should be applied
     * @throws LifecycleException if there is an error while applying lifecycle to the object
     */
    void applyCompletedPhases(Object object) throws LifecycleException;


    /**
     * Will check that the phase passed in is a valid next phase for this lifecycle manager.  If the phase is not a valid next
     * transition an exception will be thrown
     *
     * @param name The name of the lifecycle to validate as a valid next transition
     * @throws IllegalStateException if the lifecycle name is not recognised or the phase is not valid for the current lifecycle state
     */
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
