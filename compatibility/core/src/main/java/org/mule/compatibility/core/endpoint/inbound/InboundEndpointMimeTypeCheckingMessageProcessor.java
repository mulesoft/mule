/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint.inbound;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.util.ObjectUtils;

/**
 * Verify that the inbound mime type is acceptable by this endpoint.
 */
public class InboundEndpointMimeTypeCheckingMessageProcessor extends AbstractAnnotatedObject implements Processor {

  private InboundEndpoint endpoint;

  public InboundEndpointMimeTypeCheckingMessageProcessor(InboundEndpoint endpoint) {
    this.endpoint = endpoint;
  }

  @Override
  public Event process(Event event) throws MessagingException {
    MediaType endpointMimeType = endpoint.getMimeType();
    if (endpointMimeType != null) {
      InternalMessage message = event.getMessage();
      final DataType dataType = message.getPayload().getDataType();
      if (DataType.OBJECT.getMediaType().matches(dataType.getMediaType())) {
        event =
            Event.builder(event).message(InternalMessage.builder(event.getMessage()).mediaType(endpointMimeType).build()).build();
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
