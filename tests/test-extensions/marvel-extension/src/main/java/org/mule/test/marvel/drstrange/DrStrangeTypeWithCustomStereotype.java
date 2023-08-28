/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.marvel.drstrange;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;

import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.stereotype.Stereotype;
import org.mule.runtime.extension.api.stereotype.ObjectStoreStereotype;

@Stereotype(DrStrangeStereotypeDefinition.class)
public class DrStrangeTypeWithCustomStereotype {

  @Parameter
  private String storeName;
}
