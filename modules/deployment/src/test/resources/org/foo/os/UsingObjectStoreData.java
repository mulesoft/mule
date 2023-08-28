/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.os;

import java.io.Serializable;

import java.lang.String;

import javax.inject.Inject;

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
