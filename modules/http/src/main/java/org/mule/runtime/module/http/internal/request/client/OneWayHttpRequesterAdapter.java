/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.request.client;

import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.processor.MessageProcessor;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * Adapts an HTTP operation to be one-way.
 */
public class OneWayHttpRequesterAdapter implements MessageProcessor, FlowConstructAware {

  private MessageProcessor httpRequester;


  public OneWayHttpRequesterAdapter(final MessageProcessor httpRequester) {
    this.httpRequester = httpRequester;
  }

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    final MuleEvent result = this.httpRequester.process(event);
    consumePayload(event, result);
    return VoidMuleEvent.getInstance();
  }

  private void consumePayload(MuleEvent event, MuleEvent result) throws MessagingException {
    final Object payload = result.getMessage().getPayload();
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
