/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.probe;

import java.util.function.BooleanSupplier;

/**
 * Implementation of {@link JUnitProbe} which makes it Java 8 Lambda friendly.
 */
public class JUnitLambdaProbe extends JUnitProbe {

  private final BooleanSupplier probable;
  private final String failureDescription;

  public JUnitLambdaProbe(BooleanSupplier probable, String failureDescription) {
    this.probable = probable;
    this.failureDescription = failureDescription;
  }

  public JUnitLambdaProbe(BooleanSupplier probable) {
    this(probable, null);
  }

  @Override
  public String describeFailure() {
    return failureDescription == null ? super.describeFailure() : failureDescription;
  }

  @Override
  protected boolean test() throws Exception {
    return probable.getAsBoolean();
  }
}
