/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.transaction;

import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class OutboundTransactionsTestCase extends FunctionalTestCase
{

    private static final int TIMEOUT = 2000;

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/transaction/outbound-transactions.xml";
    }

    @Test
    public void testOutboundRouterTransactions() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        while (client.request("jms://my.queue1", TIMEOUT) != null)
        {
            // consume messages
        }

        while (client.request("jms://my.queue2", TIMEOUT) != null)
        {
            // consume messages
        }

        client.sendNoReceive("vm://component1", "test", null);

        assertNotNull(client.request("jms://my.queue1", TIMEOUT));
        assertNotNull(client.request("jms://my.queue2", TIMEOUT));
        assertNull(client.request("jms://my.queue1", TIMEOUT));
        assertNull(client.request("jms://my.queue2", TIMEOUT));
    }

    @Test
    public void testOutboundRouterTransactions2() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        while (client.request("jms://my.queue3", TIMEOUT) != null)
        {
            // consume messages
        }

        while (client.request("jms://my.queue4", TIMEOUT) != null)
        {
            // consume messages
        }

        client.sendNoReceive("jms://component2", "test", null);

        assertNotNull(client.request("jms://my.queue3", TIMEOUT));
        assertNotNull(client.request("jms://my.queue4", TIMEOUT));
        assertNull(client.request("jms://my.queue3", TIMEOUT));
        assertNull(client.request("jms://my.queue4", TIMEOUT));
    }

}
