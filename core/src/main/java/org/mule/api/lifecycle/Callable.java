/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.lifecycle;

import org.mule.api.MuleEventContext;

/**
 * <code>Callable</code> is used to provide a Service with an interface that supports
 * event calls. Components do not have to implement this interface, though the
 * <code>onCall</code> method provides an example lifecycle method that is executed
 * when an event is received for the implementing service.
 */
public interface Callable extends EventListener
{

    /**
     * Passes the event to the listener
     * 
     * @param eventContext the context of the current event being process
     * @return Object this object can be anything. When the
     *         <code>LifecycleAdapter</code> for the service receives this
     *         object it will first see if the Object is an <code>MuleMessage</code>
     *         if not and the Object is not null a new message will be created using
     *         the returned object as the payload. This new event will then get
     *         published via the configured outbound router if-
     *         <ol>
     *         <li>One has been configured for the component.</li>
     *         <li>the <code>setStopFurtherProcessing(true)</code> wasn't called
     *         on the event context event.</li>
     *         </ol>
     * @throws Exception if the event fails to process properly. If exceptions aren't
     *             handled by the implementation they will be handled by the
     *             exceptionListener associated with the service
     */
    Object onCall(MuleEventContext eventContext) throws Exception;

}
