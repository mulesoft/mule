/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transport.axis;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.transport.Connector;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.soap.axis.AxisConnector;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;

public abstract class AbstractAxisOverJMSWithTransactionsTestCase extends FunctionalTestCase
{
    @Test
    public void testTransactionPropertiesOnEndpoint() throws Exception
    {
        Collection<?> connectors = muleContext.getRegistry().lookupObjects(Connector.class);
        AxisConnector connector = null;
        for (Iterator<?> iterator = connectors.iterator(); iterator.hasNext();)
        {
            Connector candidate = (Connector) iterator.next();
            if (candidate instanceof AxisConnector)
            {
                connector = (AxisConnector) candidate;
            }
        }
        assertNotNull(connector);
    }

    @Test
    public void testTransactionsOverAxis() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("axis:jms://TestComponent?method=echo", getTestMuleMessage());
        MuleMessage message = client.request("jms://testout", 5000);
        assertNotNull(message.getPayload());
        assertTrue(message.getPayloadAsString().equals(TEST_PAYLOAD));
    }
}
