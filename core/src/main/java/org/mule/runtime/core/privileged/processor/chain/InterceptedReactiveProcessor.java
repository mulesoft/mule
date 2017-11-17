/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor.chain;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

import org.reactivestreams.Publisher;

/**
 * Allows to apply processing strategy to processor + previous interceptors while using the processing type of the processor
 * itself.
 *
 * @since 4.1
 */
public final class InterceptedReactiveProcessor implements ReactiveProcessor {

  private final Processor processor;
  private final ReactiveProcessor next;

  public InterceptedReactiveProcessor(Processor processor, ReactiveProcessor next) {
    this.processor = processor;
    this.next = next;
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> eventPublisher) {
    return next.apply(eventPublisher);
  }

  @Override
  public ProcessingType getProcessingType() {
    return processor.getProcessingType();
  }

  public Processor getProcessor() {
    return processor;
  }
}
