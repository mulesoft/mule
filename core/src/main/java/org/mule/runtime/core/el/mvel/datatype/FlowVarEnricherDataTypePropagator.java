/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.el.mvel.datatype;

import static org.mule.runtime.core.el.mvel.MessageVariableResolverFactory.FLOW_VARS;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.metadata.TypedValue;

/**
 * Propagates data type for flow vars used for enrichment target
 */
public class FlowVarEnricherDataTypePropagator extends AbstractVariableEnricherDataTypePropagator {

  public FlowVarEnricherDataTypePropagator() {
    super(FLOW_VARS);
  }

  @Override
  protected void addVariable(MuleEvent event, MuleEvent.Builder builder, TypedValue typedValue, String propertyName) {
    builder.addFlowVariable(propertyName, typedValue.getValue(), typedValue.getDataType());
  }

  @Override
  protected boolean containsVariable(MuleEvent event, String propertyName) {
    return event.getFlowVariableNames().contains(propertyName);
  }
}
