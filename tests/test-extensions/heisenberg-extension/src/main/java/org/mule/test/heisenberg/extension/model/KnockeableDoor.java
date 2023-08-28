/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.model;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.dsl.xml.TypeDsl;

import java.io.Serializable;

@Alias("door")
@TypeDsl(allowTopLevelDefinition = true)
public class KnockeableDoor implements Serializable {

  @Parameter
  private String victim;

  @Parameter
  private String address;

  @Parameter
  @Optional
  private KnockeableDoor previous;

  public KnockeableDoor() {}

  public KnockeableDoor(String victim) {
    this.victim = victim;
  }

  public KnockeableDoor(String victim, String address) {
    this.victim = victim;
    this.address = address;
  }

  public static String knock(String value) {
    return "Knocked on " + value;
  }

  public String knock() {
    return knock(victim);
  }

  public String getVictim() {
    return victim;
  }

  public String getAddress() {
    return address;
  }

  public KnockeableDoor getPrevious() {
    return previous;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof KnockeableDoor) {
      return victim.equals(((KnockeableDoor) obj).victim);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return victim.hashCode();
  }
}
