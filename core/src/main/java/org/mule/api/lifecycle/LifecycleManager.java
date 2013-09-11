/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.lifecycle;

import org.mule.lifecycle.phases.NotInLifecyclePhase;

/**
 * The LifecycleManager is responsible for managing the different lifecycle phases of the server and managing the
 * transitions between lifecycle phases.
 *
 * @since 3.0
 */
public interface LifecycleManager
{
    static final NotInLifecyclePhase NOT_IN_LIFECYCLE_PHASE = new NotInLifecyclePhase();

    /**
     * Applies lifecycle phase to a collection of objects.
     * @param phase that phase to execute next
     * @throws LifecycleException if the phase is not a valid transition of does not exist on this lifecycle manager
     */
    void fireLifecycle(String phase) throws LifecycleException;

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

    boolean isDirectTransition(String phase);

}
