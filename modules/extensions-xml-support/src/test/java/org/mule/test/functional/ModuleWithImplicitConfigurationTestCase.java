/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import org.mule.test.petstore.extension.PetStoreClient;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

public class ModuleWithImplicitConfigurationTestCase extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

  @Override
  protected String getModulePath() {
    return "modules/module-global-element-default-params.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-module-global-element-default-params-without-config.xml";
  }

  @Test
  public void testDoGetClient() throws java.lang.Exception {
    assertGetClient("testDoGetClient");
  }

  private void assertGetClient(String flow) throws Exception {
    PetStoreClient client = (PetStoreClient) flowRunner(flow)
        .run().getMessage().getPayload().getValue();
    MatcherAssert.assertThat(client.getUsername(), CoreMatchers.is("john"));
    MatcherAssert.assertThat(client.getPassword(), CoreMatchers.is("notDoe"));
  }

}
