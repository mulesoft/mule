/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.mvel.datatype;

import static org.mule.runtime.core.internal.el.mvel.MessageVariableResolverFactory.SESSION_VARS;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import java.io.Serializable;

public class SessionVarExpressionDataTypeResolverTestCase extends AbstractVarExpressionDataTypeResolverTestCase {

  public SessionVarExpressionDataTypeResolverTestCase() {
    super(new SessionVarExpressionDataTypeResolver(), SESSION_VARS);
  }

  @Override
  protected PrivilegedEvent setVariable(PrivilegedEvent event, Object propertyValue, DataType dataType) {
    event.getSession().setProperty(PROPERTY_NAME, (Serializable) propertyValue, dataType);
    return event;
  }
}
