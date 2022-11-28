/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.chain;

import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.component.AbstractComponent.ROOT_CONTAINER_NAME_KEY;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;

import org.mule.runtime.core.api.tracing.customization.ComponentEventBasedInitialSpanInfoProvider;
import org.mule.runtime.core.api.tracing.customization.ComponentExecutionInitialSpanInfo;
import org.mule.runtime.core.api.tracing.customization.SpanInitialInfoUtils;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import org.mule.runtime.core.internal.context.notification.DefaultFlowCallStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.namespace.QName;

import org.mule.runtime.tracer.api.span.info.StartExportInfo;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.reactivestreams.Publisher;

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
  protected MessageProcessorChain createSimpleChain(List<Processor> processors,
                                                    Optional<ProcessingStrategy> processingStrategyOptional) {
    return new SubFlowMessageProcessorChain(name, processors, processingStrategyOptional);
  }

  /**
   * Generates message processor identifiers specific for subflows.
   */
  private static class SubFlowMessageProcessorChain extends DefaultMessageProcessorChain {

    public static final ComponentIdentifier SUB_FLOW = buildFromStringRepresentation("subflow");

    private final String subFlowName;

    SubFlowMessageProcessorChain(String name, List<Processor> processors,
                                 Optional<ProcessingStrategy> processingStrategyOptional) {
      super(name, processingStrategyOptional, processors,
            NullExceptionHandler.getInstance());
      this.subFlowName = name;
      this.setCoreSpanCustomizationInfoProvider(new SubFllowComponentEventBasedInitialSpanInfoProvider(this));
    }

    private void pushSubFlowFlowStackElement(CoreEvent event) {
      ((DefaultFlowCallStack) event.getFlowCallStack())
          .push(new FlowStackElement(subFlowName, SUB_FLOW, null));
    }

    private void popSubFlowFlowStackElement(CoreEvent event) {
      ((DefaultFlowCallStack) event.getFlowCallStack()).pop();
    }

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
      return from(publisher)
          .doOnNext(this::pushSubFlowFlowStackElement)
          // To avoid recursive transformation when there are flowref cycles, the chain is lazily transformed
          .transformDeferred(super::apply)
          .doOnNext(this::popSubFlowFlowStackElement);
    }

    private class SubFllowComponentEventBasedInitialSpanInfoProvider
        extends ComponentEventBasedInitialSpanInfoProvider {

      SubFllowComponentEventBasedInitialSpanInfoProvider(Component component) {
        super(component);
      }

      @Override
      public InitialSpanInfo get(CoreEvent coreEvent) {
        return new SubFlowComponentExecutionInitialSpanInfo(component, coreEvent);
      }

      private class SubFlowComponentExecutionInitialSpanInfo extends ComponentExecutionInitialSpanInfo {

        public SubFlowComponentExecutionInitialSpanInfo(Component component, CoreEvent coreEvent) {
          super(component, coreEvent);
        }

        @Override
        public String getName() {
          return SpanInitialInfoUtils.getSpanName(SubFlowMessageProcessorChain.SUB_FLOW);
        }

        @Override
        public StartExportInfo getStartExportInfo() {
          return StartExportInfo.DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;
        }
      }
    }
  }
}
