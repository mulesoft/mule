/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.junit.Assert.assertEquals;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_DISABLE_XML_SDK_IMPLICIT_CONFIGURATION_CREATION;

import org.junit.Rule;
import org.junit.Test;
import org.mule.tck.junit4.rule.SystemProperty;

public class ModuleWithImplicitConfigurationCreatedTestCase extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

  @Rule
  public SystemProperty disableXmlSdkImplicitConfiguration =
      new SystemProperty(MULE_DISABLE_XML_SDK_IMPLICIT_CONFIGURATION_CREATION, "false");

  @Override
  protected String getModulePath() {
    return "modules/module-global-element-default-params-with-no-use.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-module-global-element-default-params-with-no-use.xml";
  }

  @Test
  public void testXmlPropertyWithoutImplicitConfiguration() throws Exception {
    String payload = (String) flowRunner("testSetXmlProperty").run().getMessage().getPayload().getValue();
    assertEquals(payload, "aniceproperty");
  }
}
