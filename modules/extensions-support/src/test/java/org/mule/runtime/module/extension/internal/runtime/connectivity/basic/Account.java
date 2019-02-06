/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.basic;

import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.api.meta.ExpressionSupport;

import java.util.Objects;

public class Account {

  @Parameter
  @Expression(ExpressionSupport.REQUIRED)
  private Owner requiredInnerPojoWithExpressionRequired;

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Account account = (Account) o;
    return Objects.equals(requiredInnerPojoWithExpressionRequired, account.requiredInnerPojoWithExpressionRequired);
  }

  @Override
  public int hashCode() {
    return Objects.hash(requiredInnerPojoWithExpressionRequired);
  }
}
