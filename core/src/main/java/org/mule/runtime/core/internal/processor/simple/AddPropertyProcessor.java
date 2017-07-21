/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.simple;


import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.processor.simple.AbstractAddVariablePropertyProcessor;

import java.io.Serializable;

public class AddPropertyProcessor extends AbstractAddVariablePropertyProcessor<Serializable> {

  @Override
  protected Event addProperty(Event event, String propertyName, Serializable value, DataType dataType) {
    return Event.builder(event)
        .message(InternalMessage.builder(event.getMessage()).addOutboundProperty(propertyName, value, dataType).build()).build();
  }

  @Override
  protected Event removeProperty(Event event, String propertyName) {
    return Event.builder(event).message(InternalMessage.builder(event.getMessage()).removeOutboundProperty(propertyName).build())
        .build();
  }

}
