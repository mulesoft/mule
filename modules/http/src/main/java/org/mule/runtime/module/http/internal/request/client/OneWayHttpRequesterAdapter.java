/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.request.client;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.processor.Processor;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * Adapts an HTTP operation to be one-way.
 */
public class OneWayHttpRequesterAdapter implements Processor, FlowConstructAware {

  private Processor httpRequester;


  public OneWayHttpRequesterAdapter(final Processor httpRequester) {
    this.httpRequester = httpRequester;
  }

  @Override
  public Event process(Event event) throws MuleException {
    final Event result = this.httpRequester.process(event);
    consumePayload(event, result);
    return event;
  }

  private void consumePayload(Event event, Event result) throws MessagingException {
    final Object payload = result.getMessage().getPayload().getValue();
    if (payload instanceof InputStream) {
      try {
        IOUtils.toByteArray((InputStream) payload);
      } catch (IOException e) {
        throw new MessagingException(event, e, this);
      }
    }
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    if (httpRequester instanceof FlowConstructAware) {
      ((FlowConstructAware) httpRequester).setFlowConstruct(flowConstruct);
    }
  }
}
