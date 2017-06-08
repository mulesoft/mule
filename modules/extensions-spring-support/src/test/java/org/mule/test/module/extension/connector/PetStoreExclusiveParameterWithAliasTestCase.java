/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.connector;

import org.mule.test.module.extension.InvalidExtensionConfigTestCase;

import org.junit.rules.ExpectedException;

public class PetStoreExclusiveParameterWithAliasTestCase extends InvalidExtensionConfigTestCase {

  @Override
  protected String getConfigFile() {
    return "petstore-exclusive-parameter-with-alias.xml";
  }

  @Override
  protected void additionalExceptionAssertions(ExpectedException expectedException) {
    expectedException.expectMessage("the following parameters cannot be set at the same time: [cash, debt]");
  }
}
