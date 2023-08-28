/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.vegan.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
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

public class VeganFidelityOperation implements Initialisable {

  @ParameterGroup(name = "Arguments")
  private VeganArguments arguments;

  @Override
  public void initialise() throws InitialisationException {
    if (arguments == null) {
      throw new IllegalStateException("Forgot why I was began");
    }
  }

  @MediaType(TEXT_PLAIN)
  public String tryEat(@ParameterDsl(allowReferences = false) Object food,
                       @Optional @ParameterDsl(allowInlineDefinition = false) Map<String, Integer> menu) {
    if (food instanceof Fruit) {
      return "tasty " + food.getClass().getSimpleName();
    }

    throw new IllegalArgumentException("I SHALL NEVER EAT " + food.toString() + " because " + arguments.toString());
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
