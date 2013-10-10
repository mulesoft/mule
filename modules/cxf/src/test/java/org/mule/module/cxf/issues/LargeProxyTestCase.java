/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.issues;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests large requests sent to the proxy and back.
 *
 * @author lenhag
 */
public class LargeProxyTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Override
    protected String getConfigResources()
    {
        return "largeproxytest-config.xml";
    }

    @Test
    public void testLargeMessageWithEchoProxy() throws Exception
    {
        int length = 5000;
        final MuleClient client = new MuleClient(muleContext);

        StringBuffer b = new StringBuffer();
        int counter = 1;
        while (b.length() < length)
        {
            // Using a counter to make it easier to see the size
            b.append(counter).append(" ");
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
                        MuleMessage result = client.send("http://localhost:" + dynamicPort1.getNumber() + "/services/EchoProxy", msg, null);
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

        latch.await(50000, TimeUnit.SECONDS);
    }

}
