/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

  @Test
  public void storeInSDKCustomStore() throws Exception {
    Message message = flowRunner("customSDKStore").run().getMessage();
    assertThat(message.getPayload().getValue(), equalTo("test value"));
  }

  @Test
  public void storeInSDKCustomStoreWithNonSdkConnectionProvider() throws Exception {
    Message message = flowRunner("anotherCustomSDKStore").run().getMessage();
    assertThat(message.getPayload().getValue(), equalTo("test value"));
  }
}
