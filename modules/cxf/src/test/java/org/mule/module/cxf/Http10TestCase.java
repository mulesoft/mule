/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class Http10TestCase extends AbstractServiceAndFlowTestCase
{

    @ClassRule
    public static DynamicPort dynamicPort = new DynamicPort("port1");

    public Http10TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        setDisposeContextPerClass(true);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "http-10-conf-service.xml"},
            {ConfigVariant.FLOW, "http-10-conf-flow.xml"}
        });
    }      

    @Test
    public void testHttp10TransformerNotOnProtocol() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String,String> props = new HashMap<String, String>();
        
        MuleMessage result = client.send("cxfOutbound", "Dan", props);
        assertEquals("Hello Dan", result.getPayload());
        
        result = client.request("vm://out", 1000);
        assertFalse("chunked".equals(result.getOutboundProperty(HttpConstants.HEADER_TRANSFER_ENCODING)));
    }

    @Test
    public void testHttp10TransformerOnProtocol() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String,String> props = new HashMap<String, String>();
        
        MuleMessage result = client.send("cxfOutbound2", "Dan", props);
        assertEquals("Hello Dan", result.getPayload());
        
        result = client.request("vm://out", 1000);
        assertFalse("chunked".equals(result.getOutboundProperty(HttpConstants.HEADER_TRANSFER_ENCODING)));
    }
}
