/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.model;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.test.heisenberg.extension.HeisenbergOperations;

import java.util.Map;

/**
 * DON'T USE THIS CLASS BEYOND {@link HeisenbergOperations#processSale(Map)} READ: MULE-10303
 */
public class SaleInfo {

  @Parameter
  private String details;

  @Parameter
  private Integer amount;

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }

  public Integer getAmount() {
    return amount;
  }

  public void setAmount(Integer amount) {
    this.amount = amount;
  }
}
