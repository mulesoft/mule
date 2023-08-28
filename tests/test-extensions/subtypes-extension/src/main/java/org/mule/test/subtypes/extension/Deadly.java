/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.subtypes.extension;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.test.heisenberg.extension.model.Weapon;

public class Deadly {

  @Parameter
  private Weapon weapon;

  public Weapon getWeapon() {
    return weapon;
  }
}
