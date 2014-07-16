/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl.issues;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class MultipleConnectorsMule1765TestCase extends AbstractServiceAndFlowTestCase
{
    protected static String TEST_SSL_MESSAGE = "Test SSL Request";

    public MultipleConnectorsMule1765TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "multiple-connectors-test-service.xml"},
            {ConfigVariant.FLOW, "multiple-connectors-test-flow.xml"}});
    }

    @Test
    public void testSend() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("clientEndpoint", TEST_SSL_MESSAGE, null);
        assertEquals(TEST_SSL_MESSAGE + " Received", result.getPayloadAsString());
    }
}
