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

import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.model.UMOEntryPointResolverSet;
import org.mule.umo.routing.UMOInboundRouterCollection;
import org.mule.umo.routing.UMONestedRouterCollection;
import org.mule.umo.routing.UMOOutboundRouterCollection;
import org.mule.umo.routing.UMOResponseRouterCollection;

import java.beans.ExceptionListener;
import java.util.Map;

/**
 * <code>UMODescriptor</code> describes all the properties for a Mule UMO. New Mule
 * UMOs can be initialised as needed from their descriptor.
 */
public interface UMOImmutableDescriptor extends Initialisable
{
    /**
     * The exception strategy to use to handle exceptions in the Mule UMO.
     *
     * @return the exception strategy to use. If none has been set a default will be
     *         used.
     */
    ExceptionListener getExceptionListener();

    /**
     * Gets the identifier for the Mule UMO created from the descriptor
     *
     * @return the identifier for the Mule UMO created from the descriptor
     */
    String getName();

    /**
     * Returns any properties configured on this descriptor.
     *
     * @return properties defined for the descriptor.
     * @deprecated MULE-1933 Properties for the underlying service should be set on the ServiceFactory instead.
     */
    Map getProperties();

    /**
     * @return Factory which creates an instance of the actual service object.
     */
    //ObjectFactory getServiceFactory();

    /**
     * Inbound Routers control how events are received by a component. If no router
     * is set. A default will be used that uses the inboundProvider set on his
     * descriptor.
     *
     * @return the inbound router for this component. This will always return a valid
     *         router.
     * @see UMOInboundRouterCollection
     */
    UMOInboundRouterCollection getInboundRouter();

    /**
     * Outbound Routers control how events are published by a component once. the
     * event has been processed. If no router is set. A default will be used that
     * uses the outboundProvider set on his descriptor to route the event.
     *
     * @return the outbound router for this component
     * @see UMOOutboundRouterCollection
     */
    UMOOutboundRouterCollection getOutboundRouter();

    UMONestedRouterCollection getNestedRouter();

    /**
     * Response Routers control how events are returned in a request/response call.
     * It cn be use to aggregate response events before returning, thus acting as a
     * Join in a forked process. This can be used to make request/response calls a
     * lot more efficient as independent tasks can be forked, execute concurrently
     * and then join before the request completes
     *
     * @return the response router for this component
     * @see UMOResponseRouterCollection
     */
    UMOResponseRouterCollection getResponseRouter();

    /**
     * Returns the initial state of this component
     *
     * @return the initial state of this component
     */
    String getInitialState();

    /**
     * Returns the name of the model that this descriptor is registered with.
     *
     * @return the name of the model that this descriptor is registered with or null
     *         if this descriptor has not been registered with a model yet
     */
    String getModelName();

    /**
     * A descriptor can have a custom entrypoint resolver for its own object.
     * By default this is null. When set this resolver will override the resolver on the model
     *
     * @return Null is a resolver set has not been set otherwise the resolver to use
     *         on this component
     */
    UMOEntryPointResolverSet getEntryPointResolverSet();

}
