/*
 * $Id$
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint.outbound;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.expression.RequiredValueException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.DispatchException;
import org.mule.endpoint.DynamicOutboundEndpoint;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.dynamic.NullConnector;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.mule.TestConnector;


public class DynamicEndpointParsingTestCase extends AbstractMuleTestCase
{
    public DynamicEndpointParsingTestCase()
    {
        setStartContext(true);
    }

    public void testSingleExpression() throws Exception
    {
        OutboundEndpoint endpoint = createEndpoint("test://localhost:#[header:port]");

        assertTrue(endpoint instanceof DynamicOutboundEndpoint);

        assertTrue(endpoint.getConnector() instanceof NullConnector);

        MuleEvent event = getTestEvent("test");
        event.getMessage().setOutboundProperty("port", 12345);

        endpoint.process(event);
        assertTrue(endpoint.getConnector() instanceof TestConnector);


    }

    public void testSingleMultiExpression() throws Exception
    {
        OutboundEndpoint endpoint = createEndpoint("test://#[header:host]:#[header:port]");

        assertTrue(endpoint instanceof DynamicOutboundEndpoint);

        assertTrue(endpoint.getConnector() instanceof NullConnector);

        MuleEvent event = getTestEvent("test");
        event.getMessage().setOutboundProperty("port", 12345);
        event.getMessage().setOutboundProperty("host", "localhost");

        endpoint.process(event);
        assertTrue(endpoint.getConnector() instanceof TestConnector);
    }

    public void testMissingExpressionResult() throws Exception
    {
        OutboundEndpoint endpoint = createEndpoint("test://#[header:host]:#[header:port]");

        assertTrue(endpoint instanceof DynamicOutboundEndpoint);

        assertTrue(endpoint.getConnector() instanceof NullConnector);

        MuleEvent event = getTestEvent("test");
        event.getMessage().setOutboundProperty("port", 12345);

        try
        {
            endpoint.process(event);
            fail("A required header is missing on the message");
        }
        catch (DispatchException e)
        {
            //expected
            assertTrue(e.getCause() instanceof RequiredValueException);
        }
    }

    public void testExpressionInScheme() throws Exception
    {
        try
        {
            createEndpoint("#[header:scheme]://#[header:host]:#[header:port]");
            fail("The scheme part of a dynamic endpoint cannot be an expression");
        }
        catch (MalformedEndpointException e)
        {
            //expected
        }
    }

    public void testMalformedEndpoint() throws Exception
    {
        try
        {
            createEndpoint("test://#[header:host:#[header:port]");
            fail("The endpoint expressions are malformed");
        }
        catch (MalformedEndpointException e)
        {
            //expected
        }
    }

    public void testInboundEndpoint() throws Exception
    {
        //Dynamic inbound endpoints not allowed
        EndpointURIEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder("test://#[header:host]:#[header:port]", muleContext);
        try
        {
            endpointBuilder.buildInboundEndpoint();
            fail("Dynamic inbound endpoints not allowed");
        }
        catch (MalformedEndpointException e)
        {
            //expected
        }
    }

    public void testConnectorURIParam() throws Exception
    {
        TestConnector tc = new TestConnector(muleContext);
        tc.setName("myTestConnector");
        muleContext.getRegistry().registerConnector(tc);

        OutboundEndpoint endpoint = createEndpoint("test://#[header:host]:#[header:port]?connectorName=myTestConnector");

        assertTrue(endpoint instanceof DynamicOutboundEndpoint);

        assertTrue(endpoint.getConnector() instanceof NullConnector);

        MuleEvent event = getTestEvent("test");
        event.getMessage().setOutboundProperty("port", 12345);
        event.getMessage().setOutboundProperty("host", "localhost");

        endpoint.process(event);
        assertTrue(endpoint.getConnector() instanceof TestConnector);
        assertEquals("myTestConnector", endpoint.getConnector().getName());
    }

    public void testMEPURIParam() throws Exception
    {
        OutboundEndpoint endpoint = createEndpoint("test://#[header:host]:#[header:port]");

        assertTrue(endpoint instanceof DynamicOutboundEndpoint);

        assertTrue(endpoint.getConnector() instanceof NullConnector);

        MuleEvent event = getTestEvent("test");
        event.getMessage().setOutboundProperty("port", 12345);
        event.getMessage().setOutboundProperty("host", "localhost");

        endpoint.process(event);
        //The default for the Test connector is ONE_WAY
        assertEquals(MessageExchangePattern.ONE_WAY, endpoint.getExchangePattern());

        //Now test set on the endpoint
        endpoint = createEndpoint("test://#[header:host]:#[header:port]?exchangePattern=REQUEST_RESPONSE");

        assertTrue(endpoint instanceof DynamicOutboundEndpoint);

        assertTrue(endpoint.getConnector() instanceof NullConnector);

        event = getTestEvent("test");
        event.getMessage().setOutboundProperty("port", 12345);
        event.getMessage().setOutboundProperty("host", "localhost");

        endpoint.process(event);
        assertEquals(MessageExchangePattern.REQUEST_RESPONSE, endpoint.getExchangePattern());
    }

    protected OutboundEndpoint createEndpoint(String uri) throws EndpointException, InitialisationException
    {
        EndpointURIEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri, muleContext);

        return endpointBuilder.buildOutboundEndpoint();
    }
    
}
