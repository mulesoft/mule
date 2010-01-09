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
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.ArrayList;
import java.util.List;

public class MulticasterAsyncTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/outbound/multicaster-async-test.xml";
    }

    public void testSplitter() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("vm://distributor.queue", new Apple(), null);

        List<Object> results = new ArrayList<Object>(3);

        MuleMessage result = client.request("vm://collector.queue", 5000);
        assertNotNull(result);
        results.add(result.getPayload());

        result = client.request("vm://collector.queue", 3000);
        assertNotNull(result);
        results.add(result.getPayload());

        result = client.request("vm://collector.queue", 3000);
        assertNotNull(result);
        results.add(result.getPayload());

        assertTrue(results.contains("Apple Received in ServiceOne"));
        assertTrue(results.contains("Apple Received in ServiceTwo"));
        assertTrue(results.contains("Apple Received in ServiceThree"));
    }

}