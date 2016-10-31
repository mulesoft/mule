/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint.outbound;

import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.message.InternalMessage.Builder;
import org.mule.runtime.core.processor.AbstractRequestResponseMessageProcessor;

import java.io.Serializable;

/**
 * Propagates properties from request message to response message as defined by {@link OutboundEndpoint#getResponseProperties()}.
 * <p>
 * //TODO This can became a standard MessageProcessor in the response chain if/when event has a (immutable) reference to request
 * message.
 */
public class OutboundResponsePropertiesMessageProcessor extends AbstractRequestResponseMessageProcessor {

  private OutboundEndpoint endpoint;

  public OutboundResponsePropertiesMessageProcessor(OutboundEndpoint endpoint) {
    this.endpoint = endpoint;
  }

  @Override
  protected Event processResponse(Event response, final Event request) throws MuleException {
    if (isEventValid(response)) {
      final InternalMessage message = response.getMessage();
      final Builder builder = InternalMessage.builder(message);

      // Properties which should be carried over from the request message
      // to the response message
      for (String propertyName : endpoint.getResponseProperties()) {
        Serializable propertyValue = request.getMessage().getOutboundProperty(propertyName);
        if (propertyValue != null) {
          builder.addOutboundProperty(propertyName, propertyValue);
        }
      }

      response = Event.builder(response).groupCorrelation(request.getGroupCorrelation()).message(builder.build()).build();
    }
    return response;
  }
}
