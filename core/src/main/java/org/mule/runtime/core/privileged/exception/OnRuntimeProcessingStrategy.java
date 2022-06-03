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

import static java.util.Optional.empty;

/**
 * A {@link ProcessingStrategy} used for {@link org.mule.runtime.core.internal.exception.GlobalErrorHandler}. The resolution of
 * the processing strategy is done in runtime depending on the flow location where the error that is being handled was thrown.
 *
 * @since 4.3.0
 */
public class OnRuntimeProcessingStrategy implements ProcessingStrategy {

  private static final Logger logger = LoggerFactory.getLogger(OnRuntimeProcessingStrategy.class);

  private final ConfigurationComponentLocator locator;

  public OnRuntimeProcessingStrategy(ConfigurationComponentLocator locator) {
    this.locator = locator;
  }

  @Override
  public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline) {
    throw new UnsupportedOperationException("This processing strategy shouldn't create any sinks, it uses the processing strategy from the parent flow.");
  }

  @Override
  public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
    return publisher -> Flux.from(publisher)
        .flatMap(e -> {
          Optional<ProcessingStrategy> processingStrategy = getProcessingStrategy(getParentFlowNameForErrorHandler(e));
          return Mono.just(e).transform(processingStrategy.map(ps -> ps.onProcessor(processor))
              .orElse(processor));
        });
  }

  private String getParentFlowNameForErrorHandler(CoreEvent e) {
    FlowStackElement element = e.getFlowCallStack().peek();
    if (element == null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Parent flow couldn't be resolved.");
      }
      return null;
    }
    return element.getFlowName();
  }

  public Optional<ProcessingStrategy> getProcessingStrategy(String location) {
    if (location == null) {
      return empty();
    }
    Optional<ProcessingStrategy> processingStrategy = MessageProcessors.getProcessingStrategy(locator, Location.builder()
        .globalName(location)
        .build());
    if (!processingStrategy.isPresent() && logger.isDebugEnabled()) {
      logger.debug("Processing strategy not found for location {}", location);
    }
    return processingStrategy;
  }
}
