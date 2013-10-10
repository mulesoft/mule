/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
