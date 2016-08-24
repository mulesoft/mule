/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import org.junit.Test;

/**
 * This test has been re-written to use entry point resolvers.
 */
public class NoArgsCallWrapperFunctionalTestCase extends AbstractIntegrationTestCase {

  private static final int RECEIVE_TIMEOUT = 5000;

  @Override
  protected String getConfigFile() {
    return "no-args-call-wrapper-config-flow.xml";
  }

  @Test
  public void testNoArgsCallWrapper() throws Exception {
    MuleClient client = muleContext.getClient();
    flowRunner("WrapperUMO").withPayload("test").asynchronously().run();
    MuleMessage reply = client.request("test://out", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(reply);
    assertThat(reply.getPayload(), is("Just an apple."));
  }

  @Test
  public void testWithInjectedDelegate() throws Exception {
    MuleClient client = muleContext.getClient();
    flowRunner("WrapperUMOInjected").withPayload("test").asynchronously().run();
    MuleMessage reply = client.request("test://outWithInjected", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(reply);
    // same as original input
    assertThat(reply.getPayload(), is("test"));
  }
}
