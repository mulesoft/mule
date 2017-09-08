/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.simple;

import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.privileged.processor.simple.AbstractRemoveVariablePropertyProcessor;

import java.util.Set;

public class RemoveFlowVariableProcessor extends AbstractRemoveVariablePropertyProcessor {

  private static final String FLOW_VAR_NAME = "flow variables";

  @Override
  protected Set<String> getPropertyNames(BaseEvent event) {
    return event.getVariables().keySet();
  }

  @Override
  protected BaseEvent removeProperty(BaseEvent event, String propertyName) {
    return BaseEvent.builder(event).removeVariable(propertyName).build();
  }

  @Override
  protected String getScopeName() {
    return FLOW_VAR_NAME;
  }
}
