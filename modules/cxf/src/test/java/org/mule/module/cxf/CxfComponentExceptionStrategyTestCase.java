/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf;

import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.module.cxf.testmodels.CustomFault;
import org.mule.module.cxf.testmodels.CxfEnabledFaultMessage;
import org.mule.tck.FunctionalTestCase;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.interceptor.Fault;

public class CxfComponentExceptionStrategyTestCase extends FunctionalTestCase
{
    public void testDefaultComponentExceptionStrategy() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage result = client.send("cxf:http://localhost:63181/services/CxfDefault?method=testCxfException", "TEST", null);
        assertNotNull(result);
        assertNotNull("Exception expected", result.getExceptionPayload());
        assertTrue(result.getExceptionPayload().getException() instanceof MessagingException);
        assertTrue(result.getExceptionPayload().getRootException().toString(), 
                   result.getExceptionPayload().getRootException() instanceof SoapFault);
    }

    /**
     * This doesn't work because of a bug in the CXF client code :-(
     * 
     * @throws Exception
     */
    public void xtestHandledException() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage result = client.send("cxf:http://localhost:63181/services/CxfWithExceptionStrategy?method=testCxfException", "TEST", null);
        assertNotNull(result);
        assertNotNull("Exception expected", result.getExceptionPayload());
        assertTrue(result.getExceptionPayload().getException() instanceof MessagingException);
        assertTrue(result.getExceptionPayload().getRootException() instanceof CxfEnabledFaultMessage);
        CxfEnabledFaultMessage cxfMsg = (CxfEnabledFaultMessage) result.getExceptionPayload().getRootException();
        CustomFault fault = cxfMsg.getFaultInfo();
        assertNotNull(fault);
        assertEquals("Custom Exception Message", fault.getDescription());
    }

    public void testUnhandledException() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage result = client.send("cxf:http://localhost:63181/services/CxfWithExceptionStrategy?method=testNonCxfException", "TEST", null);
        assertNotNull(result);
        assertNotNull("Exception expected", result.getExceptionPayload());
        assertTrue(result.getExceptionPayload().getException() instanceof MessagingException);
        assertTrue(result.getExceptionPayload().getRootException() instanceof Fault);
    }

    protected String getConfigResources()
    {
        return "exception-strategy-conf.xml";
    }
}
