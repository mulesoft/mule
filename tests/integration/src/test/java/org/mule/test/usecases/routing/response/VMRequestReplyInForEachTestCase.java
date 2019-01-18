/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.routing.response;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import org.mule.api.client.MuleClient;
import org.mule.api.MuleMessage;
import org.mule.DefaultMuleMessage;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.junit.Test;

public class VMRequestReplyInForEachTestCase extends RequestReplyInForEachTestCase
{
    protected static final int TIMEOUT = 5000;

    private final List<List<String>> collectionValuesNested = new ArrayList<>();
    private final List<String> collectionValuesSimple = new ArrayList<>();



    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/usecases/routing/response/vm-request-reply-in-for-each.xml";
    }

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        collectionValuesNested.add(new ArrayList<String>());
        collectionValuesNested.add(new ArrayList<String>());
        collectionValuesNested.get(0).add("value1");
        collectionValuesNested.get(0).add("value2");
        collectionValuesNested.get(0).add("value3");
        collectionValuesNested.get(1).add("value4");
        collectionValuesNested.get(1).add("value5");

        collectionValuesSimple.add("item1");
        collectionValuesSimple.add("item2");
        collectionValuesSimple.add("item3");
        collectionValuesSimple.add("item4");
        collectionValuesSimple.add("item5");
    }

    @Test
    public void testRequestReplyWithNestedForEachWithSplitAggregate() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage(collectionValuesNested, mock(Map.class) , muleContext);
        client.dispatch("vm://foreach-spag", message);
        for(List<String> sublist: collectionValuesNested)
        {
            assertResultCollection(client, sublist, "-processed", "test-foreach-spag-reply");
        }
    }

    @Test
    public void testRequestReplyWithSimpleForEachWithSplitAggregate() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage(collectionValuesSimple, mock(Map.class), muleContext);
        client.dispatch("vm://foreach-spag", message);
        assertResultCollection(client, collectionValuesSimple,"-processed","test-foreach-spag-reply");
    }


    private void assertResultCollection(MuleClient client, List<String> payload, String suffix, String queueName) throws Exception
    {
        MuleMessage reply = client.request("vm://" + queueName, TIMEOUT);
        assertThat(reply, is(notNullValue()));
        List<String> result = (List) reply.getPayload();
        assertThat(result, hasSize(payload.size()));
        for (String value : payload)
        {
            assertThat(result, hasItem(value + suffix));
        }
    }
}
