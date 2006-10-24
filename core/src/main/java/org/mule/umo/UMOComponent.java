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

import java.io.Serializable;

/**
 * <code>UMOComponent</code> is the interal repesentation of a Mule Managed
 * component. It is responsible for managing the interaction of events to and from
 * the component as well as managing pooled resources.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public interface UMOComponent extends Serializable, Lifecycle, Initialisable
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
     * Gets the underlying instance form this component Where the Component
     * implmentation provides pooling this is no 1-2-1 mapping between UMOComponent
     * and instance, so this method will return the object in initial state. If the
     * underlying component is Container managed in Spring or another IoC container
     * then the object instance in the IoC container will be returned
     * 
     * @return the underlying instance form this component
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
