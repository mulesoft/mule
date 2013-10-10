/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.sxc;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SxcFilterTestCase extends FunctionalTestCase
{
    int finished = 0;

    @Test
    public void testBasicXPath() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);

        final String testData = IOUtils.toString(getClass().getResourceAsStream("/purchase-order.xml"));

        MuleMessage res = client.send("vm://in", testData, null);
        assertEquals(Boolean.TRUE, res.getPayload());
    }

    @Test
    public void testAndFilter() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);

        final String testData = IOUtils.toString(getClass().getResourceAsStream("/purchase-order.xml"));

        MuleMessage res = client.send("vm://and-filter", testData, null);

        assertEquals(Boolean.TRUE, res.getPayload());
    }

    @Test
    public void testOrFilter() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);

        final String testData = IOUtils.toString(getClass().getResourceAsStream("/purchase-order.xml"));

        MuleMessage res = client.send("vm://or-filter", testData, null);

        assertEquals(Boolean.TRUE, res.getPayload());
    }

    @Test
    public void testNotFilter() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);

        final String testData = IOUtils.toString(getClass().getResourceAsStream("/purchase-order.xml"));

        MuleMessage res = client.send("vm://not-filter", testData, null);

        assertEquals(Boolean.TRUE, res.getPayload());
    }

    public void xtestBenchmark() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);

        final String testData = IOUtils.toString(getClass().getResourceAsStream("/purchase-order.xml"));

        System.out.println("Warmup");
        fire(client, testData, 1500);

        System.out.println("Running....");

        fire(client, testData, 1000);

        Thread.sleep(1000);
    }

    private void fire(final MuleClient client, final String testData, final int count)
        throws InterruptedException
    {
        long time = System.currentTimeMillis();
        finished = 0;
        for (int i = 0; i < 10; i++)
        {
            new Thread(new Runnable()
            {
                public void run()
                {
                    for (int j = 0; j < count; j++)
                    {
                        try
                        {
                            client.send("vm://in", testData, null);
                        }
                        catch (MuleException e)
                        {
                            fail("Exception in worker thread");
                        }
                    }
                    finished++;
                }
            }).start();
        }

        while (finished < 10)
        {
            Thread.sleep(100);
        }
        System.out.println("elapsed " + (System.currentTimeMillis() - time));

    }

    @Override
    protected String getConfigResources()
    {
        return "xpath-filter-conf.xml";
    }

}
