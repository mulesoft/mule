/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.resolvers;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.Base64;

import java.util.List;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DefaultEntryPointResolverSetMultithreadingTestCase extends FunctionalTestCase
{
    
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/resolvers/default-entry-point-resolver-multithreading-test-config.xml";
    }
    
    @Override
    public int getTestTimeoutSecs()
    {
        return 120;
    }

    @Test
    public void testMultithreaded() throws Exception
    {
        final int numberOfThreads = 50;
        final int requestCount = 100;
        ClientRequest[] clients = new ClientRequest[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++)
        {
            clients[i] = new ClientRequest(requestCount);
        }

        for (ClientRequest clientRequest : clients)
        {
            clientRequest.start();
            try
            {
                Thread.sleep(5);
            }
            catch (InterruptedException ie)
            {
                // ignore
            }
        }

        for (int i = 0; i < numberOfThreads; i++)
        {
            try
            {
                clients[i].join();
            }
            catch (InterruptedException ie)
            {
                // ignore
            }
        }
    }

    private static class ClientRequest extends Thread
    {
        final MuleClient client;
        int requestCount;

        private ClientRequest(final int requestCount) throws MuleException
        {
            client = new MuleClient(muleContext);
            this.requestCount = requestCount;
        }

        @Override
        public void run()
        {
            final byte[] payload = createPayload();

            while (--requestCount >= 0)
            {
                try
                {
                    final MuleMessage outbound = client.send("vm://test.inbound.sync", payload, null);
                    assertNull(outbound.getExceptionPayload());
                    assertNotNull(outbound.getPayload());
                    byte[] bytes = null;
                    if (outbound.getPayload() instanceof byte[])
                    {
                        bytes = (byte[]) outbound.getPayload();
                    }
                    else if (outbound.getPayload() instanceof List)
                    {
                        final List<?> list = (List<?>) outbound.getPayload();
                        assertEquals(1, list.size());
                        assertTrue(list.get(0) instanceof byte[]);
                        bytes = (byte[]) list.get(0);
                    }
                    else
                    {
                        fail("unexpected payload type");
                    }
                    assertEquals(Base64.encodeBytes(payload), Base64.encodeBytes(bytes));
                }
                catch (Exception e)
                {
                    fail("failed with exception: " + e);
                }
            }
        }

        private byte[] createPayload()
        {
            Random random = new Random();
            final int size = 55;
            byte[] payload = new byte[size];
            random.nextBytes(payload);
            return payload;
        }
    }
    
    public static class EchoBytes
    {
        public byte[] echo(byte[] input)
        {
            return input;
        }
    }
}
