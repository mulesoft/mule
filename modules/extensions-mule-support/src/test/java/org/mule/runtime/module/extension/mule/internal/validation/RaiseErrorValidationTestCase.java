/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.validation;

import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;

import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.core.api.config.ConfigurationException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(REUSE)
@Stories({@Story(OPERATIONS), @Story(ERROR_HANDLING)})
public class RaiseErrorValidationTestCase extends AbstractConfigFileValidationTestCase {

  @Rule
  public ExpectedException expected = none();

  @Test
  @Description("Operation can't use mule:raise-error")
  public void operationCanNotUseCoreRaiseError() throws Exception {
    expected.expect(ConfigurationException.class);
    expected
        .expectMessage("Usages of the component 'raise-error' are not allowed inside a Mule SDK Operation Definition (operation:def)");
    parseConfig("validation/operation-with-core-raise-error.xml");
  }

  @Test
  @Description("Operation's raise-error is not allowed to specify a namespace")
  public void operationRaiseErrorIsNotAllowedToSpecifyNamespace() throws Exception {
    expected.expect(ConfigurationException.class);
    expected
        .expectMessage("Operation raise error component (operation:raise-error) is not allowed to specify a namespace: 'APP'");
    parseConfig("validation/operation-raise-error-specifying-namespace.xml");
  }

  @Test
  @Description("Operation's raise-error is not allowed to specify THIS namespace")
  public void operationRaiseErrorIsNotAllowedToSpecifyNamespaceThis() throws Exception {
    expected.expect(ConfigurationException.class);
    expected
        .expectMessage("Operation raise error component (operation:raise-error) is not allowed to specify a namespace: 'THIS'");
    parseConfig("validation/operation-raise-error-specifying-namespace-this.xml");
  }

  @Test
  @Description("Operation's raise-error allowed configs don't fail")
  public void operationRaiseErrorAllowedConfigs() throws Exception {
    parseConfig("validation/operation-raise-error-allowed-usages.xml");
  }
}
