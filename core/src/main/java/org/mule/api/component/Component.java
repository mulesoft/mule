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
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.routing.InboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.management.stats.ComponentStatistics;

/**
 * A <code>Component</code> is a invoked by a {@link Service} for each incoming
 * {@link MuleEvent} routed on by the {@link InboundRouterCollection}. A component
 * processes a {@link MuleEvent} by invoking the component instance that has been
 * configured, optionally returning a result. <p/> Implementations of
 * <code>Component</code> can use different types of component implementation,
 * implement component instance pooling or implement <em>bindings</em> which allow
 * for service composition.
 */
public interface Component extends Lifecycle
{

    /**
     * Invokes the component
     * 
     * @param event the event used to invoke the component
     * @return the return event from the component
     * @throws MuleException if the call fails
     */
    MuleMessage onCall(MuleEvent event) throws MuleException;

    /**
     * Component statistics are used to gather component statistics such as
     * sync/async invocation counts and total and average execution time.
     * 
     * @return
     */
    ComponentStatistics getStatistics();

    /**
     * @param service
     */
    void setService(Service service);

    /**
     * @return
     */
    Service getService();

}
