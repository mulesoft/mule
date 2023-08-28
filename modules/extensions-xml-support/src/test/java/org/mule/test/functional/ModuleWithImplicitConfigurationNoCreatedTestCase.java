/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.functional;

import static org.junit.Assert.assertNull;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_DISABLE_XML_SDK_IMPLICIT_CONFIGURATION_CREATION;

import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

public class ModuleWithImplicitConfigurationNoCreatedTestCase extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

  @Rule
  public SystemProperty disableXmlSdkImplicitConfiguration =
      new SystemProperty(MULE_DISABLE_XML_SDK_IMPLICIT_CONFIGURATION_CREATION, "true");

  @Override
  protected String getModulePath() {
    return "modules/module-global-element-default-params-with-no-use.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-module-global-element-default-params-with-no-use.xml";
  }

  @Test
  public void testXmlPropertyWithoutImplicitConfiguration() throws java.lang.Exception {
    String payload = (String) flowRunner("testSetXmlProperty").run().getMessage().getPayload().getValue();
    assertNull(payload);
  }
}
