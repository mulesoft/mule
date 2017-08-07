/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor;

import static org.mule.runtime.core.api.processor.MessageProcessors.processToApply;
import static reactor.core.publisher.Flux.error;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.api.processor.Processor;

import org.reactivestreams.Publisher;

/**
 * Abstract {@link InterceptingMessageProcessor} that can be easily be extended and used for filtering message flow through a
 * {@link Processor} chain. The default behaviour when the filter is not accepted is to return the request event.
 */
// TODO(pablo.kraan): MULE-13154: this class has to be moved to compatibility
public abstract class AbstractFilteringMessageProcessor extends AbstractInterceptingMessageProcessor {

  /**
   * Throw a FilterUnacceptedException when a message is rejected by the filter?
   */
  protected boolean throwOnUnaccepted = false;
  protected boolean onUnacceptedFlowConstruct;


  /**
   * The <code>MessageProcessor</code> that should be used to handle messages that are not accepted by the filter.
   */
  protected Processor unacceptedMessageProcessor;

  @Override
  public Event process(Event event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<Event> apply(Publisher<Event> publisher) {
    // Optimize to only use concatMap is unaccepted processor is configured.
    if (unacceptedMessageProcessor == null) {
      return from(publisher).<Event>handle((event, sink) -> {
        Builder builder = Event.builder(event);
        boolean accepted;
        try {
          accepted = accept(event, builder);
        } catch (Exception ex) {
          sink.error(filterFailureException(builder.build(), ex));
          return;
        }
        if (accepted) {
          sink.next(builder.build());
        } else {
          if (isThrowOnUnaccepted()) {
            sink.error(filterUnacceptedException(builder.build()));
          } else {
            event.getInternalContext().success();
          }
        }
      }).transform(applyNext());
    } else {
      return from(publisher).concatMap(event -> {
        Builder builder = Event.builder(event);
        try {
          if (accept(event, builder)) {
            return just(event).transform(applyNext());
          } else {
            return just(event).transform(unacceptedMessageProcessor).doFinally(signalType -> event.getInternalContext().success())
                .materialize()
                .then(s -> empty());
          }
        } catch (Exception ex) {
          return error(filterFailureException(builder.build(), ex));
        }
      });
    }
  }

  protected abstract boolean accept(Event event, Event.Builder builder);

  protected MessagingException filterFailureException(Event event, Exception ex) {
    return new MessagingException(event, ex, this);
  }

  protected abstract MuleException filterUnacceptedException(Event event);

  public void setUnacceptedMessageProcessor(Processor unacceptedMessageProcessor) {
    this.unacceptedMessageProcessor = unacceptedMessageProcessor;
    if (unacceptedMessageProcessor instanceof FlowConstruct) {
      onUnacceptedFlowConstruct = true;
    }
  }

  public boolean isThrowOnUnaccepted() {
    return throwOnUnaccepted;
  }

  public void setThrowOnUnaccepted(boolean throwOnUnaccepted) {
    this.throwOnUnaccepted = throwOnUnaccepted;
  }
}
