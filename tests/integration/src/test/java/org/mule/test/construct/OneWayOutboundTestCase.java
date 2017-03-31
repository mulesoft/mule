/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.junit.Assert.assertEquals;
import org.mule.runtime.api.message.Message;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public abstract class OneWayOutboundTestCase extends AbstractIntegrationTestCase {

  @Test
  public void noOutbound() throws Exception {
    Message response = flowRunner("noOutbound").withPayload("TEST").run().getMessage();
    assertEquals("TEST processed", response.getPayload().getValue());
  }

  @Test
  public void noOutboundEndpointAsync() throws Exception {
    Message response = flowRunner("noOutboundAsync").withPayload("TEST").run().getMessage();
    assertEquals("TEST", response.getPayload().getValue());
  }

  @Test
  public void oneWayOutbound() throws Exception {
    Message response = flowRunner("oneWayOutbound").withPayload("TEST").run().getMessage();
    assertOneWayOutboundResponse(response);
  }

  protected abstract void assertOneWayOutboundResponse(Message response);

  @Test
  public void oneWayOutboundAfterComponent() throws Exception {
    Message response = flowRunner("oneWayOutboundAfterComponent").withPayload("TEST").run().getMessage();
    assertOneWayOutboundAfterComponentResponse(response);
  }

  protected abstract void assertOneWayOutboundAfterComponentResponse(Message response);

  @Test
  public void oneWayOutboundBeforeComponent() throws Exception {
    Message response = flowRunner("oneWayOutboundBeforeComponent").withPayload("TEST").run().getMessage();
    assertEquals("TEST processed", response.getPayload().getValue());
  }
}

