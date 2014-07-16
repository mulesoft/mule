/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction.xa;

import static org.junit.Assert.assertNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class VmXaTransactionsPersistentQueueTestCase extends AbstractServiceAndFlowTestCase
{
    private static final String TEST_MESSAGE = "TEST_MESSAGE";

    private final long timeout = getTestTimeoutSecs() * 1000 / 30;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE,
                "org/mule/test/integration/transaction/vm-xa-transaction-persistent-queue-service.xml"},
            {ConfigVariant.FLOW,
                "org/mule/test/integration/transaction/vm-xa-transaction-persistent-queue-flow.xml"}});
    }

    public VmXaTransactionsPersistentQueueTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testOutboundRouterTransactions() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage msg = client.send("vm://in", TEST_MESSAGE, null, (int) timeout);
        assertNull(msg);
    }
}
