/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo;

import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.Registerable;
import org.mule.umo.routing.UMOInboundRouterCollection;
import org.mule.umo.routing.UMONestedRouterCollection;
import org.mule.umo.routing.UMOOutboundRouterCollection;
import org.mule.umo.routing.UMOResponseRouterCollection;

import java.beans.ExceptionListener;
import java.util.List;
import java.util.Map;

/**
 * <code>UMODescriptor</code> describes all the properties for a Mule UMO. New Mule
 * UMOs can be initialised as needed from their descriptor.
 */
public interface UMOImmutableDescriptor extends Initialisable, Registerable
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
     */
    Map getProperties();

    /**
     * Returns a list of interceptor objects that will be executed before/after the
     * Mule UMO has executed
     *
     * @return a list of interceptor objects that will be executed before/after the
     *         Mule UMO has executed
     */
    List getInterceptors();

    /**
     * The version on the Mule UMO. This is currently not used by the mule run-time
     * but may be used in future.
     *
     * @return the Descriptor Version
     */
    String getVersion();

    /**
     * String used to instansiate the object, this can be a class name or a reference
     * to an object in a container
     *
     * @return the Object's class r reference name or an instance of the object to
     *         use
     */
    Object getImplementation();

    /**
     * Class used to instansiate the object, this can be a class name or a reference
     * to an object in a container
     *
     * @return the Object's class representation
     */
    Class getImplementationClass() throws UMOException;

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
     * Determines if only a single instance of this component is created. This is
     * useful when a component hands off event processing to another engine such as
     * Rules processing or Bpel and the processing engine allocates and manages its
     * own threads.
     *
     * @return true if this component is a singleton
     */
    boolean isSingleton();

    /**
     * Returns the initial state of this component
     *
     * @return the initial state of this component
     */
    String getInitialState();

    /**
     * Returns the name of the contaier where the object for this descriptor resides.
     * If this value is 'none' the 'implementaiton' attributed is expected to be a
     * fully qualified class name that will be instanciated.
     *
     * @return the container name, or null if it is not known - in which case each
     *         container will be queried for the component implementation.
     */
    String getContainer();

    /**
     * Returns the name of the model that this descriptor is registered with.
     * @return the name of the model that this descriptor is registered with or null
     * if this descriptor has not been registered with a model yet
     */
    String getModelName();

    /**
     * An instance of the Management context that this descriptor exists in
     * @return
     */
    UMOManagementContext getManagementContext();

}
