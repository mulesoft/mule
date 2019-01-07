/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import org.mule.api.MuleMessageCollection;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class CollectionAggregatorSplitterMultipleRoutersTestCase extends FunctionalTestCase {

    @Override
    protected String getConfigFile()
    {
        return "collection-aggregator-splitter-multiple-routers.xml";
    }

    @Test
    public void multipleCollectionSplitterAggregatorPreservesPayload() throws Exception {
        MuleClient client = muleContext.getClient();
        List<Integer> list = Arrays.asList(1, 2, 3);

        runFlow("splitter", getTestMuleMessage(list));

        MuleMessageCollection request = (MuleMessageCollection) client.request("vm://out?connector=queue", 10000);

        assertNotNull(request);
        assertEquals(list.size(), request.size());
        assertEquals(Arrays.asList(1, 2, 3), Arrays.asList(request.getPayloadsAsArray()));
    }

    @Test
    public void multipleCollectionSplitterAggregatorTransformedPayloadIsPreserved() throws Exception {
        MuleClient client = muleContext.getClient();
        List<Integer> list = Arrays.asList(1, 2, 3);

        runFlow("splitter2", getTestMuleMessage(list));

        MuleMessageCollection request = (MuleMessageCollection) client.request("vm://out?connector=queue", 10000);

        assertNotNull(request);
        assertEquals(list.size(), request.size());
        assertEquals(Arrays.asList(24, 48, 72), Arrays.asList(request.getPayloadsAsArray()));
    }
}
