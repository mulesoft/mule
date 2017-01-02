/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
