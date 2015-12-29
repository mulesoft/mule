/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.functional.junit4.FunctionalTestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ExpressionRecipientListSyncTestCase extends FunctionalTestCase
{

    protected ConfigVariant variant;
    protected String configResources;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.FLOW,
                "org/mule/test/integration/routing/outbound/expression-recipient-list-sync-test-flow.xml"},
            {ConfigVariant.FLOW_EL,
                "org/mule/test/integration/routing/outbound/expression-recipient-list-sync-test-flow-el.xml"}});
    }

    public ExpressionRecipientListSyncTestCase(ConfigVariant variant, String configResources)
    {
        this.variant = variant;
        this.configResources = configResources;
    }

    @Test
    public void testRecipientList() throws Exception
    {
        String message = "test";

        MuleClient client = muleContext.getClient();
        Map<String, Object> props = new HashMap<String, Object>(3);
        props.put("recipient1", "vm://service1.queue?exchangePattern=request-response");
        props.put("recipient2", "vm://service2.queue?exchangePattern=request-response");
        props.put("recipient3", "vm://service3.queue?exchangePattern=request-response");
        MuleMessage result = client.send("vm://distributor.queue", message, props);

        assertNotNull(result);

        assertTrue(result.getPayload() instanceof List);
        List<MuleMessage> results = (List<MuleMessage>) result.getPayload();
        assertEquals(3, results.size());
        MuleMessage[] resultsArray = results.toArray(new MuleMessage[3]);

        for (int i = 0; i < resultsArray.length; i++)
        {
            MuleMessage muleMessage = resultsArray[i];
            assertEquals("test " + (i+1) + " Received", muleMessage.getPayload());
        }
    }

    public static enum ConfigVariant
    {
        FLOW, FLOW_EL
    }

    @Override
    public String getConfigResources()
    {
        return configResources;
    }
}
