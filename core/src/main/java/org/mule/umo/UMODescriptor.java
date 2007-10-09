/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo;

import org.mule.impl.ManagementContextAware;
import org.mule.umo.model.UMOEntryPointResolverSet;
import org.mule.umo.routing.UMOInboundRouterCollection;
import org.mule.umo.routing.UMONestedRouterCollection;
import org.mule.umo.routing.UMOOutboundRouterCollection;
import org.mule.umo.routing.UMOResponseRouterCollection;

import java.beans.ExceptionListener;
import java.util.Map;

/**
 * <code>UMODescriptor</code> describes all the properties for a Mule UMO. New Mule
 * Managed components can be initialised as needed from their descriptor.
 */
public interface UMODescriptor extends UMOImmutableDescriptor, ManagementContextAware
{
    /**
     * The exception strategy to use to handle exceptions in the Mule UMO.
     *
     * @param listener the exception strategy to use. If none has been set or
     *                 argument is null a default
     */
    void setExceptionListener(ExceptionListener listener);

    /**
     * sets the identifier for the Mule UMO created from the descriptor
     *
     * @param newName the identifier for the Mule UMO created from the descriptor
     */
    void setName(String newName);

    /**
     * @param props the properties for the descriptor. These will be passed to the
     *              UMO when it's initialise method is called or set as bean properties
     *              whe the UMO is created
     * @deprecated MULE-1933 Properties for the underlying service should be set on the ServiceFactory instead.
     */
    void setProperties(Map props);

    /**
     * @param Factory which creates an instance of the actual service object.
     */
    //void setServiceFactory(ObjectFactory serviceFactory);

    /**
     * Inbound Routers control how events are received by a component. If no router
     * is set. A default will be used that uses the inboundProvider set on his
     * descriptor.
     *
     * @param router the inbound router for this component
     * @see UMOInboundRouterCollection
     */
    void setInboundRouter(UMOInboundRouterCollection router);

    /**
     * Outbound Routers control how events are published by a component once. the
     * event has been processed. If no router is set. A default will be used that
     * uses the outboundProvider set on his descriptor to route the event.
     *
     * @param router the outbound router for this component
     * @see UMOOutboundRouterCollection
     */
    void setOutboundRouter(UMOOutboundRouterCollection router);

    void setNestedRouter(UMONestedRouterCollection router);

    /**
     * Response Routers control how events are returned in a request/response call.
     * It cn be use to aggregate response events before returning, thus acting as a
     * Join in a forked process. This can be used to make request/response calls a
     * lot more efficient as independent tasks can be forked, execute concurrently
     * and then join before the request completes
     *
     * @param router the response router for this component
     * @see org.mule.umo.routing.UMOResponseRouterCollection
     */
    void setResponseRouter(UMOResponseRouterCollection router);

    /**
     * Sets the initial state of this component
     *
     * @param state the initial state of this component
     * @see org.mule.impl.ImmutableMuleDescriptor#INITIAL_STATE_STARTED
     * @see org.mule.impl.ImmutableMuleDescriptor#INITIAL_STATE_STOPPED
     * @see org.mule.impl.ImmutableMuleDescriptor#INITIAL_STATE_PAUSED
     */
    void setInitialState(String state);

    /**
     * Sets the Model name that this descriptor is registered within.
     *
     * @param modelName name of the model
     */
    void setModelName(String modelName);

    /**
     * A descriptor can have a custom entrypoint resolver for its own object.
     * By default this is null. When set this resolver will override the resolver on the model
     *
     * @param resolverSet theresolver set to use when resolving entry points
     *                    on this component
     */
    void setEntryPointResolverSet(UMOEntryPointResolverSet resolverSet);
}
