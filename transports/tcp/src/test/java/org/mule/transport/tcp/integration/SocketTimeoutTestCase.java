/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.integration;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.mule.api.FutureMessageResult;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.concurrent.TimeoutException;

import org.junit.Rule;
import org.junit.Test;

public class SocketTimeoutTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    protected String getConfigResources()
    {
        return "tcp-outbound-timeout-config.xml";
    }

    @Test
    public void socketReadWriteResponseTimeout() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        FutureMessageResult result = client.sendAsync("vm://inboundTest1", "something", null);
        MuleMessage message = null;
        try
        {
            message = result.getMessage(1000);
        }
        catch (TimeoutException e)
        {
            fail("Response timeout not honored.");
        }
        assertNotNull(message);
    }

    @Test
    public void socketConnectionResponseTimeout() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        FutureMessageResult result = client.sendAsync("vm://inboundTest2", "something", null);
        MuleMessage message = null;
        try
        {
            message = result.getMessage(1000);
        }
        catch (TimeoutException e)
        {
            fail("Response timeout not honored.");
        }
        assertNotNull(message);
    }

}
