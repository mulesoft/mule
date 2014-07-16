/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.polling;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;

/**
 * Extension point for overriding MessageProcessorPolling functionality.
 *
 * This allows to hook on the creation of the event that gets processed as part of the message source evaluation.
 * Users are expected to return instances of this class (or more likely subclasses) when requested via the
 * MessageProcessorPollingOverride methods.
 */
public abstract class MessageProcessorPollingInterceptor
{

    /**
     * Called before sending the event to the message processor that will do poll for events.
     * By default, a new event is created with a message containing an empty string payload.
     * This method may enrich the event or message before it is sent to the processor, or event replace the event).
     * @param event The event that is about to be sent
     * @return The event that should be sent. Must not be null
     */
    public MuleEvent prepareSourceEvent(MuleEvent event) throws MuleException
    {
        return event;
    }

    /**
     * Called after the polling message processor processes the event, but before an event is routed to the rest of the flow.
     * As poll creates an independent event if the polling message processor returns a message, any auxiliary information is discarded. This extension
     * point allows to carry over (or just add) properties to the event that will be sent to the rest of the flow.
     * Note that the source event is discarded after this point, so it doesn't make sense to modify it.
     *
     * @param sourceEvent The event that was returned by the polling processor
     * @param event The event that is about to be sent to the rest of the flow
     *
     * @return The event that should be sent to the rest of the flow. Must not be null
     */
    public MuleEvent prepareRouting(MuleEvent sourceEvent, MuleEvent event) throws MuleException
    {
        return event;
    }

    /**
     * Post process the event after it was routed to the rest of the flow.
     * When this method is called depends on the flow processing strategy. Synchronous processing will make this being called after
     * the flow is executed. Asynchronous processing will make this be called after handing the event to listeners.
     * <p>Implementations should consider the event to be immutable</p>
     * @param event The event that was routed to the rest of the flow
     */
    public void postProcessRouting(MuleEvent event) throws MuleException
    {
        // No-op
    }
}
