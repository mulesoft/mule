/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.routing.CouldNotRouteOutboundMessageException;

import org.junit.Test;

public class FirstSuccessfulTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "first-successful-test.xml";
  }

  @Test
  public void testFirstSuccessful() throws Exception {
    MuleMessage response = flowRunner("test-router").withPayload("XYZ").run().getMessage();
    assertThat(getPayloadAsString(response), is("XYZ is a string"));

    response = flowRunner("test-router").withPayload(Integer.valueOf(9)).run().getMessage();
    assertThat(getPayloadAsString(response), is("9 is an integer"));

    response = flowRunner("test-router").withPayload(Long.valueOf(42)).run().getMessage();
    assertThat(getPayloadAsString(response), is("42 is a number"));

    try {
      response = flowRunner("test-router").withPayload(Boolean.TRUE).run().getMessage();
    } catch (MessagingException e) {
      assertThat(e, instanceOf(CouldNotRouteOutboundMessageException.class));
    }
  }

  @Test
  public void testFirstSuccessfulWithExpression() throws Exception {
    MuleMessage response = flowRunner("test-router2").withPayload("XYZ").run().getMessage();
    assertThat(getPayloadAsString(response), is("XYZ is a string"));
  }

  @Test
  public void testFirstSuccessfulWithExpressionAllFail() throws Exception {
    MessagingException e = flowRunner("test-router3").withPayload("XYZ").runExpectingException();
    assertThat(e, instanceOf(CouldNotRouteOutboundMessageException.class));
  }

  @Test
  public void testFirstSuccessfulWithOneWayEndpoints() throws Exception {
    flowRunner("test-router4").withPayload(TEST_MESSAGE).asynchronously().run();

    MuleClient client = muleContext.getClient();
    MuleMessage response = client.request("test://output4.out", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(response);
    assertThat(response.getPayload(), is(TEST_MESSAGE));
  }
}
