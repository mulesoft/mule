/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.api.transport.DispatchException;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.cxf.testmodels.CustomFault;
import org.mule.transport.cxf.testmodels.CxfEnabledFaultMessage;

import org.apache.cxf.interceptor.Fault;

public class CxfComponentExceptionStrategyTestCase extends FunctionalTestCase
{
    public void testDefaultComponentExceptionStrategy() throws Exception
    {
        MuleClient client = new MuleClient();

        try
        {
            client.send("cxf:http://localhost:63181/services/CxfDefault?method=testCxfException", "TEST",
                null);
        }
        catch (DispatchException e)
        {
            assertTrue(e.getCause() instanceof CxfEnabledFaultMessage);
        }
    }

    /**
     * This doesn't work because of a bug in the CXF client code :-(
     * 
     * @throws Exception
     */
    public void xtestHandledException() throws Exception
    {
        MuleClient client = new MuleClient();

        try
        {
            client.send(
                "cxf:http://localhost:63181/services/CxfWithExceptionStrategy?method=testCxfException",
                "TEST", null);
        }
        catch (DispatchException ex)
        {
            final Throwable t = ex.getCause();
            t.printStackTrace();
            assertNotNull("Cause should've been filled in.", t);
            assertTrue(t instanceof CxfEnabledFaultMessage);
            CxfEnabledFaultMessage cxfMsg = (CxfEnabledFaultMessage) t;
            CustomFault fault = cxfMsg.getFaultInfo();
            assertNotNull(fault);
            assertEquals("Custom Exception Message", fault.getDescription());
        }
    }

    public void testUnhandledException() throws Exception
    {
        MuleClient client = new MuleClient();

        try
        {
            client.send(
                "cxf:http://localhost:63181/services/CxfWithExceptionStrategy?method=testNonCxfException",
                "TEST", null);
        }
        catch (DispatchException e)
        {
            assertTrue(e.getCause() instanceof Fault);
        }
    }

    protected String getConfigResources()
    {
        return "exception-strategy-conf.xml";
    }
}
