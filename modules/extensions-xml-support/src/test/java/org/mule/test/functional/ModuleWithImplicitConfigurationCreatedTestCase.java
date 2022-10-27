/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ModuleWithImplicitConfigurationCreatedTestCase extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

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
