/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.junit4.rule;

import java.util.function.Supplier;

/**
 * Allows using a supplier for resolving the value of the system property to set.
 */
public class SystemPropertyLambda extends SystemProperty {

  private Supplier<String> propertySupplier;

  public SystemPropertyLambda(String name, Supplier<String> propertySupplier) {
    super(name);
    this.propertySupplier = propertySupplier;
  }

  @Override
  public String getValue() {
    if (!initialized) {
      value = propertySupplier.get();
    }
    return super.getValue();
  }
}
