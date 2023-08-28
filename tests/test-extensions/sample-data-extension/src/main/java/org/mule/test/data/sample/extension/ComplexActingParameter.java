/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.data.sample.extension;

public class ComplexActingParameter {

  private String payload;
  private String attributes;

  public String getPayload() {
    return payload;
  }

  public String getAttributes() {
    return attributes;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public void setAttributes(String attributes) {
    this.attributes = attributes;
  }
}
