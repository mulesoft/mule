/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.tck.testmodels.fruit.Fruit;

import java.util.List;
import java.util.Map;

public class VeganFidelityOperation {

  public String tryEat(@XmlHints(allowReferences = false) Object food,
                       @Optional @XmlHints(allowInlineDefinition = false) Map<String, Integer> menu) {
    if (food instanceof Fruit) {
      return "tasty " + food.getClass().getSimpleName();
    }

    throw new IllegalArgumentException("I SHALL NEVER EAT " + food.toString());
  }

  public List<Map<String, String>> tryToEatThisListOfMaps(@Optional(
      defaultValue = "#[mel:new java.util.ArrayList()]") @Content List<Map<String, String>> foods) {
    if (foods != null) {
      return foods;
    }
    throw new IllegalArgumentException("I CAN'T EAT THAT TYPE, IS IMPOSSIBLE!");
  }

}
