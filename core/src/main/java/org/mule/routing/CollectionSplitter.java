/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.routing.outbound.AbstractMessageSequenceSplitter;
import org.mule.util.collection.EventToMessageSequenceSplittingStrategy;
import org.mule.util.collection.SplittingStrategy;

/**
 * Splits a message that has a Collection, Iterable, MessageSequence or Iterator
 * payload invoking the next message processor one
 * for each item in it.
 * <p>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/Sequencer.html">http
 * ://www.eaipatterns.com/Sequencer.html</a>
 */
public class CollectionSplitter extends AbstractMessageSequenceSplitter
{
    
    private SplittingStrategy<MuleEvent, MessageSequence<?>> strategy = new EventToMessageSequenceSplittingStrategy();
    
    protected MessageSequence<?> splitMessageIntoSequence(MuleEvent event)
    {
        return this.strategy.split(event);
    }

    @Override
    protected void setMessageCorrelationId(MuleMessage message, String correlationId, int correlationSequence)
    {
        message.setCorrelationId(correlationId);
    }
}
