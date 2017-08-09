/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension.model;

import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Parameter;

@TypeDsl(allowTopLevelDefinition = true)
public class Ricin implements Weapon {

  public static final String RICIN_KILL_MESSAGE = "You have been killed with Ricin";

  @Parameter
  private Long microgramsPerKilo;

  @Parameter
  private KnockeableDoor destination;

  public Ricin(KnockeableDoor destination, Long microgramsPerKilo) {
    this.destination = destination;
    this.microgramsPerKilo = microgramsPerKilo;
  }

  public Ricin() {}

  public Long getMicrogramsPerKilo() {
    return microgramsPerKilo;
  }

  public void setMicrogramsPerKilo(long microgramsPerKilo) {
    this.microgramsPerKilo = microgramsPerKilo;
  }

  public KnockeableDoor getDestination() {
    return destination;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Ricin) {
      return microgramsPerKilo.equals(((Ricin) obj).microgramsPerKilo);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return microgramsPerKilo.hashCode();
  }

  @Override
  public String kill() {
    return RICIN_KILL_MESSAGE;
  }
}
