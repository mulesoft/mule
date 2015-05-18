/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.DefaultMuleEvent;
import org.mule.NonBlockingVoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.NonBlockingSupported;
import org.mule.api.transport.ReplyToHandler;

/**
 * Abstract implementation of a {@link org.mule.api.processor.MessageProcessor} that may performs processing during both
 * the request and response processing phases while supporting non-blocking execution.
 */
public abstract class AbstractRequestResponseMessageProcessor extends AbstractInterceptingMessageProcessor implements
        NonBlockingSupported
{

    @Override
    public final MuleEvent process(MuleEvent event) throws MuleException
    {
        if (isNonBlocking(event))
        {
            return processNonBlocking(event);
        }
        else
        {
            return processBlocking(event);
        }
    }

    protected MuleEvent processBlocking(MuleEvent event) throws MuleException
    {
        MessagingException exception = null;
        try
        {
            return processResponse(processNext(processRequest(event)));
        }
        catch (MessagingException e)
        {
            exception = e;
            throw e;
        }
        finally
        {
            processFinally(event, exception);
        }
    }

    protected MuleEvent processNonBlocking(MuleEvent event) throws MuleException
    {
        final ReplyToHandler originalReplyToHandler = event.getReplyToHandler();
        event = new DefaultMuleEvent(event, new ReplyToHandler()
        {
            @Override
            public void processReplyTo(MuleEvent event, MuleMessage returnMessage, Object replyTo) throws MuleException
            {
                MuleEvent response = processResponse(new DefaultMuleEvent(event, originalReplyToHandler));
                if (!NonBlockingVoidMuleEvent.getInstance().equals(response))
                {
                    originalReplyToHandler.processReplyTo(response, null, null);
                }
                processFinally(event, null);
            }

            @Override
            public void processExceptionReplyTo(MuleEvent event, MessagingException exception, Object replyTo)
            {
                originalReplyToHandler.processExceptionReplyTo(exception.getEvent(), exception, null);
                processFinally(exception.getEvent(), exception);
            }
        });

        try
        {
            MuleEvent result = processNext(processRequest(event));
            if (!(result instanceof NonBlockingVoidMuleEvent))
            {
                return processResponse(result);
            }
            else
            {
                return result;
            }
        }
        catch (MessagingException exception)
        {
            processFinally(event, exception);
            throw exception;
        }
    }

    private boolean isNonBlocking(MuleEvent event)
    {
        return event.isAllowNonBlocking() && event.getReplyToHandler() != null && next != null;
    }

    /**
     * Processes the request phase before the next message processor is invoked.
     *
     * @param event event to be processed.
     * @return result of request processing.
     * @throws MuleException exception thrown by implementations of this method whiile performing response processing
     */
    protected MuleEvent processRequest(MuleEvent event) throws MuleException
    {
        return event;
    }

    /**
     * Processes the response phase after the next message processor and it's response phase have been invoked
     *
     * @param event event to be processed.
     * @return result of response processing.
     * @throws MuleException exception thrown by implementations of this method whiile performing response processing
     */
    protected MuleEvent processResponse(MuleEvent event) throws MuleException
    {
        return event;
    }

    /**
     * Used to perform post processing after both request and response phases have been completed.  This method will be
     * invoked both when processing is successful as well as if an exception is thrown.
     * successful result and in the case of an exception being thrown.
     *
     * @param event     the result of request and response processing. Note that this includes the request and response
     *                  processing of the rest of the Flow following this message processor too.
     * @param exception the exception thrown during processing if any. If not exception was thrown then this parameter
     *                  is null
     */
    protected void processFinally(MuleEvent event, MessagingException exception)
    {

    }

}
