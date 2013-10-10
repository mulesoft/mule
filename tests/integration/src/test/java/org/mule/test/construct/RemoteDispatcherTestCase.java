/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.construct;

import org.mule.api.ExceptionPayload;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.module.client.RemoteDispatcher;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test remote dispatcher using serialization wire format
 */
public class RemoteDispatcherTestCase extends AbstractServiceAndFlowTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/construct/remote-dispatcher.xml"}

        });
    }

    public RemoteDispatcherTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testRemoting() throws Exception
    {
        String[] targets = {"service1", "nosuch", "vmConnector", "flow1"};
        String expectedResponses[] = {"Hellogoodbye", null, null, "Helloaloha"};

        MuleClient client = new MuleClient(muleContext);
        RemoteDispatcher dispatcher = client.getRemoteDispatcher("http://localhost:" + port1.getNumber());
        for (int i = 0; i < targets.length; i++)
        {
            String construct = targets[i];
            String expected = expectedResponses[i];

            MuleMessage result = dispatcher.sendToRemoteComponent(construct, "Hello", null);

            assertNotNull(result);
            if (expected != null)
            {
                assertEquals(expected, result.getPayload());
            }
            else
            {
                ExceptionPayload payload = result.getExceptionPayload();
                assertNotNull(payload);
                assertTrue(payload.getException().getMessage().contains(construct));
            }
        }
    }
}
