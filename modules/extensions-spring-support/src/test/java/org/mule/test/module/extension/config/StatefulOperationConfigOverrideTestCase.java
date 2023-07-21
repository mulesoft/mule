/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Test;

public class StatefulOperationConfigOverrideTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "stateful-override-config.xml";
  }

  @Test
  public void statefulOverride() throws Exception {
    String response = flowRunner("statefulOverride").run().getMessage().getPayload().getValue().toString();
    assertThat(response, equalTo("implicit-config-implicit 42"));
  }
}
