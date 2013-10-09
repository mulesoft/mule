/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.components;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.RequestContext;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ComponentStoppingEventFlowTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/components/component-stopped-processing.xml";
    }

    @Test
    public void testNullReturnStopsFlow() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage msg = client.send("vm://in", "test data", null);
        assertNotNull(msg);
        final String payload = msg.getPayloadAsString();
        assertNotNull(payload);
        assertEquals(TEST_MESSAGE, payload);
    }

    public static final class ComponentStoppingFlow
    {

        public String process(String input)
        {
            RequestContext.getEventContext().setStopFurtherProcessing(true);
            return TEST_MESSAGE;
        }
    }
}
