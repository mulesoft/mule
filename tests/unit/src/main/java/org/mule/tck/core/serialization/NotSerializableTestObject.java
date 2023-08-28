/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.core.serialization;

public final class NotSerializableTestObject {

  private final String name;

  public NotSerializableTestObject(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
