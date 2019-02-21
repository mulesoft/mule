/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class TransactionExpressionFilterTestCase extends FunctionalTestCase
{

    protected static final int TIMEOUT = 5000;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/transaction/transaction-expression-filter-test-config.xml";
    }

    @Test
    public void transactionWithNestedExpressionFilterStopsFlowAndReturnsNull()
    {
        List<Integer> payload = Arrays.asList(1, 2, 3);

        MuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage(payload, mock(Map.class) , muleContext);
        try
        {
            client.dispatch("vm://transaction-filter-all", message);
            MuleMessage reply = client.request("vm://transaction-filter-all-reply", TIMEOUT);
            assertNull(reply);
        }
        catch (Exception e)
        {
            fail(String.format("Exception was thrown on executing transaction with nested expression filter: %s", e.toString()));
        }
    }
}
