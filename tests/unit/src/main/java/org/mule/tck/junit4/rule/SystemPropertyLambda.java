/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
