/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.usecases.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class TcpJmsResponseTestCase extends AbstractServiceAndFlowTestCase
{

    public TcpJmsResponseTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/usecases/sync/tcp-jms-response-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/usecases/sync/tcp-jms-response-flow.xml"}});
    }

    @Test
    public void testSyncResponse() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("tcp://localhost:4444", "request", null);
        assertNotNull(message);
        assertEquals("Received: request", message.getPayloadAsString());
    }
}
