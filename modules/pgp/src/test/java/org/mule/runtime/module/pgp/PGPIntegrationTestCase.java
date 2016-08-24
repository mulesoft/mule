/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.pgp;

import static org.junit.Assert.assertEquals;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.functional.junit4.FunctionalTestCase;

import org.junit.Test;

public class PGPIntegrationTestCase extends FunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "pgp-integration-mule-config-flow.xml";
  }

  @Test
  public void testEncryptDecrypt() throws Exception {
    doEncryptDecryptTest("this is a super simple test. Hope it works!!!");
  }

  @Test
  public void testEncryptDecryptEmptyPayload() throws Exception {
    doEncryptDecryptTest("");
  }

  private void doEncryptDecryptTest(String payload) throws Exception {
    flowRunner("pgpEncryptProcessor").withPayload(payload).run();
    MuleClient client = muleContext.getClient();
    MuleMessage message = client.request("test://out", 5000).getRight().get();
    assertEquals(payload, getPayloadAsString(message));
  }
}
