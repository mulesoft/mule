/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.chain;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.empty;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.component.AbstractComponent.ROOT_CONTAINER_NAME_KEY;
import static org.mule.runtime.core.api.config.DefaultMuleConfiguration.isFlowTrace;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ContextClassloaderAwareProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import org.reactivestreams.Publisher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

/**
 * Constructs a custom chain for subflows using the subflow name as the chain name.
 */
public class SubflowMessageProcessorChainBuilder extends DefaultMessageProcessorChainBuilder implements Component {

  private volatile Map<QName, Object> annotations = emptyMap();

  private final Object rootContainerLocationInitLock = new Object();
  private volatile Location rootContainerLocation;

  @Override
  public Object getAnnotation(QName qName) {
    return annotations.get(qName);
  }

  @Override
  public Map<QName, Object> getAnnotations() {
    return unmodifiableMap(annotations);
  }

  @Override
  public void setAnnotations(Map<QName, Object> newAnnotations) {
    annotations = new HashMap<>(newAnnotations);
  }

  @Override
  public ComponentLocation getLocation() {
    return (ComponentLocation) getAnnotation(LOCATION_KEY);
  }

  @Override
  public Location getRootContainerLocation() {
    if (rootContainerLocation == null) {
      synchronized (rootContainerLocationInitLock) {
        if (rootContainerLocation == null) {
          String rootContainerName = (String) getAnnotation(ROOT_CONTAINER_NAME_KEY);
          if (rootContainerName == null) {
            rootContainerName = getLocation().getRootContainerName();
          }
          this.rootContainerLocation = Location.builder().globalName(rootContainerName).build();
        }
      }
    }
    return rootContainerLocation;
  }

  @Override
  protected MessageProcessorChain createInterceptingChain(Processor head, List<Processor> processors,
                                                          List<Processor> processorForLifecycle) {
    return new SubFlowMessageProcessorChain(name, head, processors, processorForLifecycle);
  }

  /**
   * Generates message processor identifiers specific for subflows.
   */
  static class SubFlowMessageProcessorChain extends DefaultMessageProcessorChain implements ContextClassloaderAwareProcessor {

    private final String subFlowName;

    SubFlowMessageProcessorChain(String name, Processor head, List<Processor> processors,
                                 List<Processor> processorsForLifecycle) {
      super(name, empty(), head, processors, processorsForLifecycle);
      this.subFlowName = name;
    }

    private Consumer<CoreEvent> pushSubFlowFlowStackElement() {
      return event -> {
        if (isFlowTrace()) {
          ((DefaultFlowCallStack) event.getFlowCallStack()).push(new FlowStackElement(subFlowName, null));
        }
      };
    }

    private Consumer<CoreEvent> popSubFlowFlowStackElement() {
      return event -> {
        if (isFlowTrace()) {
          ((DefaultFlowCallStack) event.getFlowCallStack()).pop();
        }
      };
    }

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
      return from(publisher)
          .concatMap(event -> just(event)
              .doOnNext(pushSubFlowFlowStackElement())
              .transform(s -> super.apply(s))
              .doOnSuccessOrError((result, throwable) -> popSubFlowFlowStackElement().accept(event)));
    }
  }
}
