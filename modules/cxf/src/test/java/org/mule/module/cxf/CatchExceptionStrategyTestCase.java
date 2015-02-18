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
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.exception.TemplateMessagingExceptionStrategy;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transformer.AbstractTransformer;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.cxf.interceptor.Fault;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;


public class CatchExceptionStrategyTestCase extends AbstractServiceAndFlowTestCase
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

    public static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(org.mule.module.http.api.HttpConstants.Methods.POST.name()).disableStatusCodeValidation().build();

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public CatchExceptionStrategyTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.SERVICE, "catch-exception-strategy-service-conf.xml"},
                {ConfigVariant.FLOW, "catch-exception-strategy-flow-conf.xml"},
                {ConfigVariant.FLOW, "catch-exception-strategy-flow-conf-httpn.xml"}
        });
    }


    @Test
    public void testFaultInCxfServiceWithCatchExceptionStrategy() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(requestFaultPayload, (Map<String,Object>)null, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/testServiceWithFaultCatchException", request, HTTP_REQUEST_OPTIONS);
        assertNotNull(response);
        assertEquals(String.valueOf(HttpConstants.SC_OK), response.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY).toString());
        assertTrue(response.getPayloadAsString().contains("Anonymous"));
    }

    @Test
    public void testFaultInCxfServiceWithCatchExceptionStrategyRethrown() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(requestFaultPayload, (Map<String,Object>)null, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/testServiceWithFaultCatchExceptionRethrown", request, HTTP_REQUEST_OPTIONS);
        assertNotNull(response);
        assertEquals(String.valueOf(HttpConstants.SC_INTERNAL_SERVER_ERROR), response.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY).toString());
        assertTrue(response.getPayloadAsString().contains("<faultstring>"));
    }

    @Test
    public void testExceptionThrownInTransformerWithCatchExceptionStrategy() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(requestPayload, (Map<String,Object>)null, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/testTransformerExceptionCatchException", request, HTTP_REQUEST_OPTIONS);
        assertNotNull(response);
        assertEquals(String.valueOf(HttpConstants.SC_OK), response.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY).toString());
        assertTrue(response.getPayloadAsString().contains("APPEND"));
    }

    @Test
    public void testClientWithSOAPFaultCatchException() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("hello", (Map<String,Object>)null, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://testClientSOAPFaultCatchException", request);
        assertNotNull(response);
        assertTrue(response.getExceptionPayload() == null);
    }

    @Test
    public void testClientWithSOAPFaultCatchExceptionRedirect() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("TEST", (Map<String,Object>)null, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://testClientSOAPFaultCatchExceptionRedirect", request);
        assertNotNull(response);
        assertTrue(response.getPayloadAsString().contains("TEST"));
        assertTrue(response.getExceptionPayload() == null);
    }

    @Test
    public void testClientWithTransformerExceptionCatchException() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("hello", (Map<String,Object>)null, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://testClientTransformerExceptionCatchException", request);
        assertNotNull(response);
        assertTrue(response.getPayloadAsString().contains(" Anonymous"));
    }

    @Test
    public void testServerClientProxyWithTransformerExceptionCatchStrategy() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/testProxyWithTransformerExceptionCatchStrategy", getTestMuleMessage(requestPayload), HTTP_REQUEST_OPTIONS);
        String resString = result.getPayloadAsString();
        assertEquals(String.valueOf(HttpConstants.SC_OK), result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY).toString());
        assertTrue(resString.contains("Anonymous"));
    }

    public static class ProxyCustomProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            String payload = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns2:sayHiResponse xmlns:ns2=\"http://example.cxf.module.mule.org/\"><return>Hello Anonymous</return></ns2:sayHiResponse></soap:Body></soap:Envelope>";
            event.getMessage().setPayload(payload);
            return event;
        }
    }

    public static class RethrowFaultProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            throw new Fault(event.getMessage().getExceptionPayload().getException().getCause());
        }
    }

    public static class RethrowExceptionStrategy extends TemplateMessagingExceptionStrategy
    {
        @Override
        protected void nullifyExceptionPayloadIfRequired(MuleEvent event)
        {
            // does nothing
        }

        @Override
        protected MuleEvent afterRouting(Exception exception, MuleEvent event)
        {
            return event;
        }

        @Override
        protected MuleEvent beforeRouting(Exception exception, MuleEvent event)
        {
            return event;
        }
    }

    public static class CxfTransformerThrowsExceptions extends AbstractTransformer
    {
        @Override
        protected Object doTransform(Object src, String enc) throws TransformerException
        {
            throw new TransformerException(CoreMessages.failedToBuildMessage());
        }

    }

}
