/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.api.execution.CompletionHandler;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.execution.AsyncResponseFlowProcessingPhaseTemplate;
import org.mule.runtime.core.execution.ResponseCompletionCallback;

import java.io.Serializable;

final class ExtensionFlowProcessingTemplate<Payload, Attributes extends Serializable> implements AsyncResponseFlowProcessingPhaseTemplate
{

    private final MuleEvent event;
    private final MessageProcessor messageProcessor;
    private final CompletionHandler<MuleMessage<Payload, Attributes>, MessagingException> completionHandler;

    ExtensionFlowProcessingTemplate(MuleEvent event,
                                    MessageProcessor messageProcessor,
                                    CompletionHandler<MuleMessage<Payload, Attributes>, MessagingException> completionHandler)
    {
        this.event = event;
        this.messageProcessor = messageProcessor;
        this.completionHandler = completionHandler;
    }

    @Override
    public MuleEvent getMuleEvent() throws MuleException
    {
        return event;
    }

    @Override
    public MuleEvent routeEvent(MuleEvent muleEvent) throws MuleException
    {
        return messageProcessor.process(muleEvent);
    }

    @Override
    public void sendResponseToClient(MuleEvent muleEvent, ResponseCompletionCallback responseCompletionCallback) throws MuleException
    {
        runAndNotify(() -> completionHandler.onCompletion((MuleMessage<Payload, Attributes>) muleEvent.getMessage()), event, responseCompletionCallback);
    }

    @Override
    public void sendFailureResponseToClient(MessagingException messagingException, ResponseCompletionCallback responseCompletionCallback) throws MuleException
    {
        runAndNotify(() -> completionHandler.onFailure(messagingException), event, responseCompletionCallback);
    }

    private void runAndNotify(Runnable runnable, MuleEvent event, ResponseCompletionCallback responseCompletionCallback)
    {
        try
        {
            runnable.run();
            responseCompletionCallback.responseSentSuccessfully();
        }
        catch (Exception e)
        {
            responseCompletionCallback.responseSentWithFailure(e, event);
        }
    }
}
