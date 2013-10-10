/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.outbound;

import org.mule.api.MuleMessage;
import org.mule.config.i18n.CoreMessages;

import java.util.LinkedList;
import java.util.List;

/**
 * <code>FilteringListMessageSplitter</code> accepts a List as a message payload
 * then routes list elements as messages over an endpoint where the endpoint's filter
 * accepts the payload.
 */
public class ListMessageSplitter extends AbstractRoundRobinMessageSplitter
{
    public ListMessageSplitter()
    {
        setDisableRoundRobin(true);
    }

    @Override
    protected List splitMessage(MuleMessage message)
    {
        if (message.getPayload() instanceof List)
        {
            return new LinkedList((List) message.getPayload());
        }
        else
        {
            throw new IllegalArgumentException(CoreMessages.objectNotOfCorrectType(
                    message.getPayload().getClass(), List.class).getMessage());
        }
    }
}
