/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connector;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.functional.junit4.matchers.ThatMatcher.that;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.test.petstore.extension.PetStoreConnector;
import org.mule.test.petstore.extension.PetStoreOperations;
import org.mule.test.petstore.extension.SimplePetStoreConnectionProvider;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Error Handling")
@Stories("Error Mappings")
public class PetStoreErrorMappingsTestCase extends ExtensionFunctionalTestCase {

  private static final String CONNECT_ERROR_MESSAGE = "Could not connect.";
  private static final String UNMATCHED_ERROR_MESSAGE = "Error.";
  private static final String EXPRESSION_ERROR_MESSAGE = "Bad expression.";

  @Override
  protected String getConfigFile() {
    return "petstore-error-mappings.xml";
  }

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {PetStoreConnectorWithFailingOperation.class};
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
    verify("complexMapping", UNMATCHED_ERROR_MESSAGE, "Potato!");
  }

  @Test
  @Description("Verifies that each error is correctly handled given an operation with multiple mappings.")
  public void multipleMappingsRequest() throws Exception {
    verify("multipleMappings", EXPRESSION_ERROR_MESSAGE, "Potato!");
    verify("multipleMappings", CONNECT_ERROR_MESSAGE);
  }

  private void verify(String flowName, String expectedPayload) throws Exception {
    verify(flowName, expectedPayload, emptyMap());
  }

  private void verify(String flowName, String expectedPayload, Object petNames) throws Exception {
    assertThat(flowRunner(flowName).withVariable("names", petNames).run().getMessage(), hasPayload(that(is(expectedPayload))));
  }

  @Extension(name = "petstore", description = "PetStore Test connector")
  @Operations(PetStoreErrorMappingsTestCase.PetStoreFailingOperations.class)
  @ConnectionProviders(SimplePetStoreConnectionProvider.class)
  @Xml(namespace = "http://www.mulesoft.org/schema/mule/petstore", prefix = "petstore")
  public static class PetStoreConnectorWithFailingOperation extends PetStoreConnector {
  }

  public static class PetStoreFailingOperations extends PetStoreOperations {

    public String fail(Map<String, String> petNames) throws Exception {
      throw new IOException("Failed operation");
    }

  }

}
