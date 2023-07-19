/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.model.drugs;

public class DrugBatch {

  private String drugType;
  private Integer batchSize;

  public DrugBatch() {}

  public String getDrugType() {
    return drugType;
  }

  public DrugBatch(String drugType, Integer batchSize) {
    this.drugType = drugType;
    this.batchSize = batchSize;
  }

  public void setDrugType(String drugType) {
    this.drugType = drugType;
  }

  public Integer getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(Integer batchSize) {
    this.batchSize = batchSize;
  }
}
