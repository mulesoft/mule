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
import org.mule.runtime.core.api.config.ConfigurationException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(REUSE)
@Story(OPERATIONS)
public class InvalidOperationInvocationTestCase extends MuleArtifactFunctionalTestCase {

  private String configFile;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return configFile;
  }

  @Test
  public void conflictingExclusiveParameters() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage("[validation/salutation-exclusive-parameters-operation.xml:11]: "
            + "Element <helloPlace>, the following parameters cannot be set at the same time: [city, country].");
    parseConfig("validation/salutation-exclusive-parameters-operation.xml");
  }

  @Test
  public void missingRequiredExclusiveParametersOperation() throws Exception {
    // FIXME W-10831629: there is an inconsistency between the element referred to by this error message and the one where there
    // are conflicting exclusive optionals
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage("[validation/salutation-exclusive-parameters-required-operation.xml:11]: "
            + "Element <General> requires that one of its optional parameters must be set, but all of them are missing. One of the following must be set: [city, country].");
    parseConfig("validation/salutation-exclusive-parameters-required-operation.xml");
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
