/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.routing.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class ExpressionRecipientListSyncTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE,
                "org/mule/test/integration/routing/outbound/expression-recipient-list-sync-test-service.xml"},
            {ConfigVariant.FLOW,
                "org/mule/test/integration/routing/outbound/expression-recipient-list-sync-test-flow.xml"},
            {ConfigVariant.FLOW_EL,
                "org/mule/test/integration/routing/outbound/expression-recipient-list-sync-test-flow-el.xml"}});
    }

    public ExpressionRecipientListSyncTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testRecipientList() throws Exception
    {
        String message = "test";
        MuleClient client = new MuleClient(muleContext);
        Map<String, Object> props = new HashMap<String, Object>(3);
        props.put("recipient1", "vm://service1.queue?exchangePattern=request-response");
        props.put("recipient2", "vm://service2.queue?exchangePattern=request-response");
        props.put("recipient3", "vm://service3.queue?exchangePattern=request-response");
        MuleMessage result = client.send("vm://distributor.queue", message, props);

        assertNotNull(result);

        assertTrue(result instanceof MuleMessageCollection);
        MuleMessageCollection coll = (MuleMessageCollection)result;
        assertEquals(3, coll.size());
        MuleMessage[] results = coll.getMessagesAsArray();

        for (int i = 0; i < results.length; i++)
        {
            MuleMessage muleMessage = results[i];
            assertEquals("test " + (i+1) + " Received", muleMessage.getPayload());
        }
    }
}
