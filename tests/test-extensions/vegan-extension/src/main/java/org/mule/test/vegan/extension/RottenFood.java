/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.vegan.extension;

public class RottenFood implements FarmedFood {

  @Override
  public boolean canBeEaten() {
    return false;
  }
}
