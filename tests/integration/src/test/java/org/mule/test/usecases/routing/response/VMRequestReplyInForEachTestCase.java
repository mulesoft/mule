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

import org.junit.Test;
import com.google.common.collect.ImmutableList;

public class VMRequestReplyInForEachTestCase extends RequestReplyInForEachTestCase
{
    private ImmutableList<ImmutableList<String>> collectionValues;
    
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/usecases/routing/response/vm-request-reply-in-for-each.xml";
    }

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        ImmutableList<String> nestedListA = ImmutableList.<String>builder().add("value1")
                .add("value2")
                .add("value3")
                .build();

        ImmutableList<String> nestedListB = ImmutableList.<String>builder().add("value4")
                .add("value5")
                .build();

        collectionValues = ImmutableList.<ImmutableList<String>>builder().add(nestedListA).add(nestedListB).build();
    }

    @Test
    public void testRequestReplyWithNestedForEachWithSplitAggregate() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage(collectionValues, mock(Map.class) , muleContext);
        client.dispatch("vm://foreach-spag", message);
        for(List<String> sublist: collectionValues)
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
       for (String value : payload)
       {
           assertThat(result, hasItem(value + suffix));
       }
    }
}
