/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import org.mule.api.MuleMessageCollection;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.List;

import org.junit.Test;

public class CollectionAggregatorSplitterMultipleRoutersTestCase extends FunctionalTestCase {

    private final static List<Integer> TEST_LIST = asList(1, 2, 3);

    @Override
    protected String getConfigFile()
    {
        return "collection-aggregator-splitter-multiple-routers.xml";
    }

    @Test
    public void multipleCollectionSplitterAggregatorPreservesPayload() throws Exception {
        MuleClient client = muleContext.getClient();
        runFlow("multiple-splitter-aggregator-payload-preserved", getTestMuleMessage(TEST_LIST));

        MuleMessageCollection request = (MuleMessageCollection) client.request("vm://out?connector=queue", 10000);

        assertThat(request, is(notNullValue()));
        assertThat(TEST_LIST, hasSize(request.size()));
        assertThat(TEST_LIST, contains(request.getPayloadsAsArray()));
    }

    @Test
    public void multipleCollectionSplitterAggregatorTransformedPayloadIsPreserved() throws Exception {
        MuleClient client = muleContext.getClient();
        List<Integer> resultList = asList(24, 48, 72);

        runFlow("multiple-splitter-aggregator-successive-payload-transformations", getTestMuleMessage(TEST_LIST));

        MuleMessageCollection request = (MuleMessageCollection) client.request("vm://out?connector=queue", 10000);

        assertThat(request, is(notNullValue()));
        assertThat(resultList, hasSize(request.size()));
        assertThat(resultList, contains(request.getPayloadsAsArray()));
    }
}
