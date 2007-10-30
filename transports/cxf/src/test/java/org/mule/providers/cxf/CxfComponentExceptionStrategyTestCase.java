/*
 * $Id: XFireComponentExceptionStrategyTestCase.java 6306 2007-05-04 03:02:55Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.cxf;

import org.mule.extras.client.MuleClient;
import org.mule.providers.cxf.testmodels.CustomFault;
import org.mule.providers.cxf.testmodels.CxfEnabledFaultMessage;
import org.mule.tck.FunctionalTestCase;

import org.apache.cxf.interceptor.Fault;

public class CxfComponentExceptionStrategyTestCase extends FunctionalTestCase
{
    public void testDefaultComponentExceptionStrategy() throws Exception
    {
        MuleClient client = new MuleClient();

        try
        {
            client.send("cxf:http://localhost:63181/services/CxfDefault?method=testXFireException", "TEST",
                null);
        }
        catch (org.mule.umo.provider.DispatchException ex)
        {
            final Throwable t = ex.getCause();
            assertNotNull("Cause should've been filled in.", t);
            assertTrue(t instanceof Fault);
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
                "cxf:http://localhost:63181/services/CxfWithExceptionStrategy?method=testXFireException",
                "TEST", null);
        }
        catch (org.mule.umo.provider.DispatchException ex)
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
                "cxf:http://localhost:63181/services/CxfWithExceptionStrategy?method=testNonXFireException",
                "TEST", null);
        }
        catch (org.mule.umo.provider.DispatchException ex)
        {
            final Throwable t = ex.getCause();
            assertNotNull("Cause should've been filled in.", t);
            assertTrue(t instanceof Fault);
        }
    }

    protected String getConfigResources()
    {
        return "exception-strategy-conf.xml";
    }
}
