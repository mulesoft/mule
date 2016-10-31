/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint.inbound;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.util.ObjectUtils;
import org.mule.runtime.core.util.StringMessageUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InboundLoggingMessageProcessor implements Processor {

  protected final transient Logger logger = LoggerFactory.getLogger(getClass());
  protected InboundEndpoint endpoint;

  public InboundLoggingMessageProcessor(InboundEndpoint endpoint) {
    this.endpoint = endpoint;
  }

  @Override
  public Event process(Event event) throws MuleException {
    InternalMessage message = event.getMessage();
    if (logger.isDebugEnabled()) {
      logger.debug("Message Received on: " + endpoint.getEndpointURI());
    }
    if (logger.isTraceEnabled()) {
      try {
        logger.trace("Message Payload: \n"
            + StringMessageUtils.truncate(StringMessageUtils.toString(message.getPayload().getValue()), 200, false));
        logger.trace("Message detail: \n" + message.toString());
      } catch (Exception e) {
        // ignore
      }
    }

    return event;
  }

  @Override
  public String toString() {
    return ObjectUtils.toString(this);
  }
}
