/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WSProxyTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Rule
    public DynamicPort dynamicPort3 = new DynamicPort("port3");
    
    @Override
    protected String getConfigResources()
    {
        return "mule-proxy-config.xml";
    }

    @Test
    public void testDirectRequest() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("wsdl-cxf:http://localhost:" + dynamicPort1.getNumber() + "/WebService?wsdl&method=echo",
            new DefaultMuleMessage("mule", muleContext));
        assertEquals ("mule", result.getPayloadAsString());
    }

    @Test
    public void testWsdlProxyRequest() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, String> props = new HashMap<String, String>();
        props.put("http.method", "GET");
        MuleMessage replyMessage = client.send("http://localhost:" + dynamicPort2.getNumber() + "/webServiceProxy?wsdl",
            "/services/webServiceProxy?WSDL", props);
        assertNotNull(replyMessage);
        
        String wsdl = replyMessage.getPayloadAsString();
        assertNotNull(wsdl);
        System.out.println(wsdl);
        assertTrue(wsdl.indexOf("<wsdl:definitions") != -1);
        assertTrue(wsdl.indexOf("<wsdl:message name=\"echoResponse\">") != -1);
        assertTrue(wsdl.indexOf("<wsdl:message name=\"echo\">") != -1);
    }
    
    @Test
    public void testProxyRequest() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("wsdl-cxf:http://localhost:" + dynamicPort2.getNumber() + "/webServiceProxy?wsdl&method=echo",
            new DefaultMuleMessage("mule", muleContext));
        assertEquals ("mule", result.getPayloadAsString());
    }
    
    @Test
    public void testWsdlFileRequest() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, String> props = new HashMap<String, String>();
        props.put("http.method", "GET");
        MuleMessage replyMessage = client.send("http://localhost:" + dynamicPort3.getNumber() + "/webServiceProxy?wsdl",
            "/services/webServiceProxy?WSDL", props);
        assertNotNull(replyMessage);
        
        String wsdl = replyMessage.getPayloadAsString();
        assertNotNull(wsdl);
        assertTrue(wsdl.indexOf("<wsdl:definitions") != -1);
        assertTrue(wsdl.indexOf("<wsdl:message name=\"echoResponse\">") != -1);
        assertTrue(wsdl.indexOf("<wsdl:message name=\"echo\">") != -1);
    }
    
    @Test
    public void testWsdlFileProxyRequest() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("wsdl-cxf:http://localhost:" + dynamicPort3.getNumber() + "/webServiceProxy?wsdl&method=echo",
            new DefaultMuleMessage("mule", muleContext));
        assertEquals ("mule", result.getPayloadAsString());
    }
    
}
