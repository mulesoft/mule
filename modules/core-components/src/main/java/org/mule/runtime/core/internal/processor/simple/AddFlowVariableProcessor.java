/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.simple;

import static org.mule.runtime.core.api.util.StreamingUtils.updateTypedValueForStreaming;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.processor.simple.AbstractAddVariablePropertyProcessor;

public class AddFlowVariableProcessor extends AbstractAddVariablePropertyProcessor<Object> {

  @Override
  protected PrivilegedEvent addProperty(PrivilegedEvent event, String propertyName, Object value, DataType dataType) {
    return PrivilegedEvent.builder(event).addVariable(propertyName, value, dataType).build();
  }

  @Override
  protected PrivilegedEvent removeProperty(PrivilegedEvent event, String propertyName) {
    return PrivilegedEvent.builder(event).removeVariable(propertyName).build();
  }

  @Override
  protected TypedValue<Object> handleStreaming(TypedValue<Object> typedValue, CoreEvent event,
                                               StreamingManager streamingManager) {
    return updateTypedValueForStreaming(typedValue, event, streamingManager);
  }
}
