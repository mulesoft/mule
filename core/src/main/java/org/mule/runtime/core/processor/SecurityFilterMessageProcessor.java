/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.from;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.security.SecurityFilter;

import org.reactivestreams.Publisher;

/**
 * Filters the flow using the specified {@link SecurityFilter}. If unauthorised the flow is stopped and therefore the message is
 * not send or dispatched by the transport. When unauthorised the request message is returned as the response.
 */
public class SecurityFilterMessageProcessor extends AbstractInterceptingMessageProcessor
    implements Initialisable {

  private SecurityFilter filter;

  /**
   * For IoC only
   * 
   * @deprecated Use SecurityFilterMessageProcessor(SecurityFilter filter) instead
   */
  @Deprecated
  public SecurityFilterMessageProcessor() {
    super();
  }

  @Override
  public void initialise() throws InitialisationException {
    try {
      muleContext.getInjector().inject(filter);
      initialiseIfNeeded(filter, muleContext);
    } catch (MuleException e) {
      throw new InitialisationException(e, this);
    }
  }

  public SecurityFilterMessageProcessor(SecurityFilter filter) {
    this.filter = filter;
  }

  public SecurityFilter getFilter() {
    return filter;
  }

  @Override
  public Event process(Event event) throws MuleException {
    if (filter != null) {
      event = filter.doFilter(event);
    }
    return processNext(event);
  }

  @Override
  public Publisher<Event> apply(Publisher<Event> publisher) {
    if (filter == null) {
      return from(publisher).transform(applyNext());
    } else {
      return from(publisher).doOnNext(event -> {
        try {
          filter.doFilter(event);
        } catch (Exception e) {
          throw propagate(e);
        }
      }).transform(applyNext());
    }
  }

  public void setFilter(SecurityFilter filter) {
    this.filter = filter;
  }

}
