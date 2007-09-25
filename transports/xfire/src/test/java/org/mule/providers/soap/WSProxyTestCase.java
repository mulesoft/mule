/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.soap;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Map;


public class WSProxyTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "mule-proxy-config.xml";
    }
    
    public void testDirectRequest() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage result = client.send("wsdl-xfire:http://localhost:6065/WebService?wsdl&method=echo", new MuleMessage("mule"));
        assertEquals ("mule", result.getPayloadAsString());
    }
    
    public void testWsdlProxyRequest() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        props.put("http.method", "GET");
        UMOMessage replyMessage = client.send("http://localhost:6070/webServiceProxy?wsdl", "/services/webServiceProxy?WSDL", props);
        assertNotNull(replyMessage);
        
        String wsdl = replyMessage.getPayloadAsString();
        assertNotNull(wsdl);
        assertTrue(wsdl.indexOf("<wsdl:definitions") != -1);
        assertTrue(wsdl.indexOf("<wsdl:message name=\"echoResponse\">") != -1);
        assertTrue(wsdl.indexOf("<wsdl:message name=\"echoRequest\">") != -1);
    }
    
    public void testProxyRequest() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage result = client.send("wsdl-xfire:http://localhost:6070/webServiceProxy?wsdl&method=echo", new MuleMessage("mule"));
        assertEquals ("mule", result.getPayloadAsString());
    }
    
    public void testWsdlFileRequest() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        props.put("http.method", "GET");
        UMOMessage replyMessage = client.send("http://localhost:6075/webServiceProxy?wsdl", "/services/webServiceProxy?WSDL", props);
        assertNotNull(replyMessage);
        
        String wsdl = replyMessage.getPayloadAsString();
        assertNotNull(wsdl);
        assertTrue(wsdl.indexOf("<wsdl:definitions") != -1);
        assertTrue(wsdl.indexOf("<wsdl:message name=\"echoResponse\">") != -1);
        assertTrue(wsdl.indexOf("<wsdl:message name=\"echoRequest\">") != -1);
    }
    
    public void testWsdlFileProxyRequest() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage result = client.send("wsdl-xfire:http://localhost:6075/webServiceProxy?wsdl&method=echo", new MuleMessage("mule"));
        assertEquals ("mule", result.getPayloadAsString());
    }

}





