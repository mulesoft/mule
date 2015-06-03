/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.DefaultMuleEvent;
import org.mule.OptimizedRequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.NonBlockingSupported;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.api.transport.ReplyToHandler;
import org.mule.processor.AbstractFilteringMessageProcessor;
import org.mule.processor.AbstractMessageProcessorOwner;
import org.mule.util.ObjectUtils;

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The <code>WireTap</code> MessageProcessor allows inspection of messages in a flow.
 * <p>
 * The incoming message is is sent to both the primary and wiretap outputs. The flow
 * of the primary output will be unmodified and a copy of the message used for the
 * wiretap output.
 * <p>
 * An optional filter can be used to filter which message are sent to the wiretap
 * output, this filter does not affect the flow to the primary output. If there is an
 * error sending to the wiretap output no exception will be thrown but rather an
 * error logged.
 * <p>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/WireTap.html">http://www.eaipatterns.com/WireTap.html<a/>
 */
public class WireTap extends AbstractMessageProcessorOwner implements MessageProcessor, NonBlockingSupported
{
    protected final transient Log logger = LogFactory.getLog(getClass());
    protected volatile MessageProcessor tap;
    protected volatile Filter filter;

    protected MessageProcessor filteredTap = new WireTapFilter();

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (tap == null)
        {
            return event;
        }

        try
        {
            MuleEvent tapEvent = DefaultMuleEvent.copy(event);
            // Tap should not respond to reply to handler
            // TODO: Combine this with copy once we have MuleEventBuilder to avoid second copy
            tapEvent = new DefaultMuleEvent(tapEvent, (ReplyToHandler) null);
            OptimizedRequestContext.unsafeSetEvent(tapEvent);
            filteredTap.process(tapEvent);
            OptimizedRequestContext.unsafeSetEvent(event);
        }
        catch (MuleException e)
        {
            logger.error("Exception sending to wiretap output " + tap, e);
        }

        return event;
    }

    public MessageProcessor getTap()
    {
        return tap;
    }

    public void setTap(MessageProcessor tap)
    {
        this.tap = tap;
    }

    @Deprecated
    public void setMessageProcessor(MessageProcessor tap)
    {
        setTap(tap);
    }
    
    public Filter getFilter()
    {
        return filter;
    }

    public void setFilter(Filter filter)
    {
        this.filter = filter;
    }

    private class WireTapFilter extends AbstractFilteringMessageProcessor
    {
        @Override
        protected boolean accept(MuleEvent event)
        {
            if (filter == null)
            {
                return true;
            }
            else
            {
                return filter.accept(event.getMessage());
            }
        }

        @Override
        protected MuleEvent processNext(MuleEvent event) throws MuleException
        {
            if (tap != null)
            {
                tap.process(event);
            }
            return null;
        }

        @Override
        public String toString()
        {
            return ObjectUtils.toString(this);
        }
    }

    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }

        @Override
    protected List<MessageProcessor> getOwnedMessageProcessors()
    {
        return Collections.singletonList(tap);
    }

}
