/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import org.mule.functional.junit4.InvalidExtensionConfigTestCase;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

public class NamelessTopLevelElementTestCase extends InvalidExtensionConfigTestCase {

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException.expectMessage("Global element heisenberg:door does not provide a name attribute.");
  }

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class[] {HeisenbergExtension.class};
  }

  @Override
  protected String getConfigFile() {
    return "heisenberg-nameless-top-level-element-config.xml";
  }
}
