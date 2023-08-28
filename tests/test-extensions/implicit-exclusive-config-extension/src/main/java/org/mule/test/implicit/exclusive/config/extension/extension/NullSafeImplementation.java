/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.implicit.exclusive.config.extension.extension;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

public class NullSafeImplementation implements NullSafeInterface {

  @Parameter
  @Optional
  int number;

  @Override
  public int getNumber() {
    return number;
  }

  public void setNumber(int number) {
    this.number = number;
  }

}
