/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

//@RunWith(FlakinessDetectorTestRunner.class)
public class HttpMessageReceiverSelectionTestCase extends FunctionalTestCase
{
    private static final String FLOW_A = "flowA";
    private static final String FLOW_B = "flowB";
    private static final String FLOW_C = "flowC";
    private static final String FLOW_D = "flowD";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("http.port");

    @Override
    protected String getConfigFile()
    {
        return "http-message-receiver-selection-config.xml";
    }

    @Test
    public void testHttpMessageReceiverSelectionTestCase() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage response = client.send(getUrl() + FLOW_A, TEST_MESSAGE, null);
        assertEquals(FLOW_A, response.getPayloadAsString());
        response = client.send(getUrl() + FLOW_A + "/" + FLOW_B, TEST_MESSAGE, null);
        assertEquals(FLOW_B, response.getPayloadAsString());
        response = client.send(getUrl() + FLOW_C, TEST_MESSAGE, null);
        assertEquals(FLOW_C, response.getPayloadAsString());
        response = client.send(getUrl() + FLOW_D, TEST_MESSAGE, null);
        assertEquals(FLOW_D, response.getPayloadAsString());

        response = client.send(getUrl() + FLOW_A + "/extra", TEST_MESSAGE, null);
        assertEquals(FLOW_A, response.getPayloadAsString());
        response = client.send(getUrl() + FLOW_A + "/" + FLOW_B + "/extra", TEST_MESSAGE, null);
        assertEquals(FLOW_B, response.getPayloadAsString());
    }

    private String getUrl()
    {
        return "http://localhost:" + dynamicPort.getNumber() + "/";
    }


}
