/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.endpoint.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.MessageExchangePattern;
import org.mule.VoidMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.expression.RequiredValueException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.DispatchException;
import org.mule.endpoint.DynamicOutboundEndpoint;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class DynamicEndpointParsingTestCase extends AbstractMuleContextTestCase
{

    public DynamicEndpointParsingTestCase()
    {
        setStartContext(true);
    }

    @Test
    public void testDynamicEventMessageSourceURIUntouched() throws Exception
    {
        OutboundEndpoint endpoint = createRequestResponseEndpoint("test://localhost:#[header:port]");
        assertTrue(endpoint instanceof DynamicOutboundEndpoint);

        MuleEvent event = getTestEvent("test");
        event.getMessage().setOutboundProperty("port", 12345);

        MuleEvent response = endpoint.process(event);

        assertEquals("test://test", response.getMessageSourceURI().toString());
    }

    @Test
    public void testMissingExpressionResult() throws Exception
    {
        OutboundEndpoint endpoint = createRequestResponseEndpoint("test://#[header:host]:#[header:port]");

        assertTrue(endpoint instanceof DynamicOutboundEndpoint);

        MuleEvent event = getTestEvent("test");
        event.getMessage().setOutboundProperty("port", 12345);

        try
        {
            endpoint.process(event);
            fail("A required header is missing on the message");
        }
        catch (DispatchException expected)
        {
            assertTrue(expected.getCause() instanceof RequiredValueException);
        }
    }

    @Test(expected = MalformedEndpointException.class)
    public void testExpressionInSchemeIsForbidden() throws Exception
    {
        createRequestResponseEndpoint("#[header:scheme]://#[header:host]:#[header:port]");
    }

    @Test(expected = MalformedEndpointException.class)
    public void testMalformedExpressionInUriIsDetected() throws Exception
    {
        createRequestResponseEndpoint("test://#[header:host:#[header:port]");
    }

    @Test(expected = MalformedEndpointException.class)
    public void testDynamicInboundEndpointNotAllowed() throws Exception
    {
        EndpointURIEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder("test://#[header:host]:#[header:port]", muleContext);
        endpointBuilder.buildInboundEndpoint();
    }

    @Test
    public void testMEPOverridingInUri() throws Exception
    {
        OutboundEndpoint endpoint = createEndpoint("test://#[header:host]:#[header:port]", MessageExchangePattern.ONE_WAY);

        assertTrue(endpoint instanceof DynamicOutboundEndpoint);

        MuleEvent event = getTestEvent("test");
        event.getMessage().setOutboundProperty("port", 12345);
        event.getMessage().setOutboundProperty("host", "localhost");

        MuleEvent response = endpoint.process(event);
        assertSame(VoidMuleEvent.getInstance(), response);

        // Now test set on the endpoint
        endpoint = createRequestResponseEndpoint("test://#[header:host]:#[header:port]?exchangePattern=REQUEST_RESPONSE");

        assertTrue(endpoint instanceof DynamicOutboundEndpoint);

        event = getTestEvent("test");
        event.getMessage().setOutboundProperty("port", 12345);
        event.getMessage().setOutboundProperty("host", "localhost");

        response = endpoint.process(event);
        assertNotNull(response);
        assertEquals(MessageExchangePattern.REQUEST_RESPONSE, endpoint.getExchangePattern());
    }

    protected OutboundEndpoint createRequestResponseEndpoint(String uri) throws EndpointException, InitialisationException
    {
        return createEndpoint(uri, MessageExchangePattern.REQUEST_RESPONSE);
    }

    private OutboundEndpoint createEndpoint(String uri, MessageExchangePattern exchangePattern) throws EndpointException, InitialisationException
    {
        EndpointURIEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri, muleContext);
        endpointBuilder.setExchangePattern(exchangePattern);

        return endpointBuilder.buildOutboundEndpoint();
    }

}
