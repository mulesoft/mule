/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.DispatchException;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.module.client.MuleClient;
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
        MuleClient client = new MuleClient(muleContext);
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
        props.put(RmiConnector.PROPERTY_SERVICE_METHOD_PARAM_TYPES, StringBuffer.class.getName());
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
