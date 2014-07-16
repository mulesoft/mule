/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.config.i18n.CoreMessages;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Splits a message that has a map payload invoking the next message processor one
 * for each item in the map in order. The Map entry value is used as the new payload
 * and the map key is set as a message property with the following property name
 * 'key'.
 * <p>
 * <b>EIP Reference:</b> <a
 * href="http://www.eaipatterns.com/Sequencer.html">http://www
 * .eaipatterns.com/Sequencer.html</a>
 */
public class MapSplitter extends AbstractSplitter
{
    public static String MAP_ENTRY_KEY = "key";

    protected List<MuleMessage> splitMessage(MuleEvent event)
    {
        MuleMessage message = event.getMessage();
        if (message.getPayload() instanceof Map<?, ?>)
        {
            List<MuleMessage> list = new LinkedList<MuleMessage>();
            Set<Map.Entry<?, ?>> set = ((Map) message.getPayload()).entrySet();
            for (Entry<?, ?> entry : set)
            {
                MuleMessage splitMessage = new DefaultMuleMessage(entry.getValue(), muleContext);
                splitMessage.setInvocationProperty(MAP_ENTRY_KEY, entry.getKey());
                list.add(splitMessage);
            }
            return list;
        }
        else
        {
            throw new IllegalArgumentException(CoreMessages.objectNotOfCorrectType(
                message.getPayload().getClass(), Map.class).getMessage());
        }
    }
}
