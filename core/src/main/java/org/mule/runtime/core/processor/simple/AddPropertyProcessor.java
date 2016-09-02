/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.simple;


import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;

import java.io.Serializable;

public class AddPropertyProcessor extends AbstractAddVariablePropertyProcessor<Serializable> {

  @Override
  protected MuleEvent addProperty(MuleEvent event, String propertyName, Serializable value, DataType dataType) {
    return MuleEvent.builder(event)
        .message(MuleMessage.builder(event.getMessage()).addOutboundProperty(propertyName, value, dataType).build()).build();
  }

  @Override
  protected MuleEvent removeProperty(MuleEvent event, String propertyName) {
    return MuleEvent.builder(event).message(MuleMessage.builder(event.getMessage()).removeOutboundProperty(propertyName).build())
        .build();
  }

}
