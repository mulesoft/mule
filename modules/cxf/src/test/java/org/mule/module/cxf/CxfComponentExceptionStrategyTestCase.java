/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf;

import org.mule.api.MessagingException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.config.ExceptionHelper;
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
    public void testDefaultComponentExceptionStrategy() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        try
        {
            client.send("cxf:" + ((InboundEndpoint) client.getMuleContext().getRegistry()
                            .lookupObject("cxfDefaultInbound")).getAddress() + "?method=testCxfException", "TEST", null);
            fail("Exception expected");
        }
        catch (MessagingException e)
        {
            assertTrue(ExceptionHelper.getRootException(e) instanceof CxfEnabledFaultMessage);
        }
    }

    @Test
    @Ignore("This doesn't work because of a bug in the CXF client code :-(")
    public void testHandledException() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        try
        {
            client.send("cxf:" + ((InboundEndpoint) client.getMuleContext().getRegistry()
                            .lookupObject("cxfExceptionStrategyInbound")).getAddress() + "?method=testCxfException", "TEST", null);
            fail("Exception expected");
        }
        catch (MessagingException e)
        {
            Throwable t = e.getCause().getCause();
            assertTrue(t instanceof CxfEnabledFaultMessage);
            CustomFault fault = ((CxfEnabledFaultMessage) t).getFaultInfo();
            assertNotNull(fault);
            assertEquals("Custom Exception Message", fault.getDescription());
        }
    }

    @Test
    public void testUnhandledException() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        try
        {
            client.send("cxf:" + ((InboundEndpoint) client.getMuleContext().getRegistry()
                            .lookupObject("cxfExceptionStrategyInbound")).getAddress() + "?method=testNonCxfException", "TEST", null);
            fail("Exception expected");
        }
        catch (MessagingException e)
        {
            assertTrue(ExceptionHelper.getRootException(e) instanceof Fault);
        }
    }

}
