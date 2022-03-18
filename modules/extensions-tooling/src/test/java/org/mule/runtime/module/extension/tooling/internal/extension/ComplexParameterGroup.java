/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.tooling.internal.extension;

import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.List;

public class ComplexParameterGroup {

  @Parameter
  private boolean partyMode;

  @Parameter
  @NullSafe
  private List<String> greetings;

  @Parameter
  @NullSafe(defaultImplementingType = AnnoyingPojo.class)
  private AnnoyingPojo annoyingPojo;

  public boolean isPartyMode() {
    return partyMode;
  }

  public List<String> getGreetings() {
    return greetings;
  }

  public AnnoyingPojo getAnnoyingPojo() {
    return annoyingPojo;
  }
}
