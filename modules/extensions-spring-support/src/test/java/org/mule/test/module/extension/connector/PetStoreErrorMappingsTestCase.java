/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.connector;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.functional.junit4.matchers.ThatMatcher.that;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ERROR_MAPPINGS;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(ERROR_HANDLING)
@Story(ERROR_MAPPINGS)
public class PetStoreErrorMappingsTestCase extends AbstractExtensionFunctionalTestCase {

  private static final String CONNECT_ERROR_MESSAGE = "Could not connect.";
  private static final String UNMATCHED_ERROR_MESSAGE = "Error.";
  private static final String EXPRESSION_ERROR_MESSAGE = "Bad expression.";

  @Override
  protected String getConfigFile() {
    return "petstore-error-mappings.xml";
  }

  @Test
  @Description("Verifies that an unmapped error is handled as ANY.")
  public void simpleRequest() throws Exception {
    verify("noMapping", UNMATCHED_ERROR_MESSAGE);
  }

  @Test
  @Description("Verifies that a mapped error via wildcard is handled.")
  public void mappedRequest() throws Exception {
    verify("simpleMapping", CONNECT_ERROR_MESSAGE);
  }

  @Test
  @Description("Verifies that a mapped error via a custom matcher is handled. ")
  public void matchingMappedRequest() throws Exception {
    verify("complexMapping", CONNECT_ERROR_MESSAGE);
  }

  @Test
  @Description("Verifies that an unmapped error is handled as ANY.")
  public void noMatchingMappedRequest() throws Exception {
    verifyWithFailingExpression("complexMappingWithFailingExpression", UNMATCHED_ERROR_MESSAGE);
  }

  @Test
  @Description("Verifies that each error is correctly handled given an operation with multiple mappings.")
  public void multipleMappingsRequest() throws Exception {
    verifyWithFailingExpression("multipleMappings", EXPRESSION_ERROR_MESSAGE);
    verify("multipleMappings", CONNECT_ERROR_MESSAGE);
  }

  private void verifyWithFailingExpression(String flowName, String expectedPayload) throws Exception {
    assertThat(flowRunner(flowName)
        .withVariable("failExpression", true)
        .run().getMessage(), hasPayload(that(is(expectedPayload))));
  }

  private void verify(String flowName, String expectedPayload) throws Exception {
    verify(flowName, expectedPayload, emptyMap());
  }

  private void verify(String flowName, String expectedPayload, Object petNames) throws Exception {
    assertThat(flowRunner(flowName).withVariable("names", petNames).run().getMessage(), hasPayload(that(is(expectedPayload))));
  }
}
