/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transformer.AbstractTransformer;
import org.mule.transport.NullPayload;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpConnector;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.cxf.interceptor.Fault;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class ExceptionStrategyTestCase extends AbstractServiceAndFlowTestCase
{
    private static final String requestPayload =
        "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "           xmlns:hi=\"http://example.cxf.module.mule.org/\">\n" +
            "<soap:Body>\n" +
            "<hi:sayHi>\n" +
            "    <arg0>Hello</arg0>\n" +
            "</hi:sayHi>\n" +
            "</soap:Body>\n" +
            "</soap:Envelope>";

    private static final String requestFaultPayload =
        "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "           xmlns:hi=\"http://cxf.module.mule.org/\">\n" +
            "<soap:Body>\n" +
            "<hi:sayHi>\n" +
            "    <arg0>Hello</arg0>\n" +
            "</hi:sayHi>\n" +
            "</soap:Body>\n" +
            "</soap:Envelope>";

    private CountDownLatch latch;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public ExceptionStrategyTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.SERVICE, "exception-strategy-service-conf.xml"},
                {ConfigVariant.FLOW, "exception-strategy-flow-conf.xml"}
        });
    }


    @Test
    public void testFaultInCxfService() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(requestFaultPayload, (Map<String,Object>)null, muleContext);
        MuleClient client = new MuleClient(muleContext);
        latch = new CountDownLatch(1);
        muleContext.registerListener(new ExceptionNotificationListener() {
            @Override
            public void onNotification(ServerNotification notification)
            {
                latch.countDown();
            }
        });
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/testServiceWithFault", request);
        assertNotNull(response);
        assertTrue(response.getPayloadAsString().contains("<faultstring>"));
        assertEquals(String.valueOf(HttpConstants.SC_INTERNAL_SERVER_ERROR), response.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
        assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testExceptionInCxfService() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(requestPayload, (Map<String,Object>)null, muleContext);
        MuleClient client = new MuleClient(muleContext);
        latch = new CountDownLatch(1);
        muleContext.registerListener(new ExceptionNotificationListener() {
            @Override
            public void onNotification(ServerNotification notification)
            {
                latch.countDown();
            }
        });
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/testServiceWithException", request);
        assertNotNull(response);
        assertTrue(response.getPayloadAsString().contains("<faultstring>"));
        assertEquals(String.valueOf(HttpConstants.SC_INTERNAL_SERVER_ERROR), response.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
        assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testClientWithTransformerExceptionDefaultException() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("hello", (Map<String,Object>)null, muleContext);
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("vm://testClientTransformerExceptionDefaultException", request);
        assertNotNull(response);
        assertTrue(response.getExceptionPayload() != null);
        assertTrue(response.getExceptionPayload().getException() instanceof MessagingException);
        assertTrue(response.getPayload() instanceof NullPayload);
    }

    @Test
    public void testClientWithFaultDefaultException() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("hello", (Map<String,Object>)null, muleContext);
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("vm://testClientWithFaultDefaultException", request);
        assertNotNull(response);
        assertTrue(response.getExceptionPayload() != null);
        assertTrue(response.getExceptionPayload().getException().getCause() instanceof Fault);
        assertTrue(response.getPayload() instanceof NullPayload);
    }

    @Test
    public void testServerClientProxyDefaultException() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        latch = new CountDownLatch(1);
        muleContext.registerListener(new ExceptionNotificationListener() {
            @Override
            public void onNotification(ServerNotification notification)
            {
                latch.countDown();
            }
        });
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/proxyExceptionStrategy", requestPayload, null);
        assertNotNull(response);
        assertTrue(response.getPayloadAsString().contains("<faultstring>"));
        assertTrue(response.getExceptionPayload() != null);
        assertEquals(String.valueOf(HttpConstants.SC_INTERNAL_SERVER_ERROR), response.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
        assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
    }

    public static class CxfTransformerThrowsExceptions extends AbstractTransformer
    {
        @Override
        protected Object doTransform(Object src, String enc) throws TransformerException
        {
            throw new TransformerException(CoreMessages.failedToBuildMessage());
        }
    }

    public static class CustomProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            return event;
        }
    }
}
