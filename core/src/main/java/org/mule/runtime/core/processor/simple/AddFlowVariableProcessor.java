/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.simple;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.Event;

public class AddFlowVariableProcessor extends AbstractAddVariablePropertyProcessor<Object> {

  @Override
  protected Event addProperty(Event event, String propertyName, Object value, DataType dataType) {
    return Event.builder(event).addVariable(propertyName, value, dataType).build();
  }

  @Override
  protected Event removeProperty(Event event, String propertyName) {
    return Event.builder(event).removeVariable(propertyName).build();
  }
}
