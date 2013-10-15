/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.config.i18n.CoreMessages;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Splits a message that has a list payload invoking the next message processor one
 * for each item in the list in order.
 * <p>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/Sequencer.html">http://www.eaipatterns.com/Sequencer.html</a>
 */
public class CollectionSplitter extends AbstractSplitter
{

    protected List<MuleMessage> splitMessage(MuleEvent event)
    {
        MuleMessage message = event.getMessage();
        if (message.getPayload() instanceof Collection)
        {
            return new LinkedList((Collection) message.getPayload());
        }
        else
        {
            throw new IllegalArgumentException(CoreMessages.objectNotOfCorrectType(
                message.getPayload().getClass(), List.class).getMessage());
        }
    }

}
