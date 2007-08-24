/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.mbeans;

import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.Startable;
import org.mule.umo.lifecycle.Stoppable;

import javax.management.ObjectName;

/**
 * <code>ComponentServiceMBean</code> defines the management interface for a mule
 * managed component.
 */
public interface ComponentServiceMBean extends Stoppable, Startable, ComponentStatsMBean
{
    /**
     * The statistics for this component
     * 
     * @return statistics for this component
     * @see ComponentStats
     */
    ObjectName getStatistics();

    /**
     * The name of this component
     * 
     * @return The name of this component
     */
    String getName();

    /**
     * The number of queued events for this component
     * 
     * @return The number of queued events for this component
     */
    int getQueueSize();

    /**
     * Pauses event processing for theComponent. Unlike stop(), a paused component
     * will still consume messages from the underlying transport, but those messages
     * will be queued until the component is resumed. In order to persist these
     * queued messages you can set the 'recoverableMode' property on the
     * Muleconfiguration to true. this causes all internal queues to store their
     * state.
     * 
     * @throws UMOException if the component failed to pause.
     * @see org.mule.config.MuleConfiguration
     */
    void pause() throws UMOException;

    /**
     * Resumes the Component that has been paused. If the component is not paused
     * nothing is executed.
     * 
     * @throws UMOException if the component failed to resume
     */
    void resume() throws UMOException;

    boolean isPaused();

    boolean isStopped();

    void dispose() throws UMOException;

    /**
     * Causes the component to stop without processing its event queue first
     */
    void forceStop() throws UMOException;

    boolean isStopping();
}
