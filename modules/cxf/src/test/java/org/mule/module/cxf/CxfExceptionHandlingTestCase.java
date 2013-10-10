/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.module.cxf.testmodels.CxfEnabledFaultMessage;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CxfExceptionHandlingTestCase extends FunctionalTestCase
{
    private static final String requestFaultPayload =
        "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "           xmlns:hi=\"http://cxf.module.mule.org/\">\n" +
            "<soap:Body>\n" +
            "<hi:sayHi>\n" +
            "    <arg0></arg0>\n" +
            "</hi:sayHi>\n" +
            "</soap:Body>\n" +
            "</soap:Envelope>";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");
    private CountDownLatch latch;

    @Override
    protected String getConfigResources()
    {
        return "onexception-conf.xml";
    }


    @Test
    public void testFaultInCxfService() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(requestFaultPayload, (Map<String,Object>)null, muleContext);
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/testServiceWithFault", request);
        assertNotNull(response);
        assertTrue(response.getPayloadAsString().contains("<faultstring>"));
    }

    @Test
    public void testFaultInCxfServiceInvokeExceptionStrategy() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(requestFaultPayload, (Map<String,Object>)null, muleContext);
        MuleClient client = new MuleClient(muleContext);
        latch = new CountDownLatch(1);
        muleContext.registerListener(new ExceptionNotificationListener() {
            public void onNotification(ServerNotification notification)
            {
                latch.countDown();
            }
        });

        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/testServiceWithFaultInvokeStrategy", request);
        assertNotNull(response);
        assertNotNull(response.getExceptionPayload());
        assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testFaultInCxfServiceInvokeComponentExceptionStrategy() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(requestFaultPayload, (Map<String,Object>)null, muleContext);
        MuleClient client = new MuleClient(muleContext);
        latch = new CountDownLatch(1);
        muleContext.registerListener(new ExceptionNotificationListener() {
            public void onNotification(ServerNotification notification)
            {
                latch.countDown();
            }
        });
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/testServiceWithFaultInvokeComponentStrategy", request);
        assertNotNull(response);
        assertNotNull(response.getExceptionPayload());
        assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
    }

}
