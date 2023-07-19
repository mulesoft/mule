/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.implicit.exclusive.config.extension.extension;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;

@Operations({BleOperations.class})
@Configuration(name = "nonimplicit")
public class NonImplicitConfig extends ConfigWithNumber {

  @Parameter
  int number;

  public int getNumber() {
    return number;
  }
}
