/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint.outbound;

import static org.mule.runtime.core.DefaultMuleEvent.setCurrentEvent;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_ENDPOINT_PROPERTY;

import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.util.ObjectUtils;

import java.io.Serializable;

/**
 * Sets the outbound endpoint uri on as a property of the message using the following key:
 * {@link MuleProperties#MULE_ENDPOINT_PROPERTY}.
 */
public class OutboundEndpointPropertyMessageProcessor implements MessageProcessor {

  private String[] ignoredPropertyOverrides = new String[] {MuleProperties.MULE_METHOD_PROPERTY, "Content-Type"};

  private OutboundEndpoint endpoint;

  public OutboundEndpointPropertyMessageProcessor(OutboundEndpoint endpoint) {
    this.endpoint = endpoint;
  }

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    MuleMessage.Builder messageBuilder = MuleMessage.builder(event.getMessage())
        .addOutboundProperty(MULE_ENDPOINT_PROPERTY, endpoint.getEndpointURI().toString());

    if (endpoint.getProperties() != null) {
      for (String prop : endpoint.getProperties().keySet()) {
        Serializable value = endpoint.getProperties().get(prop);

        if ("Content-Type".equalsIgnoreCase(prop)) {
          messageBuilder.mediaType(MediaType.parse(value.toString()));
        } else {
          // don't overwrite property on the message
          if (!ignoreProperty(event.getMessage(), prop)) {
            // inbound endpoint properties are in the invocation scope
            messageBuilder.addOutboundProperty(prop, value);
          }
        }
      }
    }
    event.setMessage(messageBuilder.build());
    setCurrentEvent(event);
    return event;
  }

  protected boolean ignoreProperty(MuleMessage message, String key) {
    if (key == null) {
      return true;
    }

    for (String ignoredPropertyOverride : ignoredPropertyOverrides) {
      if (key.equals(ignoredPropertyOverride)) {
        return false;
      }
    }

    return null != message.getOutboundProperty(key);
  }

  @Override
  public String toString() {
    return ObjectUtils.toString(this);
  }
}
