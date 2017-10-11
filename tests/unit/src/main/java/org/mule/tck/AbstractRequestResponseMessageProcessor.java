/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import static org.mule.runtime.core.api.rx.Exceptions.checkedFunction;
import static org.mule.runtime.core.internal.util.rx.Operators.nullSafeMap;
import static org.mule.runtime.core.privileged.event.PrivilegedEvent.setCurrentEvent;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.processor.AbstractInterceptingMessageProcessor;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Base implementation of a {@link org.mule.runtime.core.api.processor.Processor} that may performs processing during both the
 * request and response processing phases while supporting non-blocking execution.
 * <p>
 * In order to define the process during the request phase you should override the
 * {@link #processRequest(CoreEvent)} method. Symmetrically, if you need to define a process to be executed
 * during the response phase, then you should override the {@link #processResponse(CoreEvent)} method.
 * <p>
 * In some cases you'll have some code that should be always executed, even if an error occurs, for those cases you should
 * override the {@link #processFinally(CoreEvent, MessagingException)} method.
 *
 * @since 3.7.0
 */
public abstract class AbstractRequestResponseMessageProcessor extends AbstractInterceptingMessageProcessor {

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    Flux<CoreEvent> flux = from(publisher).transform(processRequest());
    if (next != null) {
      flux = flux.transform(applyNext());
    }
    return flux.transform(processResponse())
        .doOnNext(result -> processFinally(result, null))
        .onErrorResume(MessagingException.class, exception -> {
          try {
            return Mono.just(processCatch(exception.getEvent(), exception));
          } catch (MessagingException me) {
            return Mono.error(me);
          } finally {
            processFinally(exception.getEvent(), exception);
          }
        });
  }

  /**
   * Processes the request phase before the next message processor is invoked.
   *
   * @param request event to be processed.
   * @return result of request processing.
   * @throws MuleException exception thrown by implementations of this method whiile performing response processing
   */
  protected CoreEvent processRequest(CoreEvent request) throws MuleException {
    return request;
  }

  /**
   * Processes the request phase before the next message processor is invoked.
   *
   * @return function that performs request processing
   */
  protected ReactiveProcessor processRequest() {
    return stream -> from(stream).cast(PrivilegedEvent.class).map(event -> {
      try {
        setCurrentEvent(event);
        return processRequest(event);
      } catch (MuleException e) {
        throw propagate(e);
      }
    });
  }

  /**
   * Processes the response phase after the next message processor and it's response phase have been invoked
   *
   * @param response response event to be processed.
   * @return result of response processing.
   * @throws MuleException exception thrown by implementations of this method whiile performing response processing
   */
  protected CoreEvent processResponse(CoreEvent response) throws MuleException {
    return response;
  }

  /**
   * Processes the response phase after the next message processor and it's response phase have been invoked
   *
   * @return function that performs request processing
   */
  protected ReactiveProcessor processResponse() {
    return stream -> from(stream).handle(nullSafeMap(checkedFunction(response -> processResponse(response))));
  }

  /**
   * Used to perform post processing after both request and response phases have been completed. This method will be invoked both
   * when processing is successful as well as if an exception is thrown. successful result and in the case of an exception being
   * thrown.
   *
   * @param event the result of request and response processing. Note that this includes the request and response processing of
   *        the rest of the Flow following this message processor too.
   * @param exception the exception thrown during processing if any. If not exception was thrown then this parameter is null
   */
  protected void processFinally(CoreEvent event, MessagingException exception) {

  }

  protected CoreEvent processCatch(CoreEvent event, MessagingException exception) throws MessagingException {
    throw exception;
  }

}
