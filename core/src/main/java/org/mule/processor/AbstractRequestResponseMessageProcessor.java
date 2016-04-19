/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.DefaultMuleEvent;
import org.mule.NonBlockingVoidMuleEvent;
import org.mule.OptimizedRequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.NonBlockingSupported;
import org.mule.api.transport.NonBlockingReplyToHandler;
import org.mule.api.transport.ReplyToHandler;

/**
 * Base implementation of a {@link org.mule.api.processor.MessageProcessor} that may performs processing during both the
 * request and response processing phases while supporting non-blocking execution.
 * <p/>
 *
 * In order to define the process during the request phase you should override the
 * {@link #processRequest(org.mule.api.MuleEvent)} method. Symmetrically, if you need to define a process to be executed
 * during the response phase, then you should override the {@link #processResponse(MuleEvent, MuleEvent)} method.
 * <p/>
 *
 * In some cases you'll have some code that should be always executed, even if an error occurs, for those cases you
 * should override the {@link #processFinally(org.mule.api.MuleEvent, org.mule.api.MessagingException)} method.
 *
 * @since 3.7.0
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
            return processResponse(processNext(processRequest(event)), event);
        }
        catch (MessagingException e)
        {
            exception = e;
            return processCatch(event, e);
        }
        finally
        {
            processFinally(event, exception);
        }
    }

    protected MuleEvent processNonBlocking(final MuleEvent event) throws MuleException
    {
        MessagingException exception = null;
        MuleEvent eventToProcess = new DefaultMuleEvent(event, createReplyToHandler(event));
        // Update RequestContext ThreadLocal for backwards compatibility
        eventToProcess = OptimizedRequestContext.unsafeSetEvent(eventToProcess);

        try
        {
            MuleEvent result = processNext(processRequest(eventToProcess));
            if (!(result instanceof NonBlockingVoidMuleEvent))
            {
                return processResponse(recreateEventWithOriginalReplyToHandler(result, event.getReplyToHandler()), event);
            }
            else
            {
                return result;
            }
        }
        catch (MessagingException e)
        {
            exception = e;
            return processCatch(event, e);
        }
        finally
        {
            processFinally(event, exception);
        }
    }

    protected ReplyToHandler createReplyToHandler(final MuleEvent request)
    {
        final ReplyToHandler originalReplyToHandler = request.getReplyToHandler();
        return new NonBlockingReplyToHandler()
        {
            @Override
            public void processReplyTo(MuleEvent event, MuleMessage returnMessage, Object replyTo) throws MuleException
            {
                try
                {
                    MuleEvent response = processResponse(recreateEventWithOriginalReplyToHandler(event, originalReplyToHandler), request);
                    if (!NonBlockingVoidMuleEvent.getInstance().equals(response))
                    {
                        originalReplyToHandler.processReplyTo(response, null, null);
                    }
                }
                catch (Exception e)
                {
                    processExceptionReplyTo(new MessagingException(event, e), null);
                }
                finally
                {
                    processFinally(event, null);
                }
            }

            @Override
            public void processExceptionReplyTo(MessagingException exception, Object replyTo)
            {
                try
                {
                    MuleEvent handledEvent = processCatch(exception.getEvent(), exception);
                    originalReplyToHandler.processReplyTo(handledEvent, null, null);
                }
                catch (Exception e)
                {
                    originalReplyToHandler.processExceptionReplyTo(exception, replyTo);
                }
                finally
                {
                    processFinally(exception.getEvent(), exception);
                }
            }
        };
    }

    private MuleEvent recreateEventWithOriginalReplyToHandler(MuleEvent event, ReplyToHandler originalReplyToHandler)
    {
        if (event != null)
        {
            event = new DefaultMuleEvent(event, originalReplyToHandler);
            // Update RequestContext ThreadLocal for backwards compatibility
            OptimizedRequestContext.unsafeSetEvent(event);
        }
        return event;
    }

    protected boolean isNonBlocking(MuleEvent event)
    {
        return event.isAllowNonBlocking() && event.getReplyToHandler() != null;
    }

    /**
     * Processes the request phase before the next message processor is invoked.
     *
     * @param request event to be processed.
     * @return result of request processing.
     * @throws MuleException exception thrown by implementations of this method whiile performing response processing
     */
    protected MuleEvent processRequest(MuleEvent request) throws MuleException
    {
        return request;
    }

    /**
     * Processes the response phase after the next message processor and it's response phase have been invoked
     *
     * @param response response event to be processed.
     * @param request the request event
     * @return result of response processing.
     * @throws MuleException exception thrown by implementations of this method whiile performing response processing
     */
    protected MuleEvent processResponse(MuleEvent response, final MuleEvent request) throws MuleException
    {
        return processResponse(response);
    }

    /**
     * Processes the response phase after the next message processor and it's response phase have been invoked.  This
     * method is deprecated, use {@link #processResponse(MuleEvent, MuleEvent)} instead.
     *
     * @param response response event to be processed.
     * @return result of response processing.
     * @throws MuleException exception thrown by implementations of this method whiile performing response processing
     */
    @Deprecated
    protected MuleEvent processResponse(MuleEvent response) throws MuleException
    {
        return response;
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

    protected MuleEvent processCatch(MuleEvent event, MessagingException exception) throws MessagingException
    {
        throw exception;
    }

}
