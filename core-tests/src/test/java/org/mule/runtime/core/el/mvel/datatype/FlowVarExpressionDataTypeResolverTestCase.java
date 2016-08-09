/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.el.mvel.datatype;

import static org.mule.runtime.core.el.mvel.MessageVariableResolverFactory.FLOW_VARS;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.api.metadata.DataType;

public class FlowVarExpressionDataTypeResolverTestCase extends AbstractVarExpressionDataTypeResolverTestCase {

  public FlowVarExpressionDataTypeResolverTestCase() {
    super(new FlowVarExpressionDataTypeResolver(), FLOW_VARS);
  }

  @Override
  protected void setVariable(MuleEvent event, Object propertyValue, DataType dataType) {
    event.setFlowVariable(PROPERTY_NAME, propertyValue, dataType);
  }
}
