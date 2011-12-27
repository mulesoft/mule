/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.requestreply;

import org.mule.DefaultMuleEvent;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessageCollection;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.RequestReplyRequesterMessageProcessor;
import org.mule.api.routing.ResponseTimeoutException;
import org.mule.api.source.MessageSource;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.RoutingNotification;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.util.ObjectUtils;
import org.mule.util.concurrent.Latch;

import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentMap;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

import org.apache.commons.collections.buffer.BoundedFifoBuffer;

public abstract class AbstractAsyncRequestReplyRequester extends AbstractInterceptingMessageProcessor
    implements RequestReplyRequesterMessageProcessor, FlowConstructAware
{
    public static final int MAX_PROCESSED_GROUPS = 50000;

    protected volatile long timeout = -1;
    protected volatile boolean failOnTimeout = true;
    protected MessageSource replyMessageSource;
    protected FlowConstruct flowConstruct;
    private final MessageProcessor internalAsyncReplyMessageProcessor = new InternalAsyncReplyMessageProcessor();

    @SuppressWarnings("unchecked")
    protected final Map<String, Latch> locks = new ConcurrentHashMap();
    
    protected final ConcurrentMap responseEvents = new ConcurrentHashMap();
    protected final Object processedLock = new Object();
    // @GuardedBy processedLock
    protected final BoundedFifoBuffer processed = new BoundedFifoBuffer(MAX_PROCESSED_GROUPS);

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (replyMessageSource == null)
        {
            return processNext(event);
        }
        else
        {
            locks.put(getAsyncReplyCorrelationId(event), new Latch());

            sendAsyncRequest(event);

            return receiveAsyncReply(event);
        }
    }

    public void setTimeout(long timeout)
    {
        this.timeout = timeout;
    }

    public void setFailOnTimeout(boolean failOnTimeout)
    {
        this.failOnTimeout = failOnTimeout;
    }

    public void setReplySource(MessageSource messageSource)
    {
        verifyReplyMessageSource(messageSource);
        replyMessageSource = messageSource;
        messageSource.setListener(internalAsyncReplyMessageProcessor);
    }

    protected void verifyReplyMessageSource(MessageSource messageSource)
    {
        // template method
    }

    protected String getAsyncReplyCorrelationId(MuleEvent event)
    {
        if (event.getMessage() instanceof MuleMessageCollection)
        {
            return event.getMessage().getCorrelationId();
        }
        else
        {
            return event.getFlowConstruct().getMessageInfoMapping().getCorrelationId(event.getMessage());
        }
    }

    protected void sendAsyncRequest(MuleEvent event) throws MuleException
    {
        processNext(event);
    }
    
    protected MuleEvent receiveAsyncReply(MuleEvent event) throws ResponseTimeoutException
    {
        String asyncReplyCorrelationId = getAsyncReplyCorrelationId(event);
        Latch asyncReplyLatch = locks.get(asyncReplyCorrelationId);
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
            if (!resultAvailable)
            {
                postLatchAwait(asyncReplyCorrelationId);
                resultAvailable = asyncReplyLatch.getCount() == 0;
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

            // Merge async-reply session properties with exiting session properties
            event.getSession().merge(result.getSession());
            
            // Copy event because the async-reply message was received by a different
            // receiver thread (or the senders dispatcher thread in case of vm
            // with queueEvents="false") and the current thread may need to mutate
            // the even. See MULE-4370
            if (result != null)
            {
                result = RequestContext.setEvent(new DefaultMuleEvent(result.getMessage(), event));
            }
            return result;
        }
        else
        {
            addProcessed(asyncReplyCorrelationId);

            if (failOnTimeout)
            {
                event.getMuleContext()
                    .fireNotification(
                        new RoutingNotification(event.getMessage(), null,
                            RoutingNotification.ASYNC_REPLY_TIMEOUT));

                throw new ResponseTimeoutException(CoreMessages.responseTimedOutWaitingForId((int) timeout,
                    asyncReplyCorrelationId), event, null);
            }
            else
            {
                return null;
            }
        }
    }

    protected void postLatchAwait(String asyncReplyCorrelationId)
    {
        // Template method
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
            String messageId = getAsyncReplyCorrelationId(event);

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
            Latch l = locks.get(messageId);
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

    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }
    
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }
}
