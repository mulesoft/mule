/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
