/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
