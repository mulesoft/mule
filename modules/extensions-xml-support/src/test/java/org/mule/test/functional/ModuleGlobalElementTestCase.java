/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mule.runtime.core.api.exception.Errors.CORE_NAMESPACE_NAME;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.UNKNOWN_ERROR_IDENTIFIER;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import org.mule.test.petstore.extension.PetStoreClient;

import java.util.Collection;

import org.junit.Test;

public class ModuleGlobalElementTestCase extends AbstractXmlExtensionMuleArtifactFunctionalTestCase {

  @Override
  protected String getModulePath() {
    return "modules/module-global-element.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-module-global-element.xml";
  }

  @Test
  public void testDoGetClient() throws java.lang.Exception {
    assertGetClient("testDoGetClient");
  }

  @Test
  public void testDoGetClientWithPrivateOperation() throws java.lang.Exception {
    assertGetClient("testDoGetClientWithPrivateOperation");
  }

  @Test
  public void testDoGetPets() throws Exception {
    Collection<String> pets = (Collection<String>) flowRunner("testDoGetPets")
        .withVariable("ownerTest", "john")
        .run().getMessage().getPayload().getValue();
    assertThat(pets, containsInAnyOrder("la tota", "la porota"));
  }

  @Test
  public void testDoGetPetsFailWrongOwnerThrowsException() throws Exception {
    flowRunner("testDoGetPets")
        .withVariable("ownerTest", "notJohn")
        .runExpectingException(errorType(CORE_NAMESPACE_NAME, UNKNOWN_ERROR_IDENTIFIER));
  }

  private void assertGetClient(String flow) throws Exception {
    PetStoreClient client = (PetStoreClient) flowRunner(flow)
        .run().getMessage().getPayload().getValue();
    assertThat(client.getUsername(), is("john"));
    assertThat(client.getPassword(), is("notDoe"));
  }


}
