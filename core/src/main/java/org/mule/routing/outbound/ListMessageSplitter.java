/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
 *
 * Deprecated from 3.6.0.  This functionality is specific to Services.
 */
@Deprecated
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
