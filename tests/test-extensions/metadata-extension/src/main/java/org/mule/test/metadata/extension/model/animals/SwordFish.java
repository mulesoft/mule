/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension.model.animals;

import static org.mule.test.metadata.extension.model.animals.AnimalClade.FISH;

public class SwordFish extends Animal {

  @Override
  public AnimalClade clade() {
    return FISH;
  }
}
