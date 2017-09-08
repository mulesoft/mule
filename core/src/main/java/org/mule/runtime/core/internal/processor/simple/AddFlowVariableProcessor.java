/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.simple;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.privileged.processor.simple.AbstractAddVariablePropertyProcessor;

public class AddFlowVariableProcessor extends AbstractAddVariablePropertyProcessor<Object> {

  @Override
  protected BaseEvent addProperty(BaseEvent event, String propertyName, Object value, DataType dataType) {
    return BaseEvent.builder(event).addVariable(propertyName, value, dataType).build();
  }

  @Override
  protected BaseEvent removeProperty(BaseEvent event, String propertyName) {
    return BaseEvent.builder(event).removeVariable(propertyName).build();
  }
}
