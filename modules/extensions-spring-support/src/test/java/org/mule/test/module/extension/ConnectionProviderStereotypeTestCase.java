/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.message.Message;

import org.junit.Test;

public class ConnectionProviderStereotypeTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "custom-os-config.xml";
  }

  @Test
  public void storeInCustomStore() throws Exception {
    Message message = flowRunner("customStore").run().getMessage();
    assertThat(message.getPayload().getValue(), equalTo("Extend all the things!"));
  }
}
