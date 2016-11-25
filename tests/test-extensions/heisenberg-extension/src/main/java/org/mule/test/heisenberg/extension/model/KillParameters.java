/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension.model;

import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

public class KillParameters {


  public KillParameters() {}

  public KillParameters(String victim, String goodbyeMessage) {
    this.victim = victim;
    this.goodbyeMessage = goodbyeMessage;
  }

  @Parameter
  @Optional(defaultValue = PAYLOAD)
  @Placement(order = 1)
  private String victim;

  @Parameter
  @Placement(order = 2)
  private String goodbyeMessage;

  public String getVictim() {
    return victim;
  }

  public String getGoodbyeMessage() {
    return goodbyeMessage;
  }
}
