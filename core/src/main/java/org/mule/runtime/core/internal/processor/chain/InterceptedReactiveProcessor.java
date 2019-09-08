/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.chain;

import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.context.thread.notification.ThreadNotificationLogger;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;

/**
 * Allows to apply processing strategy to processor + previous interceptors while using the processing type of the processor
 * itself.
 *
 * @since 4.1
 */
public final class InterceptedReactiveProcessor implements ReactiveProcessor {

  private final Processor processor;
  private final ReactiveProcessor next;
  private final ProcessingType processingType;
  private final ThreadNotificationLogger threadNotificationLogger;

  public InterceptedReactiveProcessor(Processor processor, ReactiveProcessor next,
                                      ThreadNotificationLogger threadNotificationLogger) {
    this.processor = processor;
    this.processingType = processor.getProcessingType();
    this.next = next;
    this.threadNotificationLogger = threadNotificationLogger;
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> eventPublisher) {
    Flux<CoreEvent> flux = from(eventPublisher);
    if (processor instanceof Component && ((Component) processor).getLocation() != null) {
      flux = flux.checkpoint(((Component) processor).getLocation().getLocation());
    }

    if (threadNotificationLogger != null) {
      return flux
          .doOnNext(event -> threadNotificationLogger.setFinishThread(event.getContext().getId()))
          .transform(publisher -> next.apply(publisher))
          .doOnNext(event -> threadNotificationLogger.setStartingThread(event.getContext().getId()));
    } else {
      return flux
          .transform(publisher -> next.apply(publisher));
    }
  }

  @Override
  public ProcessingType getProcessingType() {
    return processingType;
  }

  public Processor getProcessor() {
    return processor;
  }
}
