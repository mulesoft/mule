/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.subtypes.extension;

import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.test.heisenberg.extension.model.Weapon;

@TypeDsl(allowTopLevelDefinition = true)
public class Revolver implements Weapon {

  @Parameter
  @Optional(defaultValue = "6")
  private int bullets;

  public int getBullets() {
    return bullets;
  }

  @Override
  public String kill() {
    return bullets > 0 ? "BANG" : "LUCKY";
  }
}
