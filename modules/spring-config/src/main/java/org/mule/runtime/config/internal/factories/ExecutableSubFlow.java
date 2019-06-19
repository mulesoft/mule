/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.applyWithChildContext;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 * Reactor chain that wraps a SubFlow execution
 * 
 * @Since 4.3.0
 */
class ExecutableSubFlow {

  private final Publisher<CoreEvent> publisher;
  private final FluxSinkRecorder recorder;

  public ExecutableSubFlow(Processor processor, ComponentLocation flowRefLocation) {
    recorder = new FluxSinkRecorder();
    publisher = applyWithChildContext(Flux.create(recorder), processor, ofNullable(flowRefLocation));
  }

  public void execute(CoreEvent event) {
    recorder.next(event);
  }

  public void complete() {
    recorder.complete();
  }

  public Publisher<CoreEvent> getPublisher() {
    return publisher;
  }
}
