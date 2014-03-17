/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.DispatchException;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.transport.AbstractFunctionalTestCase;
import org.mule.transport.rmi.RmiConnector;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import org.junit.runners.Parameterized.Parameters;

/**
 * test EJB object invocations
 */
public class EjbFunctionalTestCase extends AbstractFunctionalTestCase
{
    public EjbFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        this.prefix = "ejb";
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "ejb-functional-test-service.xml"},
            {ConfigVariant.FLOW, "ejb-functional-test-flow.xml"}
        });
    }

    @Override
    public void testCase() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("vm://in", "1234567890", null);
        assertNotNull(result);
        assertEquals("0987654321", result.getPayloadAsString());
    }

    @Override
    public void testBadMethodType() throws Exception
    {
        // moving this to xml config requires endpoint properties
        // MULE-1790
        EndpointBuilder builder = new EndpointURIEndpointBuilder("ejb://localhost/TestService?method=reverseString",
            muleContext);
        Properties props = new Properties();
        props.put(RmiConnector.PROPERTY_SERVICE_METHOD_PARAM_TYPES, StringBuilder.class.getName());
        builder.setProperties(props);

        OutboundEndpoint ep = muleContext.getEndpointFactory().getOutboundEndpoint(
            builder);
        try
        {
            ep.process(getTestEvent("hello"));
        }
        catch (Exception e)
        {
            assertTrue(e instanceof DispatchException);
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

    @Override
    public void testCorrectMethodType() throws Exception
    {
        // moving this to xml config requires endpoint properties
        // MULE-1790
        EndpointBuilder builder = new EndpointURIEndpointBuilder("ejb://localhost/TestService?method=reverseString",
            muleContext);
        Properties props = new Properties();
        props.put(RmiConnector.PROPERTY_SERVICE_METHOD_PARAM_TYPES, String.class.getName());
        builder.setProperties(props);

        OutboundEndpoint ep = muleContext.getEndpointFactory().getOutboundEndpoint(
            builder);

        try
        {
            ep.process(getTestEvent("hello"));
        }
        catch (Exception e)
        {
            assertTrue(e instanceof DispatchException);
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }
}
