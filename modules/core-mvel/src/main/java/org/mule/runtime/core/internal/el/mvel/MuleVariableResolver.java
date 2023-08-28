/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.mvel;

import org.mule.mvel2.integration.VariableResolverFactory;
import org.mule.mvel2.integration.impl.SimpleSTValueResolver;

class MuleVariableResolver<T> extends SimpleSTValueResolver {

  private static final long serialVersionUID = -4957789619105599831L;
  protected String name;
  protected VariableAssignmentCallback<T> assignmentCallback;

  public MuleVariableResolver(String name, T value, Class<?> type, VariableAssignmentCallback<T> callback) {
    super(value, type);
    this.name = name;
    this.assignmentCallback = callback;
  }

  @Override
  public String getName() {
    return name;
  }

  @SuppressWarnings("unchecked")
  public T getValue(VariableResolverFactory variableResolverFactory) {
    return (T) getValue();
  }

  @SuppressWarnings("unchecked")
  @Override
  public void setValue(Object value) {
    if (assignmentCallback != null) {
      assignmentCallback.assignValue(name, (T) getValue(), (T) value);
    } else {
      super.setValue(value);
    }
  }
}
