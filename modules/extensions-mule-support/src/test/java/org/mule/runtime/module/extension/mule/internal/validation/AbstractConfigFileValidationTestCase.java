/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.validation;

import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.core.api.MuleContext;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(REUSE)
@Story(OPERATIONS)
public abstract class AbstractConfigFileValidationTestCase extends MuleArtifactFunctionalTestCase {

  private String configFile;

  @Override
  protected String getConfigFile() {
    return configFile;
  }

  @Override
  protected MuleContext createMuleContext() {
    // Disables MuleContext creation on setup, so we can do it on demand inside each test.
    return null;
  }

  @Override
  protected boolean doTestClassInjection() {
    return false;
  }

  protected void parseConfig(String configFile) throws Exception {
    this.configFile = configFile;
    try {
      super.createMuleContext();
    } finally {
      this.configFile = null;
    }
  }
}
