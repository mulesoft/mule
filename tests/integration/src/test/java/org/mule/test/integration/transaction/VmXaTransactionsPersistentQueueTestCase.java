/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.transaction;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class VmXaTransactionsPersistentQueueTestCase extends AbstractServiceAndFlowTestCase
{
    private static final String TEST_MESSAGE = "TEST_MESSAGE";

    private final long timeout = getTestTimeoutMillis() / 30;

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
        MuleClient client = new MuleClient(muleContext);
        MuleMessage msg = client.send("vm://in", TEST_MESSAGE, null, (int) timeout);

        assertNotNull(msg);
        assertNull(msg.getExceptionPayload());
        assertEquals("Wrong message returned", TEST_MESSAGE + " Received", msg.getPayload());
    }
}
