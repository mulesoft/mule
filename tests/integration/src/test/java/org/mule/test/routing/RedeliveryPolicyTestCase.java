/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class RedeliveryPolicyTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "redelivery-policy-test.xml";
    }

    @Test
    public void testSuccess() throws Exception
    {
        for (int i = 0; i < 100; i++)
        {
            MuleClient client = new MuleClient(muleContext);
            client.send("vm://success", "hello", null);
        }
        checkNumberOfMessages("vm://dead-letter-queue", 0, 1000);
    }

    @Test
    public void testLimitedFailures() throws Exception
    {
        int success = 0;
        int failure = 0;
        for (int i = 0; i < 100; i++)
        {
            MuleClient client = new MuleClient(muleContext);
            MuleMessage msg = client.send("vm://limitedFailures", "hello", null);
            if (msg.getExceptionPayload() != null)
            {
                failure++;
            }
            else if (msg.getPayload().equals("hello"))
            {
                success++;
            }
        }
        checkNumberOfMessages("vm://dead-letter-queue", 0, 1000);
        assertEquals(25, success);
        assertEquals(75, failure);
    }

    @Test
    public void testManyRealFailures() throws Exception
    {
        int success = 0;
        int failure = 0;
        for (int i = 0; i < 12; i++)
        {
            for (int j = 0; j < 10; j++)
            {

                MuleClient client = new MuleClient(muleContext);
                String payload = "hello" + j;
                MuleMessage msg = client.send("vm://manyRealFailures", payload, null);
                if (msg.getExceptionPayload() != null)
                {
                    failure++;
                }
                else if (msg.getPayload().equals(payload))
                {
                    success++;
                }
            }
        }
        checkNumberOfMessages("vm://dead-letter-queue", 20, 1000);
        assertEquals(10, success);
        assertEquals(90, failure);
    }

    protected void checkNumberOfMessages(String url, int size, long timeout) throws MuleException
    {
        int count = 0;
        MuleClient client = new MuleClient(muleContext);
        while (client.request(url, timeout) != null)
        {
            count++;
        }
        assertEquals(size, count);
    }


    public static class FailThree
    {
        static int count = 0;

        public String process(String msg)
        {
            if (count++ % 4 != 0)
            {
                throw new RuntimeException();
            }

            return msg;
        }
    }

    public static class FailThreeOrSeven
    {
        static Map<String, Integer> counts = new HashMap<String, Integer>();

        public String process(String msg)
        {
            Integer icount = counts.get(msg);
            int count = (icount == null) ? 0 : icount;
            counts.put(msg, count + 1);
            if (count % 12 != 3 && count++ % 12 != 11)
            {
                throw new RuntimeException();
            }

            return msg;
        }
    }
}
