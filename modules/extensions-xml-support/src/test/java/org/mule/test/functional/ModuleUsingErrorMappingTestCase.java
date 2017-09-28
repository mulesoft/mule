/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.functional.junit4.matchers.ThatMatcher.that;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ERROR_MAPPINGS;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(ERROR_HANDLING)
@Story(ERROR_MAPPINGS)
public class ModuleUsingErrorMappingTestCase extends AbstractXmlExtensionMuleArtifactFunctionalTestCase {

  private static final String CONNECT_ERROR_MESSAGE = "Could not connect.";
  private static final String UNMATCHED_ERROR_MESSAGE = "Error.";
  private static final String EXPRESSION_ERROR_MESSAGE = "Bad expression.";

  @Override
  protected String getModulePath() {
    return "modules/module-using-errormapping.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-with-module-using-errormapping.xml";
  }

  @Test
  @Description("Verifies that an unmapped error is handled as ANY.")
  public void simpleRequest() throws Exception {
    verifySuccessExpression("noMapping", UNMATCHED_ERROR_MESSAGE);
  }

  @Test
  @Description("Verifies that each error is correctly handled given an operation without mappings.")
  public void multipleMappingsDirectlyFromSmartConnector() throws Exception {
    verifyFailingExpression("multipleMappingsDirectlyFromSmartConnector", EXPRESSION_ERROR_MESSAGE);
    verifySuccessExpression("multipleMappingsDirectlyFromSmartConnector", CONNECT_ERROR_MESSAGE);
  }

  @Test
  @Description("Verifies that a mapped error via wildcard is handled.")
  public void mappedRequest() throws Exception {
    verifySuccessExpression("simpleMapping", CONNECT_ERROR_MESSAGE);
  }

  @Test
  @Description("Verifies that a mapped error via a custom matcher is handled. ")
  public void matchingMappedRequest() throws Exception {
    verifySuccessExpression("complexMapping", CONNECT_ERROR_MESSAGE);
  }

  @Test
  @Description("Verifies that an unmapped error is handled as ANY.")
  public void noMatchingMappedRequest() throws Exception {
    verifyFailingExpression("complexMapping", UNMATCHED_ERROR_MESSAGE);
  }

  @Test
  @Description("Verifies that each error is correctly handled given an operation with multiple mappings.")
  public void multipleMappingsRequest() throws Exception {
    verifyFailingExpression("multipleMappings", EXPRESSION_ERROR_MESSAGE);
    verifySuccessExpression("multipleMappings", CONNECT_ERROR_MESSAGE);
  }

  @Test
  @Description("Verifies that a mapped error via wildcard is handled through the proxy smart connector.")
  public void mappedRequestProxy() throws Exception {
    verifySuccessExpression("simpleMappingProxy", CONNECT_ERROR_MESSAGE);
  }

  @Test
  @Description("Verifies that a mapped error via a custom matcher is handled through the proxy smart connector.")
  public void matchingMappedRequestProxy() throws Exception {
    verifySuccessExpression("complexMappingProxy", CONNECT_ERROR_MESSAGE);
  }

  @Test
  @Description("Verifies that an unmapped error is handled as ANY through the proxy smart connector.")
  public void noMatchingMappedRequestProxy() throws Exception {
    verifyFailingExpression("complexMappingProxy", UNMATCHED_ERROR_MESSAGE);
  }

  @Test
  @Description("Verifies that each error is correctly handled given an operation with multiple mappings through the proxy smart connector.")
  public void multipleMappingsRequestProxy() throws Exception {
    verifyFailingExpression("multipleMappingsProxy", EXPRESSION_ERROR_MESSAGE);
    verifySuccessExpression("multipleMappingsProxy", CONNECT_ERROR_MESSAGE);
  }

  private void verifySuccessExpression(String flowName, String expectedPayload) throws Exception {
    verify(flowName, expectedPayload, false);
  }

  private void verifyFailingExpression(String flowName, String expectedPayload) throws Exception {
    verify(flowName, expectedPayload, true);
  }

  private void verify(String flowName, String expectedPayload, boolean failExpression) throws Exception {
    assertThat(flowRunner(flowName)
        .withVariable("names", emptyMap())
        .withVariable("failExpression", failExpression)
        .run().getMessage(), hasPayload(that(is(expectedPayload))));
  }

}
