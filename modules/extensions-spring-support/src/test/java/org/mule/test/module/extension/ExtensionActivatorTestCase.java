/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;
import org.mule.runtime.module.extension.internal.manager.ExtensionActivator;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import org.junit.Test;

public class ExtensionActivatorTestCase extends ExtensionFunctionalTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"heisenberg-config.xml"};
  }

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {HeisenbergExtension.class};
  }

  @Test
  public void enumsReleasedWhenStopped() throws Exception {
    DefaultExtensionManager extensionManager = (DefaultExtensionManager) muleContext.getExtensionManager();
    ExtensionActivator extensionActivator = extensionManager.getExtensionActivator();
    extensionActivator.stop();

    assertThat(extensionActivator.getEnumTypes(), hasSize(0));
  }
}
