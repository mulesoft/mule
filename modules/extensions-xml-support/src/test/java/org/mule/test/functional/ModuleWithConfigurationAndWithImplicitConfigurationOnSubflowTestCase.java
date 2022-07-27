/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.functional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.test.petstore.extension.PetStoreClient;

import org.junit.Test;

public class ModuleWithConfigurationAndWithImplicitConfigurationOnSubflowTestCase
    extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

  @Override
  protected String getModulePath() {
    return "modules/module-global-element-default-params.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-module-global-element-default-params-with-config-subflow.xml";
  }

  @Test
  public void testDoGetClient() throws Exception {
    PetStoreClient client = (PetStoreClient) flowRunner("testDoGetClient")
        .run().getMessage().getPayload().getValue();
    assertThat(client.getUsername(), is("john"));
    assertThat(client.getPassword(), is("notDoe"));
  }

  @Test
  public void testDoGetClientCustomProperties() throws Exception {
    PetStoreClient client = (PetStoreClient) flowRunner("testDoGetClientCustomProperties")
        .run().getMessage().getPayload().getValue();
    assertThat(client.getUsername(), is("john"));
    assertThat(client.getPassword(), is("changeit"));
  }
}
