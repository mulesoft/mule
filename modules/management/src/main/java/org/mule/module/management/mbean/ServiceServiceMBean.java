/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.mbean;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;

/**
 * <code>ServiceServiceMBean</code> defines the management interface for a mule
 * managed service.
 */
@Deprecated
public interface ServiceServiceMBean extends Stoppable, Startable, FlowConstructServiceMBean, ServiceStatsMBean
{
    String DEFAULT_JMX_NAME_PREFIX = "type=Service,name=";

    /**
     * The number of queued events for this service
     * 
     * @return The number of queued events for this service
     */
    int getQueueSize();

        /**
     * Pauses event processing for theComponent. Unlike stop(), a paused service
     * will still consume messages from the underlying transport, but those messages
     * will be queued until the service is resumed. In order to persist these
     * queued messages you can set the 'recoverableMode' property on the
     * Muleconfiguration to true. this causes all internal queues to store their
     * state.
     *
     * @throws org.mule.api.MuleException if the service failed to pause.
     * @see org.mule.api.config.MuleConfiguration
     */
    void pause() throws MuleException;

    /**
     * Resumes the Service that has been paused. If the service is not paused
     * nothing is executed.
     *
     * @throws MuleException if the service failed to resume
     */
    void resume() throws MuleException;

    boolean isPaused();

    boolean isStopped();

    void dispose() throws MuleException;

    /**
     * Causes the service to stop without processing its event queue first
     */
    void forceStop() throws MuleException;

    boolean isStopping();

}
