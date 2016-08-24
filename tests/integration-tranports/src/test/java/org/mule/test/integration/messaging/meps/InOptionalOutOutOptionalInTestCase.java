/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

@Ignore("These tests have a property propagation / MEPs issue")
public class InOptionalOutOutOptionalInTestCase extends FunctionalTestCase {

  public static final long TIMEOUT = 3000;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/messaging/meps/pattern_In-Optional-Out_Out-Optional-In.xml";
  }

  @Test
  public void testExchange() throws Exception {
    MuleClient client = muleContext.getClient();

    MuleMessage result = client.send("inboundEndpoint", "some data", null).getRight();
    assertNotNull(result);
    assertThat(result.getPayload(), is(nullValue()));
    // TODO Even though the component returns a null the remoteSync is honoured.
    // I don't think this is right for Out-Optional-In, but probably should be the behaviour for Out-In
    assertEquals("Received", result.getInboundProperty("externalApp"));

    Map<String, Serializable> props = new HashMap<>();
    props.put("foo", "bar");
    result = client.send("inboundEndpoint", "some data", props).getRight();
    assertNotNull(result);
    assertEquals("bar header received", result.getPayload());
    assertEquals("Received", result.getInboundProperty("externalApp"));
  }
}
