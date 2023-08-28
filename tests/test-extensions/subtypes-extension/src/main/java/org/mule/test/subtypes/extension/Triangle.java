/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.subtypes.extension;


import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;

@Alias("triangleAlias")
@TypeDsl(allowTopLevelDefinition = true)
public class Triangle extends ParentShape {

  private Integer height;

  public Integer getHeight() {
    return height;
  }

  public void setHeight(Integer height) {
    this.height = height;
  }
}
