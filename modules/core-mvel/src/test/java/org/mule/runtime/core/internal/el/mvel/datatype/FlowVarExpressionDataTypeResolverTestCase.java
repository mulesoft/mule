/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.mvel.datatype;

import static org.mule.runtime.core.internal.el.mvel.MessageVariableResolverFactory.FLOW_VARS;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

public class FlowVarExpressionDataTypeResolverTestCase extends AbstractVarExpressionDataTypeResolverTestCase {

  public FlowVarExpressionDataTypeResolverTestCase() {
    super(new FlowVarExpressionDataTypeResolver(), FLOW_VARS);
  }

  @Override
  protected PrivilegedEvent setVariable(PrivilegedEvent event, Object propertyValue, DataType dataType) {
    return PrivilegedEvent.builder(event).addVariable(PROPERTY_NAME, propertyValue, dataType).build();
  }
}
