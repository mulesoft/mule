/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.ssl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class SslCertificateTestCase extends FunctionalTestCase {

  private static int NUM_MESSAGES = 100;

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "ssl-certificate-test.xml";
  }

  @Test
  public void testOnce() throws Exception {
    doTests(1);
  }

  @Test
  public void testMany() throws Exception {
    doTests(NUM_MESSAGES);
  }

  protected void doTests(int n) throws Exception {
    FunctionalTestComponent ftc = (FunctionalTestComponent) getComponent("service");
    assertNotNull(ftc);
    assertNotNull(ftc.getEventCallback());

    SaveCertificateCallback callback = (SaveCertificateCallback) ftc.getEventCallback();
    callback.clear();

    MuleClient client = muleContext.getClient();
    for (int i = 0; i < n; ++i) {
      callback.clear();
      String msg = TEST_MESSAGE + n;
      MuleMessage result = client.send("in", msg, null).getRight();
      assertTrue(callback.isCalled());
      assertNotNull("Null certificates", callback.getCertificates());
      assertEquals(msg + " Received", getPayloadAsString(result));
    }
  }
}
