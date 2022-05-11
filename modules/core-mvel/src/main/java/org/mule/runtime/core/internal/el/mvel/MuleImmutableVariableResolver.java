/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
