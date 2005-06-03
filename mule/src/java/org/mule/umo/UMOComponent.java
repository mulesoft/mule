/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.umo;

import java.io.Serializable;

import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.Lifecycle;

/**
 * <code>UMOComponent</code> is the interal repesentation of a Mule Managed
 * component. It is responsible for managing the interaction of events to and
 * from the component as well as managing pooled resources.
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
     * Makes a synhronous event call to the component. This event will be
     * consumed by the component and a result returned.
     * 
     * @param event the event to consume
     * @return a UMOMessage containing the resulting message and properties
     * @throws UMOException if the event fails to be processed
     */
    UMOMessage sendEvent(UMOEvent event) throws UMOException;

    /**
     * Pauses event processing for theComponent. Unlike stop(), a paused
     * component will still consume messages from the underlying transport, but
     * those messages will be queued until the component is resumed.
     * 
     * In order to persist these queued messages you can set the
     * 'recoverableMode' property on the Muleconfiguration to true. this causes
     * all internal queues to store their state.
     * 
     * @throws UMOException if the component failed to pause.
     * @see org.mule.config.MuleConfiguration
     */
    void pause() throws UMOException;

    /**
     * Resumes the Component that has been paused. If the component is not
     * paused nothing is executed.
     * 
     * @throws UMOException if the component failed to resume
     */
    void resume() throws UMOException;
}
