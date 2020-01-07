/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.config;

import org.mule.test.module.extension.InvalidExtensionConfigTestCase;

import org.junit.rules.ExpectedException;

public class ImplicitDynamicConfigWithStatefulOperationOverrideTestCase extends InvalidExtensionConfigTestCase {

  @Override
  protected String getConfigFile() {
    return "implicit-dynamic-stateful-override-config.xml";
  }

  @Override
  protected void additionalExceptionAssertions(ExpectedException expectedException) {
    expectedException.expectMessage("Component 'implicit:get-enriched-name' at implicitConfig/processors/0 uses a dynamic "
        + "configuration and defines configuration override parameter 'optionalWithDefault' which is "
        + "assigned on initialization. That combination is not supported. Please use a non dynamic "
        + "configuration or don't set the parameter.");
  }

}
