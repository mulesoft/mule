/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.integration.transaction;

import org.mule.MuleManager;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.extras.client.MuleClient;
import org.mule.tck.AbstractMuleTestCase;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class OutboundTransactionsTestCase extends AbstractMuleTestCase
{
    protected void setUp() throws Exception
    {
        new MuleXmlConfigurationBuilder().configure("org/mule/test/integration/transaction/outbound-transactions.xml");
    }

    protected void tearDown() throws Exception
    {
        MuleManager.getInstance().dispose();
    }

    public void testOutboundRouterTransactions() throws Exception
    {
        MuleClient client = new MuleClient();
        client.sendNoReceive("vm://component1", "test", null);

        assertNotNull(client.receive("jms://my.queue1", 2000));
        assertNotNull(client.receive("jms://my.queue2", 2000));
        assertNull(client.receive("jms://my.queue1", 2000));
        assertNull(client.receive("jms://my.queue2", 2000));
    }

    public void testOutboundRouterTransactions2() throws Exception
    {
        MuleClient client = new MuleClient();
        client.sendNoReceive("jms://component2", "test", null);

        assertNotNull(client.receive("jms://my.queue3", 3000));
        assertNotNull(client.receive("jms://my.queue4", 3000));
        assertNull(client.receive("jms://my.queue3", 3000));
        assertNull(client.receive("jms://my.queue4", 3000));
    }
}
