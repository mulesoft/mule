/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction.xa;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.TransactionConfigEnum.ACTION_ALWAYS_BEGIN;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.transaction.XaTransactionFactory;

import org.junit.Test;

public class XaTransactionsPersistentQueueTestCase extends AbstractIntegrationTestCase
{
    private static final String TEST_MESSAGE = "TEST_MESSAGE";

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/transaction/xa-transaction-persistent-queue-flow.xml";
    }

    @Test
    public void testOutboundRouterTransactions() throws Exception
    {
        MuleClient client = muleContext.getClient();

        flowRunner("XaTestService").withPayload(TEST_MESSAGE).asynchronously().transactionally(ACTION_ALWAYS_BEGIN, new XaTransactionFactory()).run().getMessage();

        assertThat(client.request("test://finish", RECEIVE_TIMEOUT), not(nullValue()));
    }
}
