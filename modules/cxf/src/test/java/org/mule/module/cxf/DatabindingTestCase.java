/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class DatabindingTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "databinding-conf.xml";
    }

    @Test
    public void testEchoWsdl() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.request(((InboundEndpoint) muleContext.getRegistry()
            .lookupObject("httpInbound")).getAddress()
                                            + "?wsdl", 5000);
        assertNotNull(result.getPayload());
    }

    @Test
    public void testEchoWsdlAegisBinding() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.request(((InboundEndpoint) muleContext.getRegistry()
            .lookupObject("httpInboundAegis")).getAddress()
                                            + "?wsdl", 5000);
        assertNotNull(result.getPayload());
    }

    @Test
    public void testEchoWsdlSourceBinding() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.request(((InboundEndpoint) muleContext.getRegistry()
            .lookupObject("httpInboundSource")).getAddress()
                                            + "?wsdl", 5000);
        assertNotNull(result.getPayload());
    }

    @Test
    public void testEchoWsdlJaxbBinding() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.request(((InboundEndpoint) muleContext.getRegistry()
            .lookupObject("httpInboundJaxb")).getAddress()
                                            + "?wsdl", 5000);
        assertNotNull(result.getPayload());
    }

    @Test
    public void testEchoWsdlJibxBinding() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.request(((InboundEndpoint) muleContext.getRegistry()
            .lookupObject("httpInboundJibx")).getAddress()
                                            + "?wsdl", 5000);
        assertNotNull(result.getPayload());
    }
    @Test
    public void testEchoWsdlStaxBinding() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.request(((InboundEndpoint) muleContext.getRegistry()
            .lookupObject("httpInboundStax")).getAddress()
                                            + "?wsdl", 5000);
        assertNotNull(result.getPayload());
    }

    @Test
    public void testEchoWsdlCustomBinding() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.request(((InboundEndpoint) muleContext.getRegistry()
            .lookupObject("httpInboundCustom")).getAddress()
                                            + "?wsdl", 5000);
        assertNotNull(result.getPayload());
    }
}
