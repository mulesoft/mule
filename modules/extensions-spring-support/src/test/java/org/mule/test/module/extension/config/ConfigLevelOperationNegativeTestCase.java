/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.config;

import org.mule.test.module.extension.InvalidExtensionConfigTestCase;

import org.junit.rules.ExpectedException;

public class ConfigLevelOperationNegativeTestCase extends InvalidExtensionConfigTestCase {

  @Override
  protected String getConfigFile() {
    return "validation/vegan-invalid-config-for-operations.xml";
  }

  @Override
  protected void additionalExceptionAssertions(ExpectedException expectedException) {
    super.additionalExceptionAssertions(expectedException);
    expectedException
        .expectMessage("Root component 'appleEatsBanana' defines an usage of operation 'eatBanana' which points to configuration 'apple'. The selected config does not support that operation.");
  }
}
