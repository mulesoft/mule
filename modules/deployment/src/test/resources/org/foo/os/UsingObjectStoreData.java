/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo.os;

import java.io.Serializable;

import java.lang.String;

import jakarta.inject.Inject;

public class UsingObjectStoreData implements Serializable {

  private static final long serialVersionUID = 123L;

  private String value;

  public UsingObjectStoreData(String value) {
    this.value = value;
  }
  
  public String getValue() {
    return value;
  }
  
  public void setValue(String value) {
    this.value = value;
  }
  
}
