/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.DispatchException;
import org.mule.module.client.MuleClient;
import org.mule.module.cxf.testmodels.CustomFault;
import org.mule.module.cxf.testmodels.CxfEnabledFaultMessage;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.apache.cxf.interceptor.Fault;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        MuleClient client = new MuleClient(muleContext);
        try
        {
            client.send("cxf:" + ((InboundEndpoint) client.getMuleContext().getRegistry()
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
        MuleClient client = new MuleClient(muleContext);
        try
        {
            client.send("cxf:" + ((InboundEndpoint) client.getMuleContext().getRegistry()
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
        MuleClient client = new MuleClient(muleContext);
        try
        {
            client.send("cxf:" + ((InboundEndpoint) client.getMuleContext().getRegistry()
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
        MuleClient client = new MuleClient(muleContext);
        try
        {
            client.send("cxf:" + ((InboundEndpoint) client.getMuleContext().getRegistry()
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
        MuleClient client = new MuleClient(muleContext);
        try
        {
            client.send("cxf:" + ((InboundEndpoint) client.getMuleContext().getRegistry()
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
        MuleClient client = new MuleClient(muleContext);
        try
        {
            client.send("cxf:" + ((InboundEndpoint) client.getMuleContext().getRegistry()
                            .lookupObject("cxfDefaultExceptionStrategyInbound")).getAddress() + "?method=testNonCxfException", "TEST", null);
            fail("Exception expected");
        }
        catch (DispatchException e)
        {
            assertTrue(e.getCause() instanceof Fault);
        }
    }



}
