/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
public class NoReferencesToGlobalErrorHandlersValidationTestCase extends AbstractConfigFileValidationTestCase {

  @Rule
  public ExpectedException expected = none();

  @Test
  @Description("Error handlers within a reusable operation can't have references to global error handlers, neither use the default one, since it's also global")
  public void referenceToGlobalErrorHandler() throws Exception {
    expected.expect(ConfigurationException.class);
    expected.expectMessage("Error handlers within a reusable operation can't have references to global ones");
    parseConfig("validation/error-handler-referencing-global.xml");
  }

  @Test
  @Description("Error handler section can't have zero handlers")
  public void noErrorHandlersInTheErrorHandlerSection() throws Exception {
    expected.expect(ConfigurationException.class);
    expected.expectMessage("The error handler section of a try within a reusable operation must have at least one error handler");
    parseConfig("validation/try-with-empty-error-handler.xml");
  }

  @Test
  @Description("A try without error handler section is implicitly referencing the default global error handler")
  public void implicitReferenceToDefaultErrorHandler() throws Exception {
    expected.expect(ConfigurationException.class);
    expected
        .expectMessage("Try scopes within a reusable operation can't use the default error handler because it's global. You have to specify an error handler.");
    parseConfig("validation/try-without-error-handler.xml");
  }
}
