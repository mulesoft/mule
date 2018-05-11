/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.mule.api.processor.InternalMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorContainer;
import org.mule.api.processor.MessageProcessorPathElement;
import org.mule.processor.chain.DynamicMessageProcessorContainer;
import org.mule.processor.chain.InterceptingChainLifecycleWrapperPathSkip;

/**
 * Contains useful methods for the generation of message processor identifiers used by the notification system
 */
public class NotificationUtils
{
    public interface PathResolver
    {
        String resolvePath(MessageProcessor processor);
    }

    public static class FlowMap implements PathResolver
    {
        private Map<MessageProcessor, String> flowMap = new ConcurrentHashMap<MessageProcessor, String>();
        private Map<DynamicMessageProcessorContainer, String> flowDynamicsMap = new ConcurrentHashMap<DynamicMessageProcessorContainer, String>();

        public FlowMap(Map<MessageProcessor, String> paths)
        {
            flowMap.putAll(paths);
            populateDynamicsMap(paths);
        }

        @Override
        public String resolvePath(MessageProcessor processor)
        {
            String path = flowMap.get(processor);
            if (path != null)
            {
                return path;
            }

            if(flowDynamicsMap.isEmpty())
            {
                return null;
            }
            else
            {
                for (DynamicMessageProcessorContainer flowMapEntry : flowDynamicsMap.keySet())
                {
                    FlowMap resolvedInnerPaths = ((DynamicMessageProcessorContainer) flowMapEntry).buildInnerPaths();
                    if (resolvedInnerPaths != null)
                    {
                        Map<MessageProcessor, String> paths = resolvedInnerPaths.getFlowMap();
                        flowMap.putAll(paths);
                        populateDynamicsMap(paths);
                    }
                }
                return flowMap.get(processor);
            }
        }

        private void populateDynamicsMap(Map<MessageProcessor, String> paths)
        {
            for (Entry<MessageProcessor, String> flowMapEntry : paths.entrySet())
            {
                if (flowMapEntry.getKey() instanceof DynamicMessageProcessorContainer)
                {
                    flowDynamicsMap.put((DynamicMessageProcessorContainer) flowMapEntry.getKey(), flowMapEntry.getValue());
                }                
            }
        }

        public Collection<String> getAllPaths()
        {
            return flowMap.values();
        }

        public Map<MessageProcessor, String> getFlowMap()
        {
            return flowMap;
        }
    }

    private NotificationUtils()
    {
    }

    public static void addMessageProcessorPathElements(List<MessageProcessor> processors, MessageProcessorPathElement parentElement)
    {
        if (processors == null || parentElement == null)
        {
            return;
        }
        for (MessageProcessor mp : processors)
        {
            if (!(mp instanceof InternalMessageProcessor))
            {
                MessageProcessorPathElement messageProcessorPathElement;

                // To avoid adding a level in some path elements:
                if (mp instanceof InterceptingChainLifecycleWrapperPathSkip)
                {
                    messageProcessorPathElement = parentElement;
                }
                else
                {
                    messageProcessorPathElement = parentElement.addChild(mp);
                }
                if (messageProcessorPathElement != null && mp instanceof MessageProcessorContainer)
                {
                    ((MessageProcessorContainer) mp).addMessageProcessorPathElements(messageProcessorPathElement);
                }
            }
        }

    }

    /**
     * @param element where to get the paths from.
     * @return a resolver for the elements corresponding to <b>element</b>.
     */
    public static FlowMap buildPathResolver(MessageProcessorPathElement element)
    {
        return new FlowMap(buildPaths(element, new LinkedHashMap<MessageProcessor, String>()));
    }

    /**
     * @deprecated Use {@link #buildPathResolver(MessageProcessorPathElement)} instead.
     * @param element where to get the paths from.
     * @return the element paths corresponding to <b>element</b>.
     */
    @Deprecated
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
