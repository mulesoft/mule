/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint.outbound;

import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.ObjectUtils;


/**
 * Verify that the outbound mime type is acceptable by this endpoint.
 */
public class OutboundEndpointMimeTypeCheckingMessageProcessor implements Processor {

  private OutboundEndpoint endpoint;

  public OutboundEndpointMimeTypeCheckingMessageProcessor(OutboundEndpoint endpoint) {
    this.endpoint = endpoint;
  }

  @Override
  public Event process(Event event) throws MessagingException {
    org.mule.runtime.api.metadata.MediaType endpointMimeType = endpoint.getMimeType();
    if (endpointMimeType != null) {
      InternalMessage message = event.getMessage();
      final DataType dataType = message.getPayload().getDataType();
      if (DataType.OBJECT.getMediaType().matches(dataType.getMediaType())) {
        event = Event.builder(event)
            .message(InternalMessage.builder(event.getMessage()).mediaType(dataType.getMediaType()).build()).build();
      } else {
        if (!dataType.getMediaType().matches(endpointMimeType)) {
          throw new MessagingException(CoreMessages.unexpectedMIMEType(dataType.getMediaType().toRfcString(),
                                                                       endpointMimeType.toRfcString()),
                                       event, this);
        }
      }
    }

    return event;
  }

  @Override
  public String toString() {
    return ObjectUtils.toString(this);
  }
}
