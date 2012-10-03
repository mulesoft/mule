/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorContainer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *  Contains useful methods for the generation of message processor identifiers used by the notification system
 */
public class NotificationUtils
{

    private NotificationUtils()
    {
    }

    public static Map<MessageProcessor, String> buildMessageProcessorPaths(List<MessageProcessor> processors)
    {
        return buildMessageProcessorPaths(processors, null);
    }

    public static Map<MessageProcessor, String> buildMessageProcessorPaths(List<MessageProcessor> processors, String basePath)
    {
        if (basePath == null)
        {
            basePath = "/";
        }
        if (!basePath.startsWith("/"))
        {
            basePath = "/" + basePath;
        }
        if (!basePath.endsWith("/"))
        {
            basePath += "/";
        }
        Map<MessageProcessor, String> result = new LinkedHashMap<MessageProcessor, String>();
        int index = 0;
        for (MessageProcessor mp : processors)
        {
            String prefix = basePath + index;
            result.put(mp, prefix);
            if (mp instanceof MessageProcessorContainer)
            {
                Map<MessageProcessor, String> children = ((MessageProcessorContainer) mp).getMessageProcessorPaths();
                prefixMessageProcessorPaths(prefix, children);
                result.putAll(children);
            }
            index++;
        }
        return result;
    }

    public static void prefixMessageProcessorPaths(String prefix, Map<MessageProcessor, String> pathMap)
    {
        if (prefix.endsWith("/"))
        {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        for (Map.Entry entry : pathMap.entrySet())
        {
            entry.setValue(prefix + entry.getValue());
        }
    }
}
