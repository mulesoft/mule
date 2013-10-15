/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl.issues;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MultipleConnectorsMule1765TestCase extends FunctionalTestCase
{

    protected static String TEST_SSL_MESSAGE = "Test SSL Request";

    @Override
    protected String getConfigResources()
    {
        return "multiple-connectors-test.xml";
    }

    @Test
    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("clientEndpoint", TEST_SSL_MESSAGE, null);
        assertEquals(TEST_SSL_MESSAGE + " Received", result.getPayloadAsString());
    }
}
