/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.routing.response;

import org.junit.Test;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class VMRequestReplyInForEachTestCase extends RequestReplyInForEachTestCase
{
    private final List<List<String>> collectionValues = new ArrayList<>();

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/usecases/routing/response/vm-request-reply-in-for-each.xml";
    }

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        collectionValues.add(new ArrayList<String>());
        collectionValues.add(new ArrayList<String>());
        collectionValues.get(0).add("value1");
        collectionValues.get(0).add("value2");
        collectionValues.get(0).add("value3");
        collectionValues.get(1).add("value4");
        collectionValues.get(1).add("value5");
    }

    @Test
    public void testRequestReplyWithForEachWithSplitAggregate() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage(collectionValues, mock(Map.class) , muleContext);
        client.dispatch("vm://foreach-spag", message);
        for(List<String> sublist: collectionValues )
        {
            assertResultCollection(client, sublist, "-processed", "test-foreach-spag-reply");
        }
    }


    private void assertResultCollection(MuleClient client, List<String> payload, String suffix, String queueName) throws Exception
    {
        MuleMessage reply = client.request("vm://" + queueName, TIMEOUT);
        assertThat(reply, is(notNullValue()));
        List<String> result = (List) reply.getPayload();
        assertThat(result, hasSize(payload.size()));
        for (String value : payload) {
            assertThat(result, hasItem(value + suffix));
        }
    }
}
