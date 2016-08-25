/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.routing;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ForwardingMessageSplitterTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/usecases/routing/forwarding-message-splitter-flow.xml";
  }

  @Test
  public void testSyncResponse() throws Exception {
    MuleClient client = muleContext.getClient();

    List<Object> payload = new ArrayList<Object>();
    payload.add("hello");
    payload.add(new Integer(3));
    payload.add(new Exception());
    flowRunner("forwardingSplitter").withPayload(payload).asynchronously().run();
    MuleMessage m = client.request("test://component.1", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(m);
    assertThat(m.getPayload(), instanceOf(String.class));
    m = client.request("test://component.2", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(m);
    assertThat(m.getPayload(), instanceOf(Integer.class));

    m = client.request("test://error.queue", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(m);
    assertThat(m.getPayload(), instanceOf(Exception.class));
  }
}
