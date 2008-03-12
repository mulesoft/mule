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
import org.mule.model.seda.SedaService;

import java.beans.ExceptionListener;

import javax.resource.spi.work.Work;

/**
 * A <code>Component</code> is a invoked by a {@link Service} for each incoming
 * {@link MuleEvent} routed on by the {@link InboundRouterCollection}. A component
 * processes a {@link MuleEvent} by invoking the component instance that has been
 * configured, optionally returning a result with running synchronously. 
 * <p/>
 * Implementations of <code>Component</code> can use different types of component
 * implementation, implement component instance pooling or implement <em>bindings</em>
 * which allow for service composition. <br/><br/> <b>TODO</b> <code>Component</code>
 * implementations should be state-less.
 */
public interface Component extends Work, Startable, Stoppable, Disposable
{

    /**
     * Makes a asynchronous invocation of the component This is not currently used
     * because rather than calling onEvent(), setEvent() and then run() is used.
     * <br/> See <a
     * href="http://mule.mulesource.org/jira/browse/MULE-3083">http://mule.mulesource.org/jira/browse/MULE-3083</a>
     * 
     * @param event the event to pass to the component
     */
    //void onEvent(MuleEvent event);

    /**
     * Makes a synchronous invocation of the component
     * 
     * @param event the event to pass to the component
     * @return the return event from the component
     * @throws MuleException if the call fails
     */
    Object onCall(MuleEvent event) throws MuleException;

    /**
     * When an exception occurs this method can be called to invoke the configured
     * UMOExceptionStrategy on the {@link Service}
     * 
     * @param exception If the {@link ExceptionListener} implementation fails
     */
    void handleException(Exception exception);

    /**
     * Sets the current event being processed. This is currently used to set a
     * Component's event so that {@link SedaService} can then schedule a job that will
     * invoke {@link Component} run() and the event will be available for processing.
     * This is currently used followed by scheduling of a {@link Component} job
     * rather than using onEvent() directly because otherwise with the way SEDA
     * events are consumed would mean only one event would processed at the same
     * time.<br/> See <a
     * href="http://mule.mulesource.org/jira/browse/MULE-3083">http://mule.mulesource.org/jira/browse/MULE-3083</a>
     * 
     * @param event the event being processed
     */
    void setEvent(MuleEvent event);
    
    ServiceStatistics getStatistics();

    void setStatistics(ServiceStatistics stat);
}
