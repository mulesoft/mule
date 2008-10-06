/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.routing.outbound;

import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.List;

public class MulticastSyncTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/outbound/multicaster-sync-test.xml";
    }

    public void testSyncMulticast() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("vm://distributor.queue", new Apple(), null);

        assertNotNull(result);
        assertTrue(result instanceof MuleMessageCollection);
        MuleMessageCollection coll = (MuleMessageCollection) result;
        assertEquals(3, coll.size());
        List results = (List) coll.getPayload();

        assertTrue(results.contains("Apple Received in ServiceOne"));
        assertTrue(results.contains("Apple Received in ServiceTwo"));
        assertTrue(results.contains("Apple Received in ServiceThree"));
    }
}