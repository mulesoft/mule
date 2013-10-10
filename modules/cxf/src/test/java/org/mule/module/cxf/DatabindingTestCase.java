/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class DatabindingTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "databinding-conf.xml";
    }

    @Test
    public void testEchoWsdl() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.request(((InboundEndpoint) client.getMuleContext()
            .getRegistry()
            .lookupObject("httpInbound")).getAddress()
                                            + "?wsdl", 5000);
        assertNotNull(result.getPayload());
    }

    @Test
    public void testEchoWsdlAegisBinding() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.request(((InboundEndpoint) client.getMuleContext()
            .getRegistry()
            .lookupObject("httpInboundAegis")).getAddress()
                                            + "?wsdl", 5000);
        assertNotNull(result.getPayload());
    }

    @Test
    public void testEchoWsdlSourceBinding() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.request(((InboundEndpoint) client.getMuleContext()
            .getRegistry()
            .lookupObject("httpInboundSource")).getAddress()
                                            + "?wsdl", 5000);
        assertNotNull(result.getPayload());
    }

    @Test
    public void testEchoWsdlJaxbBinding() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.request(((InboundEndpoint) client.getMuleContext()
            .getRegistry()
            .lookupObject("httpInboundJaxb")).getAddress()
                                            + "?wsdl", 5000);
        assertNotNull(result.getPayload());
    }

    @Test
    public void testEchoWsdlJibxBinding() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.request(((InboundEndpoint) client.getMuleContext()
            .getRegistry()
            .lookupObject("httpInboundJibx")).getAddress()
                                            + "?wsdl", 5000);
        assertNotNull(result.getPayload());
    }
    @Test
    public void testEchoWsdlStaxBinding() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.request(((InboundEndpoint) client.getMuleContext()
            .getRegistry()
            .lookupObject("httpInboundStax")).getAddress()
                                            + "?wsdl", 5000);
        assertNotNull(result.getPayload());
    }

    @Test
    public void testEchoWsdlCustomBinding() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.request(((InboundEndpoint) client.getMuleContext()
            .getRegistry()
            .lookupObject("httpInboundCustom")).getAddress()
                                            + "?wsdl", 5000);
        assertNotNull(result.getPayload());
    }

}
