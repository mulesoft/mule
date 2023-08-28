/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.mvel;

import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.mvel2.ImmutableElementException;

@SuppressWarnings("serial")
public class MuleImmutableVariableResolver<T> extends MuleVariableResolver<T> {

  public MuleImmutableVariableResolver(String name, T value, Class<?> type) {
    super(name, value, type, new VariableAssignmentCallback<T>() {

      @Override
      public void assignValue(String name, T value, T newValue) {
        throw new ImmutableElementException(CoreMessages.expressionFinalVariableCannotBeAssignedValue(name).getMessage());
      }
    });
  }

}
