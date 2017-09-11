/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.el.mvel.datatype;

import static org.mule.runtime.core.internal.el.mvel.MessageVariableResolverFactory.SESSION_VARS;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import java.io.Serializable;

public class SessionVarEnricherDataTypePropagatorTestCase extends AbstractScopedVarAssignmentDataTypePropagatorTestCase {

  public SessionVarEnricherDataTypePropagatorTestCase() {
    super(new SessionVarEnricherDataTypePropagator(), SESSION_VARS);
  }

  @Override
  protected DataType getVariableDataType(PrivilegedEvent event) {
    return event.getSession().getPropertyDataType(PROPERTY_NAME);
  }

  @Override
  protected PrivilegedEvent setVariable(PrivilegedEvent event, Object propertyValue, DataType dataType) {
    event.getSession().setProperty(PROPERTY_NAME, (Serializable) propertyValue, dataType);
    return event;
  }
}
