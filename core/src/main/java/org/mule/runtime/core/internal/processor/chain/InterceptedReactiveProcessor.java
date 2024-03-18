/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.chain;

import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;

/**
 * Allows to apply processing strategy to processor + previous interceptors while using the processing type of the processor
 * itself.
 *
 * @since 4.1
 */
public final class InterceptedReactiveProcessor implements ReactiveProcessor {

  private final ReactiveProcessor processor;
  private final ReactiveProcessor next;
  private final ProcessingType processingType;

  public InterceptedReactiveProcessor(ReactiveProcessor processor, ReactiveProcessor next) {
    this.processor = processor;
    this.processingType = processor.getProcessingType();
    this.next = next;
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> eventPublisher) {
    Flux<CoreEvent> flux = from(eventPublisher);
    if (processor instanceof Component && ((Component) processor).getLocation() != null) {
      flux = flux.checkpoint(((Component) processor).getLocation().getLocation());
    }

    return flux.transform(next);
  }

  @Override
  public ProcessingType getProcessingType() {
    return processingType;
  }

  public ReactiveProcessor getProcessor() {
    return processor;
  }

  @Override
  public String toString() {
    return (processor instanceof Component)
        ? ((Component) processor).getLocation().getLocation()
        : processor.toString();
  }
}
