/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.routing;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class AggregatorIntegrationTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/test-correlation-aggregator.xml";
    }

    public void testAggregator() throws MuleException
    {
        String message = "test";
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("vm://distributor.queue", message, null);
        assertNotNull(result);
        assertEquals(message + message, result.getPayload());
    }
}
