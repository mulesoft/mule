/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.util.Map;

import org.apache.cxf.interceptor.Fault;
import org.junit.Rule;
import org.junit.Test;

public class CxfErrorBehaviorTestCase extends FunctionalTestCase
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

    @Override
    protected String getConfigResources()
    {
        return "cxf-error-behavior-conf.xml";
    }

    @Test
    public void testFaultInCxfService() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(requestFaultPayload, (Map<String,Object>)null, muleContext);
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/testServiceWithFault", request);
        assertNotNull(response);
        assertTrue(response.getPayloadAsString().contains("<faultstring>"));
        assertEquals(String.valueOf(HttpConstants.SC_INTERNAL_SERVER_ERROR), response.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
    }

    @Test
    public void testUnwrapException() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(requestPayload, (Map<String,Object>)null, muleContext);
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/testUnwrapException", request);
        assertNotNull(response);
        assertTrue(response.getPayloadAsString().contains("Illegal argument!!"));
        assertEquals(String.valueOf(HttpConstants.SC_INTERNAL_SERVER_ERROR), response.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
    }

    @Test
    public void testClientWithSOAPFault() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("hello", (Map<String,Object>)null, muleContext);
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("vm://testClientSOAPFault", request);
        assertNotNull(response);
        assertNotNull(response.getExceptionPayload());
        assertTrue(response.getExceptionPayload().getException().getCause() instanceof Fault);
        assertNull(response.getInboundProperty("http.status"));

    }

    @Test
    public void testServerClientProxyWithFault() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/testProxyWithFault", requestFaultPayload, null);
        String resString = result.getPayloadAsString();
        assertTrue(resString.contains("<faultstring>Cxf Exception Message</faultstring>"));
        assertEquals(String.valueOf(HttpConstants.SC_INTERNAL_SERVER_ERROR), result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
    }

    @Test
    public void testServerClientJaxwsWithUnwrapFault() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/testUnwrapProxyFault", requestPayload, null);
        String resString = result.getPayloadAsString();
        assertTrue(resString.contains("Illegal argument!!"));
        assertEquals(String.valueOf(HttpConstants.SC_INTERNAL_SERVER_ERROR), result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
    }

}
