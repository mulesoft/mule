/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint.outbound;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_ROOT_MESSAGE_ID_PROPERTY;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.util.ObjectUtils;

/**
 * Sets the outbound root message id on as a property of the message.
 */
public class OutboundRootMessageIdPropertyMessageProcessor implements MessageProcessor {

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    return MuleEvent.builder(event).message(MuleMessage.builder(event.getMessage())
        .addOutboundProperty(MULE_ROOT_MESSAGE_ID_PROPERTY, event.getCorrelationId()).build()).build();
  }

  @Override
  public String toString() {
    return ObjectUtils.toString(this);
  }
}
