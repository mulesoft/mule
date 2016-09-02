/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.simple;

import org.mule.runtime.core.PropertyScope;
import org.mule.runtime.core.api.MuleEvent;

import java.util.Set;

public class RemoveFlowVariableProcessor extends AbstractRemoveVariablePropertyProcessor {

  @Override
  protected Set<String> getPropertyNames(MuleEvent event) {
    return event.getFlowVariableNames();
  }

  @Override
  protected MuleEvent removeProperty(MuleEvent event, String propertyName) {
    return MuleEvent.builder(event).removeFlowVariable(propertyName).build();
  }

  @Override
  protected String getScopeName() {
    return PropertyScope.FLOW_VAR_NAME;
  }
}
