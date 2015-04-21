/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.api.NonBlockingSupported;
import org.mule.VoidMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.NonBlockingResponseReplyToHandler;
import org.mule.processor.chain.DefaultMessageProcessorChain;

/**
 * Asbtract implementation of a {@link org.mule.api.processor.MessageProcessor} that performs processing during the
 * response procesing phase.  Non-blocking processing is supported by implementors seperating the response processing
 * into a seperate {@link org.mule.api.processor.MessageProcessor} returned by {@link #getResponseProcessor()}.
 */
public abstract class AbstractResponseMessageProcessor extends AbstractInterceptingMessageProcessor implements
        NonBlockingSupported
{

    private DefaultMessageProcessorChain chain;

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (event.getReplyToHandler() != null && event.getReplyToHandler() instanceof NonBlockingResponseReplyToHandler)
        {
            ((NonBlockingResponseReplyToHandler) event.getReplyToHandler()).addResponseMessageProcessor
                    (getResponseProcessor());
        }
        MuleEvent result = processNext(event);
        if (result != null && !VoidMuleEvent.isVoid(result))
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

    protected abstract MuleEvent processResponse(MuleEvent processNext) throws MuleException;

}
