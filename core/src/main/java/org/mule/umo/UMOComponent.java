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
import org.mule.umo.lifecycle.Lifecycle;
import org.mule.umo.lifecycle.Registerable;

import java.io.Serializable;

/**
 * <code>UMOComponent</code> is the interal repesentation of a Mule Managed
 * component. It is responsible for managing the interaction of events to and from
 * the component as well as managing pooled resources.
 */

public interface UMOComponent extends Serializable, Lifecycle, Initialisable, Registerable
{
    /**
     * @return the UMODescriptor associated with the component
     * @see UMODescriptor
     */
    UMODescriptor getDescriptor();

    /**
     * Makes an asynhronous event call to the component.
     * 
     * @param event the event to consume
     * @throws UMOException if the event fails to be processed
     */
    void dispatchEvent(UMOEvent event) throws UMOException;

    /**
     * Makes a synhronous event call to the component. This event will be consumed by
     * the component and a result returned.
     * 
     * @param event the event to consume
     * @return a UMOMessage containing the resulting message and properties
     * @throws UMOException if the event fails to be processed
     */
    UMOMessage sendEvent(UMOEvent event) throws UMOException;

    /**
     * Determines whether this component has been started
     * 
     * @return true is the component is started andready to receive events
     */
    boolean isStarted();

    /**
     * Gets the underlying service instance for this component.  Whether this
     * method returns an existing instance or creates a new one will depend on 
     * the particular component and service implementations.  For example, the 
     * service may be a singleton, in which case the same instance is always returned.
     * If object pooling is enabled, it may be an existing instance from the pool
     * of services.  If the service is not a singleton and pooling is not enabled,
     * it will be a new instance of the service.
     * 
     * @return the underlying service instance for this component
     */
    Object getInstance() throws UMOException;

    /**
     * Pauses event processing for a single Mule Component. Unlike stop(), a paused
     * component will still consume messages from the underlying transport, but those
     * messages will be queued until the component is resumed.
     */
    void pause() throws UMOException;

    /**
     * Resumes a single Mule Component that has been paused. If the component is not
     * paused nothing is executed.
     */
    void resume() throws UMOException;

    /**
     * True if the component is in a paused state, false otherwise
     * 
     * @return True if the component is in a paused state, false otherwise
     */
    boolean isPaused();
}
