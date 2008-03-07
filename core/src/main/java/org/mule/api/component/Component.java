/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.component;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.routing.InboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.management.stats.ServiceStatistics;
import org.mule.util.queue.QueueSession;

import javax.resource.spi.work.Work;

/**
 * A <code>Component</code> is a invoked by a {@link Service} for each incoming
 * {@link MuleEvent} routed on by the {@link InboundRouterCollection}. A component
 * processes a {@link MuleEvent} by invoking the component instance that has been
 * configured, optionally returning a result. <br/> Implementations of
 * <code/>Component</code> can use different types of component implementation,
 * implement component instance pooling or implement <i>binding's<i/> which allow
 * for service composition. <br/><br/> <b>TODO</b> <code>Component</code>
 * implementations should be state-less.
 */
public interface Component extends Work, Startable, Stoppable, Disposable
{

    /**
     * Sets the current event being processed
     * 
     * @param event the event being processed
     */
    void onEvent(QueueSession session, MuleEvent event);

    ServiceStatistics getStatistics();

    void setStatistics(ServiceStatistics stat);

    /**
     * Makes a synchronous call on the UMO
     * 
     * @param event the event to pass to the UMO
     * @return the return event from the UMO
     * @throws MuleException if the call fails
     */
    Object onCall(MuleEvent event) throws MuleException;

    /**
     * When an exception occurs this method can be called to invoke the configured
     * UMOExceptionStrategy on the UMO
     * 
     * @param exception If the UMOExceptionStrategy implementation fails
     */
    void handleException(Exception exception);

    /**
     * Determines if the proxy is suspended
     * 
     * @return true if the proxy (and the UMO) are suspended
     */
    boolean isSuspended();

    /**
     * Controls the suspension of the UMO event processing
     */
    void suspend();

    /**
     * Triggers the UMO to resume processing of events if it is suspended
     */
    void resume();
}
