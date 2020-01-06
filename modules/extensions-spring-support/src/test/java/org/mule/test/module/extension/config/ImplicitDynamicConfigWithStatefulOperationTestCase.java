/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.config;

import org.mule.test.module.extension.InvalidExtensionConfigTestCase;

import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ImplicitDynamicConfigWithStatefulOperationTestCase extends InvalidExtensionConfigTestCase {

  @Override
  protected String getConfigFile() {
    return "implicit-dynamic-stateful-config.xml";
  }

  @Override
  protected void additionalExceptionAssertions(ExpectedException expectedException) {
    //expectedException.expect();
  }

  @Test
  public void implicitStateful() throws Exception {
    //flowRunner("implicitConfig")
    //    .withVariable("number", "42")
    //    .run();
  }
}
