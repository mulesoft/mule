/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.routing.outbound.AbstractMessageSequenceSplitter;
import org.mule.routing.outbound.CollectionMessageSequence;

import java.util.List;

/**
 * Splits a message invoking the next message processor one for each split part.
 * Implementations must implement {@link #splitMessage(MuleEvent)} and determine how
 * the message is split.
 * <p>
 * <b>EIP Reference:</b> <a
 * href="http://www.eaipatterns.com/Sequencer.html">http://www
 * .eaipatterns.com/Sequencer.html</a>
 */

public abstract class AbstractSplitter extends AbstractMessageSequenceSplitter
{
    @Override
    @SuppressWarnings("unchecked")
    protected MessageSequence<?> splitMessageIntoSequence(MuleEvent event) throws MuleException
    {
        return new CollectionMessageSequence(splitMessage(event));
    }

    protected abstract List<MuleMessage> splitMessage(MuleEvent event) throws MuleException;

}
