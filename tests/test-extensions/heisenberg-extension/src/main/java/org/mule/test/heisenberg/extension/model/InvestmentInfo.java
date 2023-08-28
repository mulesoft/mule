/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.model;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

public class InvestmentInfo {

  @Parameter
  @Optional
  private Investment investmentPlanB;

  @Parameter
  private Long valuation;

  public Long getValuation() {
    return valuation;
  }

  public void setValuation(Long valuation) {
    this.valuation = valuation;
  }

  public Investment getInvestmentPlanB() {
    return investmentPlanB;
  }

  public void setInvestmentPlanB(Investment investmentPlanB) {
    this.investmentPlanB = investmentPlanB;
  }
}
