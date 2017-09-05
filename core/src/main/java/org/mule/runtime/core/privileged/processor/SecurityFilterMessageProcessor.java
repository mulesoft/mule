/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.processor.MessageProcessors.processToApply;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.security.SecurityFilter;

import org.reactivestreams.Publisher;

/**
 * Filters the flow using the specified {@link SecurityFilter}. If unauthorised the flow is stopped and therefore the message is
 * not send or dispatched by the transport. When unauthorised the request message is returned as the response.
 */
public class SecurityFilterMessageProcessor extends AbstractAnnotatedObject
    implements Processor, Initialisable, MuleContextAware {

  private MuleContext muleContext;

  private SecurityFilter filter;

  public SecurityFilterMessageProcessor(SecurityFilter filter) {
    this.filter = filter;
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

  public SecurityFilter getFilter() {
    return filter;
  }

  @Override
  public InternalEvent process(InternalEvent event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<InternalEvent> apply(Publisher<InternalEvent> publisher) {
    return from(publisher).map(event -> {
      try {
        return filter.doFilter(event);
      } catch (Exception e) {
        throw propagate(e);
      }
    });
  }

  public void setFilter(SecurityFilter filter) {
    this.filter = filter;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  public MuleContext getMuleContext() {
    return muleContext;
  }
}
