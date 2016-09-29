/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util;

import static java.util.Collections.singletonList;
import static java.util.Collections.synchronizedSet;

import org.mule.runtime.core.api.processor.InternalMessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.MessageProcessorContainer;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.processor.chain.DynamicMessageProcessorContainer;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contains useful methods for the generation of message processor identifiers used by the notification system
 */
public class NotificationUtils {

  public interface PathResolver {

    String resolvePath(Processor processor);
  }

  public static class FlowMap implements PathResolver {

    private Map<Processor, String> flowMap = new ConcurrentHashMap<>();
    // This set allows for dynamic containers to not be analyzed more than once. Dynamic containers cannot be removed because as a
    // container it also must have a path.
    private Set<Processor> resolvedDynamicContainers = synchronizedSet(new HashSet<Processor>());

    public FlowMap(Map<Processor, String> paths) {
      flowMap.putAll(paths);
    }

    @Override
    public String resolvePath(Processor processor) {
      String path = flowMap.get(processor);
      if (path != null) {
        return path;
      } else {
        for (Entry<Processor, String> flowMapEntries : flowMap.entrySet()) {
          if (flowMapEntries.getKey() instanceof DynamicMessageProcessorContainer
              && !resolvedDynamicContainers.contains(flowMapEntries.getKey())) {
            FlowMap resolvedInnerPaths = ((DynamicMessageProcessorContainer) flowMapEntries.getKey()).buildInnerPaths();
            if (resolvedInnerPaths != null) {
              flowMap.putAll(resolvedInnerPaths.getFlowMap());
              resolvedDynamicContainers.add(flowMapEntries.getKey());
            }
          }
        }
        return flowMap.get(processor);
      }
    }

    public Collection<String> getAllPaths() {
      return flowMap.values();
    }

    public Map<Processor, String> getFlowMap() {
      return flowMap;
    }
  }

  private NotificationUtils() {}

  public static void addMessageProcessorPathElements(List<Processor> processors,
                                                     MessageProcessorPathElement parentElement) {
    if (processors == null) {
      return;
    }
    for (Processor mp : processors) {
      addMessageProcessorPathElements(mp, parentElement);
    }
  }

  public static void addMessageProcessorPathElements(Processor processor,
                                                     MessageProcessorPathElement parentElement) {
    if (processor == null || parentElement == null) {
      return;
    }
    if (!(processor instanceof InternalMessageProcessor)) {
      if (processor instanceof MessageProcessorContainer) {
        ((MessageProcessorContainer) processor).addMessageProcessorPathElements(parentElement);
      } else {
        parentElement.addChild(processor);
      }
    }
  }

  /**
   * @param element where to get the paths from.
   * @return a resolver for the elements corresponding to <b>element</b>.
   */
  public static FlowMap buildPathResolver(MessageProcessorPathElement element) {
    return new FlowMap(buildPaths(element, new LinkedHashMap<Processor, String>()));
  }

  /**
   * @deprecated Use {@link #buildPathResolver(MessageProcessorPathElement)} instead.
   * @param element where to get the paths from.
   * @return the element paths corresponding to <b>element</b>.
   */
  @Deprecated
  public static Map<Processor, String> buildPaths(MessageProcessorPathElement element) {
    return buildPaths(element, new LinkedHashMap<Processor, String>());
  }

  private static Map<Processor, String> buildPaths(MessageProcessorPathElement element,
                                                   Map<Processor, String> elements) {
    if (element.getMessageProcessor() != null) {
      elements.put(element.getMessageProcessor(), element.getPath());
    }
    List<MessageProcessorPathElement> children = element.getChildren();
    for (MessageProcessorPathElement child : children) {
      buildPaths(child, elements);
    }
    return elements;
  }


}
