/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension.model;

import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.ParameterGroup;

public abstract class Investment {

  private boolean approved = false;

  @Parameter
  private String commercialName;

  @ParameterGroup
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
}
