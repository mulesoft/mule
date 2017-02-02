/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension.model;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

public class BarberPreferences {

  public enum BEARD_KIND {
    GOATIE, MUSTACHE
  }

  @Parameter
  @Optional
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

}
