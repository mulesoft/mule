/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import org.junit.Test;

public class TcpToFileTestCase extends FunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/usecases/sync/tcp-to-file.xml";
  }

  @Test
  public void testSyncResponse() throws Exception {
    MuleClient client = muleContext.getClient();
    String payload = "payload";

    client.send("tcp://localhost:4444", payload, null);

    MuleMessage msg = client.request("file://temp/tests/mule", 10000).getRight().get();
    assertNotNull(msg);
    assertEquals(payload, getPayloadAsString(msg));
  }
}
