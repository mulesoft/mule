/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension.model.attribute;

import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;

import java.io.Serializable;

@TypeDsl(allowTopLevelDefinition = true)
public class AnimalsOutputAttributes implements Serializable, AbstractOutputAttributes {

  private String outputId = "AnimalsOutputAttributes";
  private String kind;

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  @Override
  public String getOutputId() {
    return outputId;
  }
}
