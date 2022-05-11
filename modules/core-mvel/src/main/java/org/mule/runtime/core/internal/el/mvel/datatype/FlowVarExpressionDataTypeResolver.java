/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.el.mvel.datatype;

import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.core.internal.el.mvel.MessageVariableResolverFactory.FLOW_VARS;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

/**
 * Resolves data type for flow var when flowVars['x'] syntax is used
 */
public class FlowVarExpressionDataTypeResolver extends AbstractVariableExpressionDataTypeResolver {

  public FlowVarExpressionDataTypeResolver() {
    super(FLOW_VARS);
  }

  @Override
  protected DataType getVariableDataType(PrivilegedEvent event, String propertyName) {
    if (event.getVariables().containsKey(propertyName)) {
      return event.getVariables().get(propertyName).getDataType();
    } else {
      return OBJECT;
    }
  }
}
