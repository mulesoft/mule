/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.response;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.AsyncReplyInterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.MessageInfoMapping;
import org.mule.api.routing.ResponseTimeoutException;
import org.mule.api.source.MessageSource;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.RoutingNotification;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.routing.MuleMessageInfoMapping;
import org.mule.util.concurrent.Latch;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentMap;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

import org.apache.commons.collections.buffer.BoundedFifoBuffer;

public class DefaultAsyncReplyInterceptingMessageProcessor extends AbstractInterceptingMessageProcessor
    implements AsyncReplyInterceptingMessageProcessor
{

    private volatile long timeout = -1; // undefined
    protected MessageSource replyMessageSource;
    private MessageInfoMapping messageInfoMapping = new MuleMessageInfoMapping();

    private final MessageProcessor internalAsyncReplyMessageProcessor = new InternalAsyncReplyMessageProcessor();

    protected final ConcurrentMap locks = new ConcurrentHashMap();
    protected final ConcurrentMap responseEvents = new ConcurrentHashMap();

    public static final int MAX_PROCESSED_GROUPS = 50000;

    protected final Object processedLock = new Object();

    // @GuardedBy groupsLock
    protected final BoundedFifoBuffer processed = new BoundedFifoBuffer(MAX_PROCESSED_GROUPS);

    public DefaultAsyncReplyInterceptingMessageProcessor(long timeout)
    {
        this.timeout = timeout;
    }

    public void setReplySource(MessageSource messageSource)
    {
        replyMessageSource = messageSource;
        messageSource.setListener(internalAsyncReplyMessageProcessor);
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (replyMessageSource == null)
        {
            return processNext(event);
        }
        else
        {
            locks.put(getAsyncReplyCorrelationId(event), new Latch());

            // Send one-way requert
            processNext(event);

            // Receive one-way async-reply response
            return processAsyncReply(event);
        }
    }

    protected String getAsyncReplyCorrelationId(MuleEvent event)
    {
        return messageInfoMapping.getCorrelationId(event.getMessage());
    }

    protected MuleEvent processAsyncReply(MuleEvent event) throws ResponseTimeoutException
    {
        String asyncReplyCorrelationId = getAsyncReplyCorrelationId(event);
        Latch asyncReplyLatch = (Latch) locks.get(asyncReplyCorrelationId);
        // flag for catching the interrupted status of the Thread waiting for a
        // result
        boolean interruptedWhileWaiting = false;
        boolean resultAvailable = false;
        MuleEvent result = null;

        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Waiting for async reply message with id: " + asyncReplyCorrelationId);
            }
            // how long should we wait for the lock?
            if (timeout <= 0)
            {
                asyncReplyLatch.await();
                resultAvailable = true;
            }
            else
            {
                resultAvailable = asyncReplyLatch.await(timeout, TimeUnit.MILLISECONDS);
            }
        }
        catch (InterruptedException e)
        {
            interruptedWhileWaiting = true;
        }
        finally
        {
            locks.remove(asyncReplyCorrelationId);
            result = (MuleEvent) responseEvents.remove(asyncReplyCorrelationId);
            if (interruptedWhileWaiting)
            {
                Thread.currentThread().interrupt();
            }
        }

        if (interruptedWhileWaiting)
        {
            Thread.currentThread().interrupt();
        }

        if (resultAvailable)
        {
            if (result == null)
            {
                // this should never happen, just using it as a safe guard for now
                throw new IllegalStateException("Response MuleEvent is null");
            }
            return result;
        }
        else
        {
            addProcessed(asyncReplyCorrelationId);

            event.getMuleContext().fireNotification(
                new RoutingNotification(event.getMessage(), null, RoutingNotification.ASYNC_REPLY_TIMEOUT));

            throw new ResponseTimeoutException(CoreMessages.responseTimedOutWaitingForId((int) timeout,
                asyncReplyCorrelationId), event.getMessage(), null);
        }
    }

    protected void addProcessed(Object id)
    {
        synchronized (processedLock)
        {
            if (processed.isFull())
            {
                processed.remove();
            }
            processed.add(id);
        }
    }

    protected boolean isAlreadyProcessed(Object id)
    {
        synchronized (processedLock)
        {
            return processed.contains(id);
        }
    }

    class InternalAsyncReplyMessageProcessor implements MessageProcessor
    {
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            String messageId = messageInfoMapping.getCorrelationId(event.getMessage());

            if (isAlreadyProcessed(messageId))
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("An event was received for an event group that has already been processed, "
                                 + "this is probably because the async-reply timed out. Correlation Id is: "
                                 + messageId + ". Dropping event");
                }
                // Fire a notification to say we received this message
                event.getMuleContext().fireNotification(
                    new RoutingNotification(event.getMessage(), event.getEndpoint()
                        .getEndpointURI()
                        .toString(), RoutingNotification.MISSED_ASYNC_REPLY));
                return null;
            }

            addProcessed(messageId);
            MuleEvent previousResult = (MuleEvent) responseEvents.putIfAbsent(messageId, event);
            if (previousResult != null)
            {
                // this would indicate that we need a better way to prevent
                // continued aggregation for a group that is currently being
                // processed. Can this actually happen?
                throw new IllegalStateException("Detected duplicate result message with id: " + messageId);
            }
            Latch l = (Latch) locks.get(messageId);
            if (l != null)
            {
                l.countDown();
            }
            else
            {
                logger.warn("Unexpected  message with id " + messageId
                            + " received.   This message will be discarded.");
            }
            return null;
        }
    }

}
