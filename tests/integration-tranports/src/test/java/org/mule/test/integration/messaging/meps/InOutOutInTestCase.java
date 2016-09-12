/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.api.client.MuleClient;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

@Ignore("This test causes HttpsSharePortTestCase to fail")
public class InOutOutInTestCase extends FunctionalTestCase {

  public static final long TIMEOUT = 3000;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/messaging/meps/pattern_In-Out_Out-In.xml";
  }

  @Test
  public void testExchange() throws Exception {
    MuleClient client = muleContext.getClient();

    Map<String, Serializable> props = new HashMap<>();
    props.put("foo", "bar");
    InternalMessage result = client.send("inboundEndpoint", "some data", props).getRight();
    assertNotNull(result);
    assertEquals("bar header received", result.getPayload().getValue());
  }
}
