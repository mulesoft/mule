/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.RequestContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class ComponentStoppingEventFlowTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/components/component-stopped-processing.xml";
    }

    @Test
    public void testNullReturnStopsFlow() throws Exception
    {
        MuleClient client = muleContext.getClient();

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
