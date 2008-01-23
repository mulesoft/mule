/*
 * $Id:XFireWsdlTestCase.java 7586 2007-07-19 04:06:50Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.xfire;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.DefaultMuleSession;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.Endpoint;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.extras.client.MuleClient;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transport.AbstractConnector;

import org.custommonkey.xmlunit.XMLAssert;

/**
 * This test requires an external Xfire instance to call out to 
 */
public class XFireWsdlTestCase extends AbstractMuleTestCase
{
    public static final String TEST_URL = "wsdl-xfire:http://localhost:63080/mule-tests-external-xfire/services/TestService?WSDL&method=getTest";
    public static final String TEST_URL_NOWSDL = "wsdl-xfire:http://localhost:63080/mule-tests-external-xfire/services/TestService?method=getTest";
    public static final String TEST_URL_WSDL = "http://localhost:63080/mule-tests-external-xfire/services/TestService?wsdl";

    public void testXFireWsdlService() throws Exception
    {
        MuleClient client = new MuleClient();

        MuleMessage message = new DefaultMuleMessage("test1");
        MuleMessage reply = client.send(TEST_URL, message);
        assertNotNull(reply);

        String response = reply.getPayloadAsString();
        assertNotNull(response);

        XMLAssert.assertXpathEvaluatesTo("test1", "//*[namespace-uri()='http://applications.external.tck.mule.org' and local-name()='key']", response);
    }

    /**
     * This tests the endpoint propery of wsdlUrl which specifies an alternative WSDL
     * location (see MULE-1368)
     */
    public void testXFireWsdlServiceWithEndpointParam() throws Exception
    {
        // make sure the Mule is up when not using MuleClient
        // MuleManager.getInstance().start();

        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(TEST_URL_NOWSDL, muleContext);
        endpointBuilder.setProperty("wsdlUrl", TEST_URL_WSDL);
        Endpoint endpoint = (Endpoint) muleContext.getRegistry()
            .lookupEndpointFactory()
            .getOutboundEndpoint(endpointBuilder);

        MuleMessage message = new DefaultMuleMessage("test1");
        MuleSession session = new DefaultMuleSession(message, ((AbstractConnector) endpoint.getConnector())
            .getSessionHandler());
        DefaultMuleEvent event = new DefaultMuleEvent(message, endpoint, session, true);
        MuleMessage reply = session.sendEvent(event);

        assertNotNull(reply);

        String response = reply.getPayloadAsString();
        assertNotNull(response);
        XMLAssert.assertXpathEvaluatesTo("test1", "//*[namespace-uri()='http://applications.external.tck.mule.org' and local-name()='key']", response);
        
        
        // bye-bye
        // MuleManager.getInstance().dispose();
    }
}
