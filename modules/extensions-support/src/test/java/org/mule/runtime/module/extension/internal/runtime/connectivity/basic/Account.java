/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
