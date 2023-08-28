/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api;

import java.io.Serializable;

public class TestSerializableMessageProcessor extends TestNotSerializableMessageProcessor implements Serializable {

  private static final long serialVersionUID = -6309566893615114065L;

  private String value;

  public TestSerializableMessageProcessor() {
    super();
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
