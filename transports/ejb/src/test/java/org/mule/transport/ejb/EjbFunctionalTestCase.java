/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ejb;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.DispatchException;
import org.mule.config.i18n.Message;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.rmi.RmiConnector;
import org.mule.transport.rmi.i18n.RmiMessages;

import java.util.HashMap;
import java.util.Properties;

/**
 * test RMI object invocations
 */
public class EjbFunctionalTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "ejb-functional-test.xml";
    }

    private MuleMessage send(String uri, String message) throws Exception
    {
        MuleClient client = new MuleClient();
        return client.send(uri, message, new HashMap());
    }

    public void testReverseString() throws Exception
    {
        MuleMessage message = send("ejb://localhost/TestService?method=reverseString", "hello");
        assertNotNull(message.getPayload());
        assertEquals("olleh", message.getPayloadAsString());
    }

    public void testUpperCaseString() throws Exception
    {
        MuleMessage message = send("ejb://localhost/TestService?method=upperCaseString", "hello");
        assertNotNull(message.getPayload());
        assertEquals("HELLO", message.getPayloadAsString());
    }

    public void testNoMethodSet() throws Exception
    {
        try
        {
            send("ejb://localhost/TestService", "hello");

        }
        catch (MuleException e)
        {
            assertTrue(e instanceof DispatchException);
            
            Message message = RmiMessages.messageParamServiceMethodNotSet();
            assertTrue(e.getMessage().startsWith(message.toString()));
        }
    }

    public void testBadMethodName() throws Exception
    {
        try
        {
            send("ejb://localhost/TestService?method=foo", "hello");
        }
        catch (MuleException e)
        {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

    public void testBadMethodType() throws Exception
    {
        // moving this to xml config requires endpoint properties
        // MULE-1790
        EndpointBuilder builder = new EndpointURIEndpointBuilder("ejb://localhost/TestService?method=reverseString",
            muleContext);
        Properties props = new Properties();
        props.put(RmiConnector.PROPERTY_SERVICE_METHOD_PARAM_TYPES, StringBuffer.class.getName());
        builder.setProperties(props);

        ImmutableEndpoint ep = muleContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(
            builder);
        try
        {
            ep.send(getTestEvent("hello", ep));
        }
        catch (MuleException e)
        {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

    public void testCorrectMethodType() throws Exception
    {
        // moving this to xml config requires endpoint properties
        // MULE-1790
        EndpointBuilder builder = new EndpointURIEndpointBuilder("ejb://localhost/TestService?method=reverseString",
            muleContext);
        Properties props = new Properties();
        props.put(RmiConnector.PROPERTY_SERVICE_METHOD_PARAM_TYPES, StringBuffer.class.getName());
        builder.setProperties(props);
        
        ImmutableEndpoint ep = muleContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(
            builder);
        
        try
        {
            ep.send(getTestEvent("hello", ep));
        }
        catch (MuleException e)
        {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

}
