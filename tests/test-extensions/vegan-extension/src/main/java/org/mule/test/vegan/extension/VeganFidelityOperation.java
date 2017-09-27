/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.extension.api.annotation.dsl.xml.ParameterDsl;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.tck.testmodels.fruit.Fruit;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class VeganFidelityOperation {

  @MediaType(TEXT_PLAIN)
  public String tryEat(@ParameterDsl(allowReferences = false) Object food,
                       @Optional @ParameterDsl(allowInlineDefinition = false) Map<String, Integer> menu) {
    if (food instanceof Fruit) {
      return "tasty " + food.getClass().getSimpleName();
    }

    throw new IllegalArgumentException("I SHALL NEVER EAT " + food.toString());
  }

  public List<Map<String, String>> tryToEatThisListOfMaps(@Optional @NullSafe @Content List<Map<String, String>> foods) {
    if (foods != null) {
      return foods;
    }
    throw new IllegalArgumentException("I CAN'T EAT THAT TYPE, IS IMPOSSIBLE!");
  }

  @OutputResolver(output = FruitMetadataResolver.class)
  public List<FarmedFood> consumingGroupedFood(@ParameterGroup(name = "As Group") GroupedFood groupedFood,
                                               @Optional @NullSafe GroupedFood pojoGroupedFood) {
    return Arrays.asList(groupedFood.getFood(), pojoGroupedFood.getFood());
  }

}
