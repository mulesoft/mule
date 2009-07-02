/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.issues;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests large requests sent to the proxy and back.
 *
 * @author lenhag
 * @version $Id$
 */
public class LargeProxyTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "largeproxytest-config.xml";
    }

    public void testLargeMessageWithEchoProxy() throws Exception
    {
        int length = 5000;
        final MuleClient client = new MuleClient();

        StringBuffer b = new StringBuffer();
        int counter = 1;
        while (b.length() < length)
        {
            // Using a counter to make it easier to see the size
            b.append(counter).append(" ");
//      b.append((char) (Math.random() * 26 + 'a'));
            counter++;
        }
        final String largeString = b.toString().trim();

        final String msg =
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
                        "<soap:Body>" +
                        "<echo xmlns=\"http://simple.component.mule.org/\">" +
                        "<echo>" + largeString + "</echo>" +
                        "</echo>" +
                        "</soap:Body>" +
                        "</soap:Envelope>";

        final CountDownLatch latch = new CountDownLatch(100);

        Runnable runnable = new Runnable()
        {

            public void run()
            {
                for (int i = 0; i < 20; i++)
                {
                    try
                    {
                        MuleMessage result = client.send("http://localhost:63082/services/EchoProxy", msg, null);
                        String payloadAsStr = result.getPayloadAsString();
                        assertTrue("The payload length should never be 0", payloadAsStr.length() != 0);
                        assertTrue(payloadAsStr.indexOf(largeString) != -1);
                        latch.countDown();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }

        };

        for (int j = 0; j < 5; j++)
        {
            new Thread(runnable).start();
        }

        latch.await(60000, TimeUnit.SECONDS);
    }


}
