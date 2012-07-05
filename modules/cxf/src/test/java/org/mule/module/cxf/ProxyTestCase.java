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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.module.cxf.testmodels.AsyncService;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;
import org.mule.util.concurrent.Latch;

public class ProxyTestCase extends AbstractServiceAndFlowTestCase
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

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public ProxyTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "proxy-conf-service.xml"},
            {ConfigVariant.FLOW, "proxy-conf-flow.xml"}
        });
    }

    @Test
    public void testServerWithEcho() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/Echo", msg, null);
        String resString = result.getPayloadAsString();
//        System.out.println(resString);
        assertTrue(resString.indexOf("<test xmlns=\"http://foo\"> foo </test>") != -1);
    }

   @Test
    public void testServerClientProxy() throws Exception
    {
        String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                     + "<soap:Body> <foo xmlns=\"http://foo\"></foo>" + "</soap:Body>" + "</soap:Envelope>";

        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/proxy", msg, null);
        String resString = result.getPayloadAsString();

        assertTrue(resString.indexOf("<foo xmlns=\"http://foo\"") != -1);
    }


    @Test
    public void testProxyBodyValidation() throws Exception
    {
        doTestProxyValidation("http://localhost:" + dynamicPort.getNumber() + "/services/proxyBodyWithValidation");
    }

   @Test
   public void testProxyBodyValidationWithExternalSchema() throws Exception
   {
        doTestProxyValidation("http://localhost:" + dynamicPort.getNumber() + "/services/proxyBodyWithValidationAndSchemas");
   }

   @Test
   public void testProxyEnvelopeValidation() throws Exception
   {
        doTestProxyValidation("http://localhost:" + dynamicPort.getNumber() + "/services/proxyEnvelopeWithValidation");
   }

    public void doTestProxyValidation(String url) throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send(url, msg, null);
        String resString = result.getPayloadAsString();
        assertTrue(resString.indexOf("Schema validation error on message") != -1);

        String valid =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body> " +
                    "<echo xmlns=\"http://www.muleumo.org\">" +
                    "  <echo>test</echo>" +
                    "</echo>"
            + "</soap:Body>"
            + "</soap:Envelope>";
        result = client.send(url, valid, null);
        resString = result.getPayloadAsString();
        assertTrue(resString.contains("<echoResponse xmlns=\"http://www.muleumo.org\">"));
    }

   @Test
   public void testServerClientProxyWithWsdl() throws Exception
   {
        final Latch latch = new Latch();
        ((FunctionalTestComponent) getComponent("serverClientProxyWithWsdl")).setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                latch.countDown();
            }
        });

        String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                     + "<soap:Body> <test xmlns=\"http://foo\"></test>" + "</soap:Body>" + "</soap:Envelope>";

        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/proxyWithWsdl", msg, null);
        String resString = result.getPayloadAsString();
        assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
        assertTrue(resString.indexOf("<test xmlns=\"http://foo\"") != -1);
   }

   @Test
   public void testServerClientProxyWithWsdl2() throws Exception
   {
        final Latch latch = new Latch();
        ((FunctionalTestComponent) getComponent("serverClientProxyWithWsdl2")).setEventCallback(new EventCallback()
        {

            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                latch.countDown();
            }
        });

        String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                     + "<soap:Body> <test xmlns=\"http://foo\"></test>" + "</soap:Body>" + "</soap:Envelope>";

        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/proxyWithWsdl2", msg, null);
        String resString = result.getPayloadAsString();
        assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
        assertTrue(resString.indexOf("<test xmlns=\"http://foo\"") != -1);
   }

   @Test
   public void testServerClientProxyWithTransform() throws Exception
   {
        String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                     + "<soap:Body> <test xmlns=\"http://foo\"></test>" + "</soap:Body>" + "</soap:Envelope>";

        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/proxyWithTransform", msg, null);
        String resString = result.getPayloadAsString();
        System.out.println(resString);
        assertTrue(resString.indexOf("<transformed xmlns=\"http://foo\">") != -1);
   }

   @Test
   public void testProxyWithDatabinding() throws Exception
   {
        String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body><greetMe xmlns=\"http://apache.org/hello_world_soap_http/types\"><requestType>Dan</requestType></greetMe>" +
                    "</soap:Body>" + "</soap:Envelope>";

        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/greeter-databinding-proxy", msg, null);
        String resString = result.getPayloadAsString();
        assertTrue(resString.indexOf("greetMeResponse") != -1);
   }

   @Test
   public void testProxyWithFault() throws Exception
   {
        String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body><invalid xmlns=\"http://apache.org/hello_world_soap_http/types\"><requestType>Dan</requestType></invalid>" +
                    "</soap:Body>" + "</soap:Envelope>";

        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/greeter-proxy", msg, null);
        String resString = result.getPayloadAsString();

        assertFalse("Status code should not be 'OK' when the proxied endpoint returns a fault",
                    String.valueOf(HttpConstants.SC_OK).equals(result.getOutboundProperty("http.status")));

        assertTrue(resString.indexOf("Fault") != -1);
   }

   @Test
   public void testProxyWithIntermediateTransform() throws Exception
   {
        String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body><greetMe xmlns=\"http://apache.org/hello_world_soap_http/types\"><requestType>Dan</requestType></greetMe>" +
                    "</soap:Body>" + "</soap:Envelope>";

        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/transform-proxy", msg, null);
        String resString = result.getPayloadAsString();
        assertTrue(resString.indexOf("greetMeResponse") != -1);
   }

   @Test
   public void testSoapActionRouting() throws Exception
   {
        String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body> <test xmlns=\"http://foo\"></test>" + "</soap:Body>" + "</soap:Envelope>";

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("SOAPAction", "http://acme.com/transform");

        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/routeBasedOnSoapAction", msg, props);
        String resString = result.getPayloadAsString();
        System.out.println(resString);
        assertTrue(resString.indexOf("<transformed xmlns=\"http://foo\">") != -1);
   }

   @Test
   public void testOneWaySend() throws Exception
   {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/oneway",
            prepareOneWayTestMessage(), prepareOneWayTestProperties());
        assertEquals("", result.getPayloadAsString());

        AsyncService component = (AsyncService) getComponent("asyncService");
        assertTrue(component.getLatch().await(10000, TimeUnit.MILLISECONDS));
   }

   @Test
   public void testOneWayDispatch() throws Exception
   {
        new MuleClient(muleContext).dispatch("http://localhost:" + dynamicPort.getNumber() + "/services/oneway", prepareOneWayTestMessage(),
            prepareOneWayTestProperties());

        AsyncService component = (AsyncService) getComponent("asyncService");
        assertTrue(component.getLatch().await(10000, TimeUnit.MILLISECONDS));
   }

   /**
     * MULE-4549 ReversibleXMLStreamReader chokes on comments with ClassCastException
     * @throws Exception
     */
   @Test
   public void testProxyWithCommentInRequest() throws Exception
   {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/envelope-proxy", msgWithComment, null);
        String resString = result.getPayloadAsString();
        assertTrue(resString.contains(doGoogleSearch));
   }

   /**
     * MULE-6188: ReversibleXMLStreamReader throw NPE after reset because current event is null.
     * @throws Exception
     */
   @Test
   public void testProxyEnvelopeWithXsltTransformation() throws Exception
   {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/envelope-xslt-proxy", msg, null);
        assertTrue(result.getPayloadAsString().contains(msg));
   }


   @Test
   public void testProxyCDATA() throws Exception
   {
        String msg="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:sup=\"http://support.cxf.module.mule.org/\">\n" +
                   "<soapenv:Header/>\n" +
                   "<soapenv:Body>\n" +
                   "<sup:invoke>\n" +
                   "<soapenv:Envelope>\n" +
                   "<soapenv:Header/>\n" +
                   "<soapenv:Body>\n" +
                   "<sup:invoke>\n" +
                   "<Request>\n" +
                   "<servicePayload><![CDATA[<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Header/><soapenv:Body><int:test/></soapenv:Body></soapenv:Envelope>]]></servicePayload>\n" +
                   "</Request>\n" +
                   "</sup:invoke>\n" +
                   "</soapenv:Body>\n" +
                   "</soapenv:Envelope>\n" +
                   "</sup:invoke>\n" +
                   "</soapenv:Body>\n" +
                   "</soapenv:Envelope>";

        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/CDATAService", msg, null);
        assertNotNull(result);
        assertTrue(result.getPayloadAsString().contains("![CDATA["));
   }

   /** MULE-6159: Proxy service fails when WSDL has faults **/
   @Test
   public void testProxyWithSoapFault() throws Exception
   {
        MuleClient client = new MuleClient(muleContext);

        String proxyFaultMsg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body><greetMe xmlns=\"http://apache.org/hello_world_fault/types\"><requestType>Dan</requestType></greetMe>" +
                    "</soap:Body>" + "</soap:Envelope>";

        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/proxyFault", proxyFaultMsg, null);
        String resString = result.getPayloadAsString();
        assertTrue(resString.contains("ERROR"));
   }

    protected String prepareOneWayTestMessage()
    {
        return "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" + "<soap:Body>"
               + "<ns:send xmlns:ns=\"http://testmodels.cxf.module.mule.org/\"><text>hello</text></ns:send>"
               + "</soap:Body>" + "</soap:Envelope>";
    }
    
    protected Map prepareOneWayTestProperties()
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("SOAPAction", "http://acme.com/oneway");
        return props;
    }

}
