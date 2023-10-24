/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import org.junit.Test;

public class ModuleTlsEnabledTestCase extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

  @Override
  protected String getModulePath() {
    return "modules/module-tls-config.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-module-tls-config.xml";
  }

  @Override
  protected boolean shouldValidateXml() {
    return true;
  }

  @Test
  public void test() throws Exception {
    // The fact that this loads correctly without exception is already validating that the tlsContext parameter is being
    // added to the extension's config.
    // TODO W-14321226: check that it is macro-expanded correctly
  }

  @Override
  public boolean mustRegenerateComponentBuildingDefinitionRegistryFactory() {
    // returns true because not same extensions are loaded by all tests.
    return true;
  }
}
