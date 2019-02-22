/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction;

import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.Is.is;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class TransactionExpressionFilterTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/transaction/transaction-expression-filter-test-config.xml";
    }

    @Test
    public void transactionWithNestedExpressionFilterEvaluatesFalseStopsFlowAndReturnsNull() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://transaction-filter-all", "ok", null);

        MuleMessage reply = client.request("vm://transaction-filter-all-reply", RECEIVE_TIMEOUT);
        assertThat(reply, nullValue());
    }

    @Test
    public void transactionWithNestedExpressionFilterEvaluatesTrueReturnsPayload() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://transaction-filter-nothing", "ok", null);

        MuleMessage reply = client.request("vm://transaction-filter-nothing-reply", RECEIVE_TIMEOUT);
        assertThat(reply.getPayload().toString(), is(equalTo("ok")));
    }
}
