/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension.model.animals;

import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.test.metadata.extension.resolver.TestInputResolver;

public class AnimalShelter {

  @Parameter
  @TypeResolver(TestInputResolver.class)
  private Bear bear;

  @Parameter
  private SwordFish swordFish;

  public Bear getBear() {
    return bear;
  }

  public SwordFish getSwordFish() {
    return swordFish;
  }

}
