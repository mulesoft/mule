/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.transport;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;

/**
 * Define generic methods for dispatching events.
 * The exact behaviour of the action is defined by the implementing class.
 * 
 * @see org.mule.api.endpoint.OutboundEndpoint
 * @see org.mule.api.transport.MessageDispatcher
 */
public interface MessageDispatching
{
    long RECEIVE_WAIT_INDEFINITELY = 0;
    long RECEIVE_NO_WAIT = -1;

    /**
     * Dispatches an event from the endpoint to the external system
     * 
     * @param event The event to dispatch
     * @throws DispatchException if the event fails to be dispatched
     */
    void dispatch(MuleEvent event) throws DispatchException;

    /**
     * Sends an event from the endpoint to the external system
     * 
     * @param event The event to send
     * @return event the response form the external system wrapped in a MuleEvent
     * @throws DispatchException if the event fails to be dispatched
     */
    MuleMessage send(MuleEvent event) throws DispatchException;

}
