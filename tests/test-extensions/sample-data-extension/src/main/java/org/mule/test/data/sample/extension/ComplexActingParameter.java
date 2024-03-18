/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
