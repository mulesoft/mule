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
import org.mule.module.client.MuleClient;
import org.mule.module.cxf.testmodels.CustomFault;
import org.mule.module.cxf.testmodels.CxfEnabledFaultMessage;
import org.mule.tck.DynamicPortTestCase;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.interceptor.Fault;

public class CxfComponentExceptionStrategyTestCase extends DynamicPortTestCase
{
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
            assertTrue(e.getCause().getCause() instanceof SoapFault);
        }
    }

    /**
     * This doesn't work because of a bug in the CXF client code :-(
     * 
     * @throws Exception
     */
    public void xtestHandledException() throws Exception
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
            assertTrue(e.getCause().getCause() instanceof Fault);
        }
    }

    protected String getConfigResources()
    {
        return "exception-strategy-conf.xml";
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }
}
