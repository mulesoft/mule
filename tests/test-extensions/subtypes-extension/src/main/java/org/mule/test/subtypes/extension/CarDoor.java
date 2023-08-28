/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.subtypes.extension;

import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

@TypeDsl(allowTopLevelDefinition = true)
public class CarDoor implements Door {

  @Parameter
  private String color;

  @Parameter
  @Optional(defaultValue = "button")
  private String handle;

  @Override
  public void open() {}

  public void raiseWindow() {}

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  @Override
  public String getHandle() {
    return this.handle;
  }
}
