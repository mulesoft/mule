/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.construct;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.PropertyScope;
import org.mule.construct.AbstractFlowConstruct;
import org.mule.construct.AbstractFlowConstuctTestCase;
import org.mule.tck.MuleTestUtils;

import java.net.URI;
import java.util.Collections;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Test WSDL rewriting.  MULE-5520
 */
public class WSProxyPathBugTestCase extends AbstractFlowConstuctTestCase
{
    private static final String INBOUND_ADDRESS = "test://myhost:8090/weather-forecast";
    private static final String OUTBOUND_ADDRESS = "test://ws.acme.com:6090/weather-forecast";
    private static final String WSDL_LOCATION_SAME_PATH = OUTBOUND_ADDRESS;
    private static final String WSDL_LOCATION_DIFFERENT_PATH = "test://ws.acme.com:6090/wsdl/weather-forecast";

    private WSProxy proxy;
    private MuleEvent inboundEvent;

    @Mock
    private EndpointBuilder mockEndpointBuilder;
    @Mock
    private InboundEndpoint mockWsdlEndpoint;
    @Mock
    private MuleMessage mockMuleMessage;

    private OutboundEndpoint testOutboundEndpoint;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        MockitoAnnotations.initMocks(this);
        when(mockMuleMessage.getPayloadAsString()).thenReturn(OUTBOUND_ADDRESS);
        inboundEvent = getTestEvent(null, getTestInboundEndpoint("test-inbound", INBOUND_ADDRESS));
        when(mockWsdlEndpoint.request(inboundEvent.getTimeout())).thenReturn(mockMuleMessage);
        inboundEvent.getMessage().setProperty("http.request", INBOUND_ADDRESS + "?wsdl", PropertyScope.INBOUND);
        when(mockEndpointBuilder.buildInboundEndpoint()).thenReturn(mockWsdlEndpoint);
        testOutboundEndpoint = MuleTestUtils.getTestOutboundEndpoint(
            MessageExchangePattern.REQUEST_RESPONSE, muleContext, OUTBOUND_ADDRESS, null);
        proxy = new WSProxy("test-proxy", muleContext, directInboundMessageSource, testOutboundEndpoint,
                            Collections.EMPTY_LIST, Collections.EMPTY_LIST);

    }

    @Test
    public void testWithSamePath() throws Exception
    {
        assertOnUriRewrite(WSDL_LOCATION_SAME_PATH);
    }

    @Test
    public void testWithDifferentPath() throws Exception
    {
        assertOnUriRewrite(WSDL_LOCATION_DIFFERENT_PATH);
    }

    private void assertOnUriRewrite(String wsdlLocation) throws Exception
    {
        setUpProxy(wsdlLocation);
        MuleEvent response = directInboundMessageSource.process(inboundEvent);
        assertEquals(INBOUND_ADDRESS, response.getMessageAsString());
    }


    private void setUpProxy(String wsdlLocation) throws Exception
    {
        muleContext.getRegistry().registerEndpointBuilder(wsdlLocation, mockEndpointBuilder);
        proxy = new WSProxy("test-proxy", muleContext, directInboundMessageSource,
            testOutboundEndpoint, Collections.EMPTY_LIST, Collections.EMPTY_LIST, new URI(wsdlLocation));
        proxy.initialise();
        proxy.start();
    }

    @Override
    protected AbstractFlowConstruct getFlowConstruct()
    {
        return proxy;
    }
}
