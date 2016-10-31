/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint.inbound;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_ORIGINATING_ENDPOINT_PROPERTY;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.util.ObjectUtils;
import org.mule.runtime.core.util.StringUtils;

;


/**
 * Sets the inbound endpoint uri on as a property of the message using the following key:
 * {@link MuleProperties#MULE_ORIGINATING_ENDPOINT_PROPERTY}.
 */
public class InboundEndpointPropertyMessageProcessor implements Processor {

  private InboundEndpoint endpoint;

  public InboundEndpointPropertyMessageProcessor(InboundEndpoint endpoint) {
    this.endpoint = endpoint;
  }

  @Override
  public Event process(Event event) throws MuleException {
    // If the endpoint has a logical name, use it, otherwise use the URI.
    String inboundEndpoint = endpoint.getName();

    if (StringUtils.isBlank(inboundEndpoint)) {
      // URI
      inboundEndpoint = endpoint.getEndpointURI().getUri().toString();
    }
    return Event.builder(event).message(InternalMessage.builder(event.getMessage())
        .addInboundProperty(MULE_ORIGINATING_ENDPOINT_PROPERTY, inboundEndpoint).build()).build();
  }

  @Override
  public String toString() {
    return ObjectUtils.toString(this);
  }
}
