/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.mvel.datatype;

import static org.mule.runtime.core.internal.el.mvel.MessageVariableResolverFactory.FLOW_VARS;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

/**
 * Propagates data type for flow vars used for enrichment target
 */
public class FlowVarEnricherDataTypePropagator extends AbstractVariableEnricherDataTypePropagator {

  public FlowVarEnricherDataTypePropagator() {
    super(FLOW_VARS);
  }

  @Override
  protected void addVariable(PrivilegedEvent event, PrivilegedEvent.Builder builder, TypedValue typedValue, String propertyName) {
    builder.addVariable(propertyName, typedValue);
  }

  @Override
  protected boolean containsVariable(PrivilegedEvent event, String propertyName) {
    return event.getVariables().containsKey(propertyName);
  }
}
