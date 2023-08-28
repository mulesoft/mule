/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.mule.internal.validation;

import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.PARAMETERS;

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
@Stories({@Story(OPERATIONS), @Story(PARAMETERS)})
public class OptionalParameterValidationsTestCase extends AbstractConfigFileValidationTestCase {

  @Rule
  public ExpectedException expected = none();

  @Test
  @Description("Optional parameters can't declare an expression as defaultValue")
  public void optionalParameterDefaultValueCanNotBeAnExpression() throws Exception {
    expected.expect(ConfigurationException.class);
    expected.expectMessage("An expression was given for 'defaultValue' of the optional parameter 'someparam'");
    parseConfig("validation/optional-parameter-with-expression-default.xml");
  }

  @Test
  @Description("An operation with only optional parameters is legal")
  public void operationWithOnlyOptionalParametersIsLegal() throws Exception {
    parseConfig("validation/operation-with-only-optional-parameter.xml");
  }

  @Test
  @Description("An operation empty parameters tag is legal")
  public void operationWithEmptyParametersTagIsLegal() throws Exception {
    parseConfig("validation/operation-with-empty-parameters-tag.xml");
  }
}
