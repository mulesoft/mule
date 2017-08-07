/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.el.mvel.datatype;

import static org.mule.runtime.core.el.mvel.MessageVariableResolverFactory.FLOW_VARS;
import static org.mule.runtime.core.api.Event.getVariableValueOrNull;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.Event;

public class FlowVarEnricherDataTypePropagatorTestCase extends AbstractScopedVarAssignmentDataTypePropagatorTestCase {

  public FlowVarEnricherDataTypePropagatorTestCase() {
    super(new FlowVarEnricherDataTypePropagator(), FLOW_VARS);
  }

  @Override
  protected DataType getVariableDataType(Event event) {
    return event.getVariables().get(PROPERTY_NAME).getDataType();
  }

  @Override
  protected Event setVariable(Event event, Object propertyValue, DataType dataType) {
    return Event.builder(event).addVariable(PROPERTY_NAME, propertyValue, dataType).build();
  }
}
