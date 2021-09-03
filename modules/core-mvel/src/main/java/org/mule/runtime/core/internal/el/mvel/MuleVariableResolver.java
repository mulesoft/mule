/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
