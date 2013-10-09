/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertNotNull;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Test;
import org.w3c.dom.Document;

public class CxfWsdlTestCase extends AbstractMuleContextTestCase
{
    public static final String TEST_URL = "wsdl-cxf:http://localhost:8080/mule-tests-external-cxf/services/TestService?WSDL&method=getTest";
    public static final String TEST_URL_NOWSDL = "wsdl-cxf:http://localhost:8080/mule-tests-external-cxf/services/TestService?method=getTest";
    public static final String TEST_URL_WSDL = "http://localhost:8080/mule-tests-external-cxf/services/TestService?wsdl";

    @Test
    public void testCxfWsdlService() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage message = new DefaultMuleMessage("test1", muleContext);
        MuleMessage reply = client.send(TEST_URL, message);
        assertNotNull(reply);

        Document response = (Document) reply.getPayload();
        assertNotNull(response);

        XMLAssert.assertXpathEvaluatesTo("test1",
            "//*[namespace-uri()='http://applications.external.tck.mule.org' and local-name()='key']",
            response);
    }

    /**
     * This tests the endpoint propery of wsdlUrl which specifies an alternative WSDL
     * location (see MULE-1368)
     */
    @Test
    public void testCxfWsdlServiceWithEndpointParam() throws Exception
    {
        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(TEST_URL_NOWSDL, muleContext);
        endpointBuilder.setProperty("wsdlUrl", TEST_URL_WSDL);

        OutboundEndpoint endpoint = 
            muleContext.getEndpointFactory().getOutboundEndpoint(endpointBuilder);

        MuleMessage message = new DefaultMuleMessage("test1", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, endpoint.getExchangePattern(),(FlowConstruct) null);
        MuleMessage reply = endpoint.process(event).getMessage();

        assertNotNull(reply);

        Document response = (Document) reply.getPayload();
        assertNotNull(response);

        XMLAssert.assertXpathEvaluatesTo("test1",
            "//*[namespace-uri()='http://applications.external.tck.mule.org' and local-name()='key']",
            response);
    }
}
