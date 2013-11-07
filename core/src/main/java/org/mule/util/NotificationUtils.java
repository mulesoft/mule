/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import org.mule.api.processor.InternalMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorContainer;
import org.mule.api.processor.MessageProcessorPathElement;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains useful methods for the generation of message processor identifiers used by the notification system
 */
public class NotificationUtils
{

    private NotificationUtils()
    {
    }

    public static void addMessageProcessorPathElements(List<MessageProcessor> processors, MessageProcessorPathElement parentElement)
    {
        if (processors == null)
        {
            return;
        }
        for (MessageProcessor mp : processors)
        {
            if (!(mp instanceof InternalMessageProcessor))
            {

                MessageProcessorPathElement messageProcessorPathElement = parentElement.addChild(mp);
                if (mp instanceof MessageProcessorContainer)
                {
                    ((MessageProcessorContainer) mp).addMessageProcessorPathElements(messageProcessorPathElement);
                }
            }

        }

    }


    public static Map<MessageProcessor, String> buildPaths(MessageProcessorPathElement element)
    {
        return buildPaths(element, new LinkedHashMap<MessageProcessor, String>());
    }

    private static Map<MessageProcessor, String> buildPaths(MessageProcessorPathElement element, Map<MessageProcessor, String> elements)
    {
        if (element.getMessageProcessor() != null)
        {
            elements.put(element.getMessageProcessor(), element.getPath());
        }
        List<MessageProcessorPathElement> children = element.getChildren();
        for (MessageProcessorPathElement child : children)
        {
            buildPaths(child, elements);
        }
        return elements;
    }


}
