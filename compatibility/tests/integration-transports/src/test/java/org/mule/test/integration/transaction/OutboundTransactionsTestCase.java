/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.compatibility.module.client.MuleClient;
import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OutboundTransactionsTestCase extends CompatibilityFunctionalTestCase {

  private static final int TIMEOUT = 2000;

  private MuleClient client;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/transaction/outbound-transactions.xml";
  }

  @Before
  public void before() throws MuleException {
    client = new MuleClient(muleContext);
  }

  @After
  public void after() {
    client.dispose();
  }

  @Test
  public void testOutboundRouterTransactions() throws Exception {
    while (client.request("jms://my.queue1", TIMEOUT) != null) {
      // consume messages
    }

    while (client.request("jms://my.queue2", TIMEOUT) != null) {
      // consume messages
    }

    client.sendNoReceive("vm://component1", "test", null);

    assertNotNull(client.request("jms://my.queue1", TIMEOUT));
    assertNotNull(client.request("jms://my.queue2", TIMEOUT));
    assertNull(client.request("jms://my.queue1", TIMEOUT));
    assertNull(client.request("jms://my.queue2", TIMEOUT));
  }

  @Test
  public void testOutboundRouterTransactions2() throws Exception {
    while (client.request("jms://my.queue3", TIMEOUT) != null) {
      // consume messages
    }

    while (client.request("jms://my.queue4", TIMEOUT) != null) {
      // consume messages
    }

    client.sendNoReceive("jms://component2", "test", null);

    assertNotNull(client.request("jms://my.queue3", TIMEOUT));
    assertNotNull(client.request("jms://my.queue4", TIMEOUT));
    assertNull(client.request("jms://my.queue3", TIMEOUT));
    assertNull(client.request("jms://my.queue4", TIMEOUT));
  }
}
