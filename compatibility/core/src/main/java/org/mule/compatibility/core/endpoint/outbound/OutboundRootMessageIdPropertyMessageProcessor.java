/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint.outbound;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_ROOT_MESSAGE_ID_PROPERTY;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.util.ObjectUtils;

/**
 * Sets the outbound root message id on as a property of the message.
 */
public class OutboundRootMessageIdPropertyMessageProcessor extends AbstractAnnotatedObject implements Processor {

  @Override
  public Event process(Event event) throws MuleException {
    return Event.builder(event).message(InternalMessage.builder(event.getMessage())
        .addOutboundProperty(MULE_ROOT_MESSAGE_ID_PROPERTY, event.getCorrelationId()).build()).build();
  }

  @Override
  public String toString() {
    return ObjectUtils.toString(this);
  }
}
