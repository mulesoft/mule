/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.substitutiongroup.extension;

import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Parameter;

@TypeDsl(substitutionGroup = "heisenberg:global-abstract-weapon",
    baseType = "heisenberg:org.mule.test.heisenberg.extension.model.Weapon", allowTopLevelDefinition = true)
public class SomeExportedPojo {

  @Parameter
  private boolean attribute;

}
