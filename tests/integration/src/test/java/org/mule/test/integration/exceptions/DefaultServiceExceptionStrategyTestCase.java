/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.exceptions;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.service.Service;
import org.mule.message.ExceptionMessage;
import org.mule.module.client.MuleClient;
import org.mule.service.DefaultServiceExceptionStrategy;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.exceptions.FunctionalTestException;

public class DefaultServiceExceptionStrategyTestCase extends FunctionalTestCase
{

    // @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/default-service-exception-strategy-config.xml";
    }

    public void testDefaultExceptionStrategySingleEndpoint() throws MuleException
    {
        Service service1 = muleContext.getRegistry().lookupService("testService1");
        assertNotNull(service1);
        assertNotNull(service1.getExceptionListener());
        assertTrue(service1.getExceptionListener() instanceof DefaultServiceExceptionStrategy);
        assertEquals(1, ((DefaultServiceExceptionStrategy) service1.getExceptionListener()).getEndpoints().size());

        MuleClient mc = new MuleClient();
        mc.dispatch("vm://in1", "test", null);
        assertExceptionMessage(mc.request("vm://out1", FunctionalTestCase.RECEIVE_TIMEOUT));
    }

    public void testDefaultExceptionStrategyMultipleEndpoints() throws MuleException
    {
        Service service2 = muleContext.getRegistry().lookupService("testService2");
        assertNotNull(service2);
        assertNotNull(service2.getExceptionListener());
        assertTrue(service2.getExceptionListener() instanceof DefaultServiceExceptionStrategy);
        assertEquals(2, ((DefaultServiceExceptionStrategy) service2.getExceptionListener()).getEndpoints().size());

        MuleClient mc = new MuleClient();
        mc.dispatch("vm://in2", "test", null);
        MuleMessage out2 = mc.request("vm://out2", FunctionalTestCase.RECEIVE_TIMEOUT);
        MuleMessage out3 = mc.request("vm://out3", FunctionalTestCase.RECEIVE_TIMEOUT);
        assertExceptionMessage(out2);
        assertExceptionMessage(out3);
        assertNotSame(out2, out3);
        assertEquals(out2.getPayload(), out3.getPayload());
    }

    private void assertExceptionMessage(MuleMessage out)
    {
        assertTrue(out.getPayload() instanceof ExceptionMessage);
        assertEquals(FunctionalTestException.class, ((ExceptionMessage) out.getPayload()).getException()
            .getCause()
            .getClass());
        assertEquals("test", ((ExceptionMessage) out.getPayload()).getPayload());
    }
}
