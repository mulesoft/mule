/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.providers.jms.activemq;

import org.mule.extras.client.MuleClient;
import org.mule.providers.NullPayload;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class MultipleConnectorsAndTransactionsTestCase extends FunctionalTestCase
{
    private static final int TIMEOUT = 20000;

    protected String getConfigResources()
    {
        return "org/mule/test/integration/providers/jms/multiple-connectors-and-transactions-config.xml";
    }

    public void testDispatchingToSeparateEndpoints() throws Exception
    {
        String message = "testing";
        MuleClient client = new MuleClient();

        //Clear the output queue
        UMOMessage result = client.receive("client-endpoint3", 5000);
        while(result != null)
        {
            result = client.receive("client-endpoint3", 5000);
        }

        client.dispatch("client-endpoint1", message, null);

        result = client.receive("client-endpoint3", TIMEOUT);
        assertNotNull(result);

        assertFalse(result.getPayload() instanceof NullPayload);
        assertEquals(result.getPayloadAsString(), message);

        client.dispatch("client-endpoint2", message, null);

        result = client.receive("client-endpoint3", TIMEOUT);
        assertNotNull(result);
        assertFalse(result.getPayload() instanceof NullPayload);
        assertEquals(result.getPayloadAsString(), message);

        //The exception should occur on this step
        client.dispatch("client-endpoint1", message, null);

        result = client.receive("client-endpoint3", TIMEOUT);
        assertNotNull(result);
        assertFalse(result.getPayload() instanceof NullPayload);
        assertEquals(result.getPayloadAsString(), message);
        
    }

}
