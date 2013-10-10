/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transport.NullPayload;

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
        MuleClient client = new MuleClient(muleContext);
        MuleMessage msg = client.send("vm://in", TEST_MESSAGE, null, (int) timeout);

        assertNotNull(msg);
        assertNull(msg.getExceptionPayload());
        assertEquals(NullPayload.getInstance(), msg.getPayload());
    }
}
