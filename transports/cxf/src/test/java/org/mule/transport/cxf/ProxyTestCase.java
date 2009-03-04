/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConnector;

import java.util.HashMap;
import java.util.Map;

public class ProxyTestCase extends FunctionalTestCase
{
    String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body><test xmlns=\"http://foo\"> foo </test>" + "</soap:Body>" + "</soap:Envelope>";

    
    public void testServerWithEcho() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("http://localhost:63081/services/Echo", msg, null);
        String resString = result.getPayloadAsString();
//        System.out.println(resString);
        assertTrue(resString.indexOf("<test xmlns=\"http://foo\"> foo </test>") != -1);
    }
    
    public void testServerClientProxy() throws Exception
    {
        String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                     + "<soap:Body> <test xmlns=\"http://foo\"></test>" + "</soap:Body>" + "</soap:Envelope>";

        MuleClient client = new MuleClient();
        MuleMessage result = client.send("http://localhost:63081/services/proxy", msg, null);
        String resString = result.getPayloadAsString();
        assertTrue(resString.indexOf("<test xmlns=\"http://foo\"") != -1);
    }

    public void testServerClientProxyWithWsdl() throws Exception
    {
        String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                     + "<soap:Body> <test xmlns=\"http://foo\"></test>" + "</soap:Body>" + "</soap:Envelope>";

        MuleClient client = new MuleClient();
        MuleMessage result = client.send("http://localhost:63081/services/proxyWithWsdl", msg, null);
        String resString = result.getPayloadAsString();
        assertTrue(resString.indexOf("<test xmlns=\"http://foo\"") != -1);
    }
    
    public void testServerClientProxyWithTransform() throws Exception
    {
        String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                     + "<soap:Body> <test xmlns=\"http://foo\"></test>" + "</soap:Body>" + "</soap:Envelope>";

        MuleClient client = new MuleClient();
        MuleMessage result = client.send("http://localhost:63081/services/proxyWithTransform", msg, null);
        String resString = result.getPayloadAsString();
        System.out.println(resString);
        assertTrue(resString.indexOf("<transformed xmlns=\"http://foo\">") != -1);
    }

    public void testProxyWithDatabinding() throws Exception 
    {
        String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body><greetMe xmlns=\"http://apache.org/hello_world_soap_http/types\"><requestType>Dan</requestType></greetMe>" +
                    "</soap:Body>" + "</soap:Envelope>";

        MuleClient client = new MuleClient();
        MuleMessage result = client.send("http://localhost:63081/services/greeterProxy", msg, null);
        String resString = result.getPayloadAsString();
        assertTrue(resString.indexOf("greetMeResponse") != -1);
    }

    public void testProxyWithIntermediateTransform() throws Exception 
    {
        String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body><greetMe xmlns=\"http://apache.org/hello_world_soap_http/types\"><requestType>Dan</requestType></greetMe>" +
                    "</soap:Body>" + "</soap:Envelope>";

        MuleClient client = new MuleClient();
        MuleMessage result = client.send("http://localhost:63081/services/transform-proxy", msg, null);
        String resString = result.getPayloadAsString();
        assertTrue(resString.indexOf("greetMeResponse") != -1);
    }

    public void testSoapActionRouting() throws Exception 
    {
        String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body> <test xmlns=\"http://foo\"></test>" + "</soap:Body>" + "</soap:Envelope>";

        Map<String, Object> httpHeaders = new HashMap<String, Object>();
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(HttpConnector.HTTP_CUSTOM_HEADERS_MAP_PROPERTY, httpHeaders);
        props.put("SOAPAction", "http://acme.com/transform");
              
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("http://localhost:63081/services/routeBasedOnSoapAction", msg, props);
        String resString = result.getPayloadAsString();
        System.out.println(resString);
        assertTrue(resString.indexOf("<transformed xmlns=\"http://foo\">") != -1);
    }
    
    public void testOneWay() throws Exception 
    {
        String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" + 
            "<soap:Body>" +
            "<send xmlns=\"http://testmodels.cxf.transport.mule.org\"><text>hello</text></send>" + 
            "</soap:Body>" + 
            "</soap:Envelope>";

        Map<String, Object> httpHeaders = new HashMap<String, Object>();
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(HttpConnector.HTTP_CUSTOM_HEADERS_MAP_PROPERTY, httpHeaders);
        props.put("SOAPAction", "http://acme.com/oneway");
              
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("http://localhost:63081/services/routeBasedOnSoapAction", msg, props);
        assertEquals("", result.getPayloadAsString());
    }
    
    protected String getConfigResources()
    {
        return "proxy-conf.xml";
    }

}
