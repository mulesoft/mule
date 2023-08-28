/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension.model.attribute;

import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;

import java.io.Serializable;

@TypeDsl(allowTopLevelDefinition = true)
public class ShapeOutputAttributes implements Serializable, AbstractOutputAttributes {

  private String outputId = "ShapesOutputAttributes";

  private String sides;

  public String getSides() {
    return sides;
  }

  public void setSides(String sides) {
    this.sides = sides;
  }

  @Override
  public String getOutputId() {
    return outputId;
  }
}
