/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.scopes;

import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.test.module.extension.InvalidExtensionConfigTestCase;

public class ScopeStereotypeValidationTestCase extends InvalidExtensionConfigTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"scopes/heisenberg-stereotype-validation-config.xml"};
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException.expectMessage("Invalid content was found starting with element 'set-variable'");
  }

}
