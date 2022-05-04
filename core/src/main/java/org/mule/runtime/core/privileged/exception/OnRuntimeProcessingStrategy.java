/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.exception;

import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.privileged.processor.MessageProcessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class OnRuntimeProcessingStrategy implements ProcessingStrategy {

  private static final Logger logger = LoggerFactory.getLogger(OnRuntimeProcessingStrategy.class);

  private final ConfigurationComponentLocator locator;

  public OnRuntimeProcessingStrategy(ConfigurationComponentLocator locator) {
    this.locator = locator;
  }

  @Override
  public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline) {
    return null;
  }

  @Override
  public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
    return publisher -> Flux.from(publisher)
        .flatMap(e -> {
          String location = getFlowName(e);
          return Mono.just(e).transform(getProcessingStrategy(location).map(ps -> ps.onProcessor(processor))
              .orElse(getProcessor(processor, location)));
        });
  }

  private String getFlowName(CoreEvent e) {
    FlowStackElement element = e.getFlowCallStack().peek();
    if (element == null) {
      return "null";
    }
    return element.getFlowName();
  }

  private ReactiveProcessor getProcessor(ReactiveProcessor processor, String location) {
    if (logger.isDebugEnabled()) {
      logger.debug("Processing strategy not found for location {}", location);
    }
    return processor;
  }

  public Optional<ProcessingStrategy> getProcessingStrategy(String location) {
    return MessageProcessors.getProcessingStrategy(locator, Location.builder()
        .globalName(location)
        .build());
  }
}
