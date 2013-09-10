/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.client.MuleClient;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.DispatchException;
import org.mule.module.cxf.testmodels.CustomFault;
import org.mule.module.cxf.testmodels.CxfEnabledFaultMessage;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.apache.cxf.interceptor.Fault;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class CxfComponentExceptionStrategyTestCase extends AbstractServiceAndFlowTestCase
{
    public CxfComponentExceptionStrategyTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "exception-strategy-conf-service.xml"},
            {ConfigVariant.FLOW, "exception-strategy-conf-flow.xml"}
        });
    }

    @Test
    public void testDefaultComponentExceptionStrategyWithFault() throws Exception
    {
        MuleClient client = muleContext.getClient();
        try
        {
            client.send("cxf:" + ((InboundEndpoint) muleContext.getRegistry()
                            .lookupObject("cxfExceptionStrategyInbound")).getAddress() + "?method=testFault", "TEST", null);
            fail("Exception expected");
        }
        catch (DispatchException e)
        {
            assertTrue(e.getCause() instanceof Fault);
            assertTrue(e.getCause().getMessage().contains("Invalid data argument"));
        }
    }


    // Test to prove that the CxfComponentExceptionStrategy is not needed anymore to unwrap the Fault, the
    // exception cause is the same with or without the custom exception strategy defined, it is only unwrapped inside of
    // the exception block
    @Test
    public void testDefaultExceptionStrategyWithFault() throws Exception
    {
        MuleClient client = muleContext.getClient();
        try
        {
            client.send("cxf:" + ((InboundEndpoint) muleContext.getRegistry()
                            .lookupObject("cxfDefaultExceptionStrategyInbound")).getAddress() + "?method=testFault", "TEST", null);
            fail("Exception expected");
        }
        catch (DispatchException e)
        {
            assertTrue(e.getCause() instanceof Fault);
            assertTrue(e.getCause().getMessage().contains("Invalid data argument"));
        }
    }

    @Test
    public void testDefaultComponentExceptionStrategyWithCxfException() throws Exception
    {
        MuleClient client = muleContext.getClient();
        try
        {
            client.send("cxf:" + ((InboundEndpoint) muleContext.getRegistry()
                            .lookupObject("cxfExceptionStrategyInbound")).getAddress() + "?method=testCxfException", "TEST", null);
            fail("Exception expected");
        }
        catch (DispatchException e)
        {
            Throwable t = e.getCause();
            assertTrue(t instanceof CxfEnabledFaultMessage);
            CustomFault fault = ((CxfEnabledFaultMessage) t).getFaultInfo();
            assertNotNull(fault);
            assertEquals("Custom Exception Message", fault.getDescription());
        }
    }


    // Test to prove that the CxfComponentExceptionStrategy is not needed anymore to unwrap the Fault, the
    // exception cause is the same with or without the custom exception strategy defined, it is only unwrapped inside of
    // the exception block
    @Test
    public void testDefaultExceptionStrategyWithCxfException() throws Exception
    {
        MuleClient client = muleContext.getClient();
        try
        {
            client.send("cxf:" + ((InboundEndpoint) muleContext.getRegistry()
                            .lookupObject("cxfDefaultExceptionStrategyInbound")).getAddress() + "?method=testCxfException", "TEST", null);
            fail("Exception expected");
        }
        catch (DispatchException e)
        {
            Throwable t = e.getCause();
            assertTrue(t instanceof CxfEnabledFaultMessage);
            CustomFault fault = ((CxfEnabledFaultMessage) t).getFaultInfo();
            assertNotNull(fault);
            assertEquals("Custom Exception Message", fault.getDescription());
        }
    }

    @Test
    public void testDefaultComponentExceptionStrategyWithException() throws Exception
    {
        MuleClient client = muleContext.getClient();
        try
        {
            client.send("cxf:" + ((InboundEndpoint) muleContext.getRegistry()
                            .lookupObject("cxfExceptionStrategyInbound")).getAddress() + "?method=testNonCxfException", "TEST", null);
            fail("Exception expected");
        }
        catch (DispatchException e)
        {
            assertTrue(e.getCause() instanceof Fault);
        }
    }

    // Test to prove that the CxfComponentExceptionStrategy is not needed anymore to unwrap the Fault, the
    // exception cause is the same with or without the custom exception strategy defined, it is only unwrapped inside of
    // the exception block
    @Test
    public void testDefaultExceptionStrategyWithException() throws Exception
    {
        MuleClient client = muleContext.getClient();
        try
        {
            client.send("cxf:" + ((InboundEndpoint) muleContext.getRegistry()
                            .lookupObject("cxfDefaultExceptionStrategyInbound")).getAddress() + "?method=testNonCxfException", "TEST", null);
            fail("Exception expected");
        }
        catch (DispatchException e)
        {
            assertTrue(e.getCause() instanceof Fault);
        }
    }
}
