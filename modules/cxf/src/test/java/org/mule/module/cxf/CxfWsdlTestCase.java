/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertNotNull;
import org.mule.DefaultMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.endpoint.EndpointURIEndpointBuilder;
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
        MuleClient client = muleContext.getClient();

        MuleMessage message = getTestMuleMessage("test1");
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

        MuleMessage message = getTestMuleMessage("test1");
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
