/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.probe;

import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.core.api.util.func.CheckedSupplier;

/**
 * Implementation of {@link JUnitProbe} which makes it Java 8 Lambda friendly.
 */
public class JUnitLambdaProbe extends JUnitProbe {

  private final CheckedSupplier<Boolean> probable;
  private final CheckedFunction<AssertionError, String> failureDescription;

  public JUnitLambdaProbe(CheckedSupplier<Boolean> probable, String failureDescription) {
    this.probable = probable;
    this.failureDescription = ae -> failureDescription;
  }

  public JUnitLambdaProbe(CheckedSupplier<Boolean> probable, CheckedFunction<AssertionError, String> failureDescription) {
    this.probable = probable;
    this.failureDescription = failureDescription;
  }

  public JUnitLambdaProbe(CheckedSupplier<Boolean> probable, CheckedSupplier<String> failureDescription) {
    this.probable = probable;
    this.failureDescription = ae -> failureDescription.get();
  }

  public JUnitLambdaProbe(CheckedSupplier<Boolean> probable) {
    this(probable, ae -> null);
  }

  @Override
  public String describeFailure() {
    String description = failureDescription.apply(lastFailure);
    return description == null ? super.describeFailure() : description;
  }

  @Override
  protected boolean test() throws Exception {
    return probable.get();
  }
}
