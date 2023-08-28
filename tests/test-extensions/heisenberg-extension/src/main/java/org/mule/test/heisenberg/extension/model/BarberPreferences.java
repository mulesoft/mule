/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.model;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.semantics.SemanticTerms;

public class BarberPreferences {

  public enum BEARD_KIND {
    GOATIE, MUSTACHE
  }

  @Parameter
  @Optional
  @SemanticTerms("hairy")
  private boolean fullyBald;

  @Parameter
  @Optional(defaultValue = "GOATIE")
  private BEARD_KIND beardTrimming;

  public boolean isFullyBald() {
    return fullyBald;
  }

  public BEARD_KIND getBeardTrimming() {
    return beardTrimming;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof BarberPreferences)) {
      return false;
    }

    BarberPreferences that = (BarberPreferences) o;
    return fullyBald == that.fullyBald && beardTrimming == that.beardTrimming;
  }

  @Override
  public int hashCode() {
    return 31 * (fullyBald ? 1 : 0) + beardTrimming.hashCode();
  }

}
