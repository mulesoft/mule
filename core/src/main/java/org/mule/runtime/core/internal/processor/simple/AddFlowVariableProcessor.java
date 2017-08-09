/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.simple;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.privileged.processor.simple.AbstractAddVariablePropertyProcessor;

public class AddFlowVariableProcessor extends AbstractAddVariablePropertyProcessor<Object> {

  @Override
  protected InternalEvent addProperty(InternalEvent event, String propertyName, Object value, DataType dataType) {
    return InternalEvent.builder(event).addVariable(propertyName, value, dataType).build();
  }

  @Override
  protected InternalEvent removeProperty(InternalEvent event, String propertyName) {
    return InternalEvent.builder(event).removeVariable(propertyName).build();
  }
}
