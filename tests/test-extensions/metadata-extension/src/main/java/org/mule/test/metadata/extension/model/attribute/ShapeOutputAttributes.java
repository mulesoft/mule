/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
