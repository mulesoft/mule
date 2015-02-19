/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
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

public class CxfErrorBehaviorTestCase extends AbstractServiceAndFlowTestCase
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

    private static final String requestPayload =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "           xmlns:hi=\"http://example.cxf.module.mule.org/\">\n" +
            "<soap:Body>\n" +
            "<hi:sayHi>\n" +
            "    <arg0>hi</arg0>\n" +
            "</hi:sayHi>\n" +
            "</soap:Body>\n" +
            "</soap:Envelope>";


    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(org.mule.module.http.api.HttpConstants.Methods.POST.name()).disableStatusCodeValidation().build();

    public CxfErrorBehaviorTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.SERVICE, "cxf-error-behavior-service.xml"},
                {ConfigVariant.FLOW, "cxf-error-behavior-flow.xml"},
                {ConfigVariant.FLOW, "cxf-error-behavior-flow-httpn.xml"}
        });
    }

    @Test
    public void testFaultInCxfService() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(requestFaultPayload, (Map<String,Object>)null, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/testServiceWithFault", request, HTTP_REQUEST_OPTIONS);
        assertNotNull(response);
        assertTrue(response.getPayloadAsString().contains("<faultstring>"));
        assertEquals(String.valueOf(HttpConstants.SC_INTERNAL_SERVER_ERROR), response.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY).toString());
    }

    @Test
    public void testFaultInCxfSimpleService() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(requestPayload, (Map<String,Object>)null, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/testSimpleServiceWithFault", request, HTTP_REQUEST_OPTIONS);
        assertNotNull(response);
        assertTrue(response.getPayloadAsString().contains("<faultstring>"));
        assertEquals(String.valueOf(HttpConstants.SC_INTERNAL_SERVER_ERROR), response.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY).toString());
    }

    @Test
    public void testExceptionThrownInTransformer() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(requestPayload, (Map<String,Object>)null, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/testTransformerException", request, HTTP_REQUEST_OPTIONS);
        assertNotNull(response);
        assertTrue(response.getPayloadAsString().contains("<faultstring>"));
        assertEquals(String.valueOf(HttpConstants.SC_INTERNAL_SERVER_ERROR), response.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY).toString());
    }

    @Test
    public void testUnwrapException() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(requestPayload, (Map<String,Object>)null, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/testUnwrapException", request, HTTP_REQUEST_OPTIONS);
        assertNotNull(response);
        assertTrue(response.getPayloadAsString().contains("Illegal argument!!"));
        assertEquals(String.valueOf(HttpConstants.SC_INTERNAL_SERVER_ERROR), response.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY).toString());
    }

    @Test
    public void testClientWithSOAPFault() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("hello", (Map<String,Object>)null, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://testClientSOAPFault", request);
        assertNotNull(response);
        assertNotNull(response.getExceptionPayload());
        assertTrue(response.getExceptionPayload().getException().getCause() instanceof Fault);
        assertNull(response.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
    }

    @Test
    public void testClientWithTransformerException() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("hello", (Map<String,Object>)null, muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://testClientTransformerException", request);
        assertNotNull(response);
        assertNotNull(response.getExceptionPayload());
        assertTrue(response.getExceptionPayload().getException() instanceof MessagingException);
    }

    @Test
    public void testServerClientProxyWithFault() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/testProxyWithFault", getTestMuleMessage(requestFaultPayload), HTTP_REQUEST_OPTIONS);
        String resString = result.getPayloadAsString();
        assertThat(resString, containsString("<faultstring>Cxf Exception Message</faultstring>"));
        assertEquals(String.valueOf(HttpConstants.SC_INTERNAL_SERVER_ERROR), result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY).toString());
    }

    @Test
    public void testServerClientProxyWithTransformerException() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/testProxyWithTransformerException", getTestMuleMessage(requestPayload), HTTP_REQUEST_OPTIONS);
        String resString = result.getPayloadAsString();
        assertTrue(resString.contains("TransformerException"));
        assertEquals(String.valueOf(HttpConstants.SC_INTERNAL_SERVER_ERROR), result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY).toString());
    }
    @Test
    public void testServerClientJaxwsWithUnwrapFault() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/testUnwrapProxyFault", getTestMuleMessage(requestPayload), HTTP_REQUEST_OPTIONS);
        String resString = result.getPayloadAsString();
        assertThat(resString, containsString("Illegal argument!!"));
        assertEquals(String.valueOf(HttpConstants.SC_INTERNAL_SERVER_ERROR), result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY).toString());
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
