/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.NonBlockingVoidMuleEvent;
import org.mule.api.NonBlockingSupported;
import org.mule.VoidMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.NonBlockingResponseReplyToHandler;
import org.mule.processor.chain.DefaultMessageProcessorChain;

/**
 * Abstract implementation of a {@link org.mule.api.processor.MessageProcessor} that performs processing during the
 * response processing phase.  Non-blocking processing is supported by implementors seperating the response processing
 * into a separate {@link org.mule.api.processor.MessageProcessor} returned by {@link #getResponseProcessor()}.
 */
public abstract class AbstractRequestResponseMessageProcessor extends AbstractInterceptingMessageProcessor implements
        NonBlockingSupported
{

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        boolean processResponseOnCallback = false;
        if (isNonBlocking(event))
        {
            ((NonBlockingResponseReplyToHandler) event.getReplyToHandler()).addResponseMessageProcessor
                    (getResponseProcessor());
        }
        MuleEvent result = processNext(event);
        if (!(result instanceof NonBlockingVoidMuleEvent))
        {
            return getResponseProcessor().process(result);
        }
        else
        {
            return result;
        }
    }

    protected MessageProcessor getResponseProcessor()
    {
        return new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                return processResponse(event);
            }
        };
    }

    private boolean isNonBlocking(MuleEvent event)
    {
        return event.isAllowNonBlocking() && event.getReplyToHandler() instanceof NonBlockingResponseReplyToHandler &&
               next != null;
    }

    protected abstract MuleEvent processRequest(MuleEvent processNext) throws MuleException;

    protected abstract MuleEvent processResponse(MuleEvent processNext) throws MuleException;

}
