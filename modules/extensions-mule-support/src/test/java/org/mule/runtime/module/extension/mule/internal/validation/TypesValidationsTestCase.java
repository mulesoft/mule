/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.validation;

import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.PARAMETERS;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.TYPES_CATALOG;

import org.mule.runtime.core.api.config.ConfigurationException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(REUSE)
@Stories({@Story(OPERATIONS), @Story(PARAMETERS), @Story(TYPES_CATALOG)})
public class TypesValidationsTestCase extends AbstractConfigFileValidationTestCase {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Test
  @Description("Operation must have an output element")
  public void operationMustHaveAnOutputElement() throws Exception {
    expected.expect(ConfigurationException.class);
    expected.expectMessage("Operation 'operationWithoutOutput' is missing its <output> declaration");
    parseConfig("validation/no-output-operation.xml");
  }

  @Test
  @Description("Operation output must have a payload-type element")
  public void operationOutputMustHaveAPayloadTypeElement() throws Exception {
    expected.expect(ConfigurationException.class);
    expected.expectMessage("Operation 'outputWithoutPayloadType' is missing its <payload-type> declaration");
    parseConfig("validation/output-without-payload-type-operation.xml");
  }

  @Test
  @Description("Operation output must have a payload-type present in the ApplicationTypeLoader")
  public void operationOutputMustHaveAPayloadTypePresentInTheApplicationTypeLoader() throws Exception {
    expected.expect(ConfigurationException.class);
    expected
        .expectMessage("Component <operation:payload-type> defines type as 'invalid' but such type is not defined in the application");
    parseConfig("validation/output-with-invalid-payload-type-operation.xml");
  }

  @Test
  @Description("Operation output payload-type can be a primitive")
  public void payloadTypeStringIsOk() throws Exception {
    parseConfig("validation/output-with-string-payload-type-operation.xml");
  }

  @Test
  @Description("Operation output payload-type can be void")
  public void payloadTypeVoidIsOk() throws Exception {
    parseConfig("validation/output-with-void-payload-type-operation.xml");
  }

  @Test
  @Description("Parameter type can't be void")
  public void parameterTypeCanNotBeVoid() throws Exception {
    expected.expect(ConfigurationException.class);
    expected.expectMessage("Parameter 'someparam' references type 'void', which is forbidden for parameters");
    parseConfig("validation/parameter-with-void-type.xml");
  }

  @Test
  @Description("Parameter type can be a primitive")
  public void parameterTypeCanBeString() throws Exception {
    parseConfig("validation/parameter-with-string-type.xml");
  }
}
