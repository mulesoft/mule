/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.multi.implicit.config.extension.extension;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.test.implicit.exclusive.config.extension.extension.BleOperations;
import org.mule.test.implicit.exclusive.config.extension.extension.ConfigWithNumber;

@Operations({BleOperations.class})
@Configuration(name = "yetanotherimplicitconfig")
public class AnotherConfigThatCanBeUsedImplicitly extends ConfigWithNumber {

  @Override
  protected int getNumber() {
    return 20;
  }

}
