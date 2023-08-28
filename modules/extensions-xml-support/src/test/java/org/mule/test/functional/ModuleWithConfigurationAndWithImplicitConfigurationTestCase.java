/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.functional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_DISABLE_XML_SDK_IMPLICIT_CONFIGURATION_CREATION;
import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import org.junit.Rule;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.petstore.extension.PetStoreClient;

import io.qameta.allure.Feature;
import org.junit.Test;

@Feature(XML_SDK)
public class ModuleWithConfigurationAndWithImplicitConfigurationTestCase
    extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

  @Rule
  public SystemProperty disableXmlSdkImplicitConfiguration =
      new SystemProperty(MULE_DISABLE_XML_SDK_IMPLICIT_CONFIGURATION_CREATION, "false");

  @Override
  protected String getModulePath() {
    return "modules/module-global-element-default-params.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-module-global-element-default-params-with-config-and-implicit-config.xml";
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
