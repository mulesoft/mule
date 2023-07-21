/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

@ExclusiveOptionals
public class ExclusiveCashier {

  @Parameter
  private String cashierName;

  @Parameter
  @Optional
  private String rothIRA;

  @Parameter
  @Optional
  private String pensionPlan;

  @Parameter
  @Optional
  @Alias("cash")
  private Integer money;

  @Parameter
  @Optional
  private Integer debt;

  public String getCashierName() {
    return cashierName;
  }

  public String getRothIRA() {
    return rothIRA;
  }

  public String getPensionPlan() {
    return pensionPlan;
  }

  public Integer getMoney() {
    return money;
  }

  public Integer getDebt() {
    return debt;
  }
}
