/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.NullPayload;
import org.mule.RequestContext;

public class ComponentStoppingEventFlowTestCase extends FunctionalTestCase
{
    private static final String TEST_DATA_STRING = "this data should not be returned";

    protected String getConfigResources()
    {
        return "org/mule/test/components/component-stopped-processing.xml";
    }

    public void testNullReturnStopsFlow() throws Exception
    {
        MuleClient client = new MuleClient();

        MuleMessage msg = client.send("vm://in", "test data", null);
        assertNotNull(msg);
        final String payload = msg.getPayloadAsString();
        assertNotNull(payload);
        assertEquals(TEST_DATA_STRING, payload);
    }

    public static final class ComponentStoppingFlow
    {


        public String process(String input)
        {
            RequestContext.getEventContext().setStopFurtherProcessing(true);
            return TEST_DATA_STRING;
        }
    }
}