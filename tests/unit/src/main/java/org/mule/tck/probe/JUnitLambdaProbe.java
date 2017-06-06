/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.probe;

import org.mule.runtime.core.api.util.func.CheckedSupplier;

/**
 * Implementation of {@link JUnitProbe} which makes it Java 8 Lambda friendly.
 */
public class JUnitLambdaProbe extends JUnitProbe {

  private final CheckedSupplier<Boolean> probable;
  private final CheckedSupplier<String> failureDescription;

  public JUnitLambdaProbe(CheckedSupplier<Boolean> probable, String failureDescription) {
    this.probable = probable;
    this.failureDescription = () -> failureDescription;
  }

  public JUnitLambdaProbe(CheckedSupplier<Boolean> probable, CheckedSupplier<String> failureDescription) {
    this.probable = probable;
    this.failureDescription = failureDescription;
  }

  public JUnitLambdaProbe(CheckedSupplier<Boolean> probable) {
    this(probable, () -> null);
  }

  @Override
  public String describeFailure() {
    return failureDescription.get() == null ? super.describeFailure() : failureDescription.get();
  }

  @Override
  protected boolean test() throws Exception {
    return probable.get();
  }
}
