/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.mvel.datatype;

import static org.mule.runtime.core.internal.el.mvel.MessageVariableResolverFactory.SESSION_VARS;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

/**
 * Resolves data type for session var when sessionVars['x'] syntax is used
 */

public class SessionVarExpressionDataTypeResolver extends AbstractVariableExpressionDataTypeResolver {

  public SessionVarExpressionDataTypeResolver() {
    super(SESSION_VARS);
  }

  @Override
  protected DataType getVariableDataType(PrivilegedEvent event, String propertyName) {
    return event.getSession().getPropertyDataType(propertyName);
  }

}
