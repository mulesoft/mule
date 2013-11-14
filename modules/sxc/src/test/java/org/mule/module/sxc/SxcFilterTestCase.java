/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.sxc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

public class SxcFilterTestCase extends FunctionalTestCase
{
    private static String TestData;

    int finished = 0;

    @BeforeClass
    public static void loadTestData() throws Exception
    {
        TestData = IOUtils.toString(SxcFilterTestCase.class.getResourceAsStream("/purchase-order.xml"));
    }

    @Test
    public void testBasicXPath() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage res = client.send("vm://in", TestData, null);
        assertEquals(Boolean.TRUE, res.getPayload());
    }

    @Test
    public void testAndFilter() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage res = client.send("vm://and-filter", TestData, null);
        assertEquals(Boolean.TRUE, res.getPayload());
    }

    @Test
    public void testOrFilter() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage res = client.send("vm://or-filter", TestData, null);
        assertEquals(Boolean.TRUE, res.getPayload());
    }

    @Test
    public void testNotFilter() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage res = client.send("vm://not-filter", TestData, null);
        assertEquals(Boolean.TRUE, res.getPayload());
    }

    public void xtestBenchmark() throws Exception
    {
        MuleClient client = muleContext.getClient();

        System.out.println("Warmup");
        fire(client, TestData, 1500);

        System.out.println("Running....");
        fire(client, TestData, 1000);
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
                @Override
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
    protected String getConfigFile()
    {
        return "xpath-filter-conf.xml";
    }
}
