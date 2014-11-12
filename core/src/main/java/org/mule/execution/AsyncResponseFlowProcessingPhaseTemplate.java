/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.execution;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;

/**
 * Template methods for {@link org.mule.api.source.MessageSource} specific behavior during
 * flow execution.
 */
public interface AsyncResponseFlowProcessingPhaseTemplate extends MessageProcessTemplate
{

    /**
     * @return a {@link org.mule.api.MuleEvent} created from the original message
     */
    MuleEvent getMuleEvent() throws MuleException;

    /**
     * @return the original message
     */
    Object getOriginalMessage() throws MuleException;

    /**
     * Pre processing of the {@link org.mule.api.MuleEvent} to route
     *
     * @param muleEvent
     */
    MuleEvent beforeRouteEvent(MuleEvent muleEvent) throws MuleException;

    /**
     * Routes the {@link org.mule.api.MuleEvent} through the processors chain
     *
     * @param muleEvent {@link org.mule.api.MuleEvent} created from the raw message of this context
     * @return the response {@link org.mule.api.MuleEvent}
     * @throws org.mule.api.MuleException
     */
    MuleEvent routeEvent(MuleEvent muleEvent) throws MuleException;

    /**
     * Post processing of the routed {@link org.mule.api.MuleEvent}
     *
     * @param muleEvent
     */
    MuleEvent afterRouteEvent(MuleEvent muleEvent) throws MuleException;

    /**
     * Call when the processing of the message through the flow fails in an exception strategy
     *
     * @param exception
     */
    void afterFailureProcessingFlow(Exception exception);

    /**
     * Template method to send a response after processing the message.
     * <p/>
     * This method is executed within the flow so if it fails it will trigger the exception strategy.
     *
     *
     *
     * @param template
     * @param muleEvent the event with the content of the response to be sent.
     * @param responseCompletationCallback
     * @throws MuleException exception thrown when processing the message to send the response. If there's a failure when writing the response
     *                       using the underlying transport or connector then the exception to throw must be a {@link ResponseDispatchException}.
     */
    void sendResponseToClient(AsyncResponseFlowProcessingPhaseTemplate template, MuleEvent muleEvent, AsyncResponseFlowProcessingPhase.ResponseCompletationCallback responseCompletationCallback) throws MuleException;


    /**
     *
     * @param messagingException exception thrown during the flow execution.
     * @param responseCompletationCallback
     * @throws MuleException exception thrown when processing the message to send the response.
     */
    void sendFailureResponseToClient(MessagingException messagingException, AsyncResponseFlowProcessingPhase.ResponseCompletationCallback responseCompletationCallback) throws MuleException;

}
