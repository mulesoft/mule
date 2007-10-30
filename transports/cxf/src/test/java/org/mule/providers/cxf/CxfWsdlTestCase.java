/*
 * $Id: XFireWsdlTestCase.java 6489 2007-05-11 14:00:13Z Lajos $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.cxf;

import org.custommonkey.xmlunit.XMLAssert;
import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.providers.AbstractConnector;
import org.mule.registry.Registry;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;

import org.w3c.dom.Document;

public class CxfWsdlTestCase extends AbstractMuleTestCase
{
    public static final String TEST_URL = "wsdl-cxf:http://localhost:8080/mule-tests-external-cxf/services/TestService?WSDL&method=getTest";
    public static final String TEST_URL_NOWSDL = "wsdl-cxf:http://localhost:8080/mule-tests-external-cxf/services/TestService?method=getTest";
    public static final String TEST_URL_WSDL = "http://localhost:8080/mule-tests-external-cxf/services/TestService?wsdl";

    public void testCxfWsdlService() throws Exception
    {
        MuleClient client = new MuleClient();

        UMOMessage message = new MuleMessage("test1");
        UMOMessage reply = client.send(TEST_URL, message);
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
    public void testCxfWsdlServiceWithEndpointParam() throws Exception
    {

        Registry registry = managementContext.getRegistry();

        UMOEndpoint endpoint = (UMOEndpoint) registry.lookupEndpointFactory().getOutboundEndpoint(TEST_URL_NOWSDL,
            managementContext);
        endpoint.setProperty("wsdlUrl", TEST_URL_WSDL);

        UMOMessage message = new MuleMessage("test1");
        UMOSession session = new MuleSession(message,
            ((AbstractConnector) endpoint.getConnector()).getSessionHandler());
        MuleEvent event = new MuleEvent(message, endpoint, session, true);
        UMOMessage reply = session.sendEvent(event);

        assertNotNull(reply);

        Document response = (Document) reply.getPayload();
        assertNotNull(response);

        XMLAssert.assertXpathEvaluatesTo("test1",
            "//*[namespace-uri()='http://applications.external.tck.mule.org' and local-name()='key']",
            response);
    }
}
