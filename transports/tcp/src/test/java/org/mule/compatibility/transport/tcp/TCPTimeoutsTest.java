/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.client.DefaultLocalMuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class TCPTimeoutsTest extends FunctionalTestCase {

  @Rule
  public DynamicPort tcpPort = new DynamicPort("tcpPort");

  @Override
  protected String getConfigFile() {
    return "tcp-response-timeout-config.xml";
  }

  @Test
  public void testOutboundResponseTimeoutSet() throws Exception {
    final MuleClient client = new DefaultLocalMuleClient(muleContext);

    final MuleMessage result = client.send("vm://testIn", TEST_MESSAGE, null).getRight();

    assertThat(result.getPayload(), is(nullValue()));
  }
}
