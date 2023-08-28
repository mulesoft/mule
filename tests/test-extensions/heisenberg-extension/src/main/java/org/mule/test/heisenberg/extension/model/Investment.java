/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.model;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.util.List;
import java.util.Map;

public abstract class Investment {

  private boolean approved = false;

  @Parameter
  private String commercialName;

  @Parameter
  @Optional
  private List<Investment> discardedInvestments;

  @Parameter
  @Optional
  private Map<String, Investment> investmentSpinOffs;

  @ParameterGroup(name = "Investment")
  private InvestmentInfo investmentInfo;

  public String getCommercialName() {
    return commercialName;
  }

  public InvestmentInfo getInvestmentInfo() {
    return investmentInfo;
  }

  public void approve() {
    approved = true;
  }

  public boolean isApproved() {
    return approved;
  }

  public List<Investment> getDiscardedInvestments() {
    return discardedInvestments;
  }

  public Map<String, Investment> getInvestmentSpinOffs() {
    return investmentSpinOffs;
  }
}
