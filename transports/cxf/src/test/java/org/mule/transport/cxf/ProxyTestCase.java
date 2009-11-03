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

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.transport.cxf.testmodels.AsyncService;
import org.mule.transport.http.HttpConnector;
import org.mule.util.concurrent.Latch;

import java.util.HashMap;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class ProxyTestCase extends FunctionalTestCase
{
    String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body><test xmlns=\"http://foo\"> foo </test>" + "</soap:Body>" + "</soap:Envelope>";

    String doGoogleSearch = "<urn:doGoogleSearch xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:urn=\"urn:GoogleSearch\">";

    String msgWithComment = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<!-- comment 1 -->"
        + "<soap:Header>"
        + "<!-- comment 2 -->"
        + "</soap:Header>"
        + "<!-- comment 3 -->"
        + "<soap:Body>"
        + "<!-- comment 4 -->"
        + doGoogleSearch
        + "<!-- this comment breaks it -->"
        + "<key>1</key>"
        + "<!-- comment 5 -->"
        + "<q>a</q>"
        + "<start>0</start>"
        + "<maxResults>1</maxResults>"
        + "<filter>false</filter>"
        + "<restrict>a</restrict>"
        + "<safeSearch>true</safeSearch>"
        + "<lr>a</lr>"
        + "<ie>b</ie>"
        + "<oe>c</oe>"
        + "</urn:doGoogleSearch>"
        + "<!-- comment 6 -->"
        + "</soap:Body>"
        + "<!-- comment 7 -->"
        + "</soap:Envelope>";

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
        System.out.println(resString);
        assertTrue(resString.indexOf("<test xmlns=\"http://foo\"") != -1);
    }

    public void testServerClientProxyWithWsdl() throws Exception
    {
        final Latch latch = new Latch();
        ((FunctionalTestComponent) getComponent("serverClientProxyWithWsdl")).setEventCallback(new EventCallback()
        {

            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                latch.countDown();
            }
        });

        String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                     + "<soap:Body> <test xmlns=\"http://foo\"></test>" + "</soap:Body>" + "</soap:Envelope>";

        MuleClient client = new MuleClient();
        MuleMessage result = client.send("http://localhost:63081/services/proxyWithWsdl", msg, null);
        String resString = result.getPayloadAsString();
        assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
        assertTrue(resString.indexOf("<test xmlns=\"http://foo\"") != -1);
    }

    public void testServerClientProxyWithWsdl2() throws Exception
    {
        final Latch latch = new Latch();
        ((FunctionalTestComponent) getComponent("serverClientProxyWithWsdl2")).setEventCallback(new EventCallback()
        {

            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                latch.countDown();
            }
        });

        String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                     + "<soap:Body> <test xmlns=\"http://foo\"></test>" + "</soap:Body>" + "</soap:Envelope>";

        MuleClient client = new MuleClient();
        MuleMessage result = client.send("http://localhost:63081/services/proxyWithWsdl2", msg, null);
        String resString = result.getPayloadAsString();
        assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
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
        MuleMessage result = client.send("http://localhost:63081/services/greeter-databinding-proxy", msg, null);
        String resString = result.getPayloadAsString();
        assertTrue(resString.indexOf("greetMeResponse") != -1);
    }

    public void testProxyWithFault() throws Exception 
    {
        String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body><invalid xmlns=\"http://apache.org/hello_world_soap_http/types\"><requestType>Dan</requestType></invalid>" +
                    "</soap:Body>" + "</soap:Envelope>";

        MuleClient client = new MuleClient();
        MuleMessage result = client.send("http://localhost:63081/services/greeter-proxy", msg, null);
        String resString = result.getPayloadAsString();
        
        assertTrue(resString.indexOf("invalid was not recognized") != -1);
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

    public void testOneWaySend() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("http://localhost:63081/services/oneway",
            prepareOneWayTestMessage(), prepareOneWayTestProperties());
        assertEquals("", result.getPayloadAsString());

        AsyncService component = (AsyncService) getComponent("asyncService");
        assertTrue(component.getLatch().await(10000, TimeUnit.MILLISECONDS));
    }

    public void testOneWayDispatch() throws Exception
    {
        new MuleClient().dispatch("http://localhost:63081/services/oneway", prepareOneWayTestMessage(),
            prepareOneWayTestProperties());

        AsyncService component = (AsyncService) getComponent("asyncService");
        assertTrue(component.getLatch().await(10000, TimeUnit.MILLISECONDS));
    }

    /**
     * MULE-4549 ReversibleXMLStreamReader chokes on comments with ClassCastException
     * @throws Exception
     */
    public void testProxyWithCommentInRequest() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("http://localhost:63081/services/envelope-proxy", msgWithComment, null);
        String resString = result.getPayloadAsString();
        assertTrue(resString.indexOf("<test xmlns=\"http://foo\"> foo </test>") != -1);
    }
    
    protected String prepareOneWayTestMessage()
    {
        return "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" + "<soap:Body>"
               + "<send xmlns=\"http://testmodels.cxf.transport.mule.org\"><text>hello</text></send>"
               + "</soap:Body>" + "</soap:Envelope>";
    }

    protected Map prepareOneWayTestProperties()
    {
        Map<String, Object> httpHeaders = new HashMap<String, Object>();

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(HttpConnector.HTTP_CUSTOM_HEADERS_MAP_PROPERTY, httpHeaders);
        props.put("SOAPAction", "http://acme.com/oneway");
        return props;
    }   
    
    protected String getConfigResources()
    {
        return "proxy-conf.xml";
    }

}
