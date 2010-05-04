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

import java.util.HashMap;
import java.util.Map;

public class DefaultServiceExceptionStrategyTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/default-service-exception-strategy-config.xml";
    }

    public void testDefaultExceptionStrategySingleEndpoint() throws MuleException
    {
        assertExceptionStrategyHasNumberOfEndpoints("testService1", 1);

        MuleClient mc = new MuleClient();
        mc.dispatch("vm://in1", "test", null);
        assertExceptionMessage(mc.request("vm://out1", RECEIVE_TIMEOUT));
        // request one more time to ensure that only one exception message was sent per exception
        assertNull(mc.request("vm://out1", RECEIVE_TIMEOUT));
    }

    public void testDefaultExceptionStrategyMultipleEndpoints() throws MuleException
    {
        assertExceptionStrategyHasNumberOfEndpoints("testService2", 2);

        MuleClient mc = new MuleClient();
        mc.dispatch("vm://in2", "test", null);
        MuleMessage out2 = mc.request("vm://out2", FunctionalTestCase.RECEIVE_TIMEOUT);
        MuleMessage out3 = mc.request("vm://out3", FunctionalTestCase.RECEIVE_TIMEOUT);
        assertExceptionMessage(out2);
        assertExceptionMessage(out3);
        assertNotSame(out2, out3);
        assertEquals(out2.getPayload(), out3.getPayload());
    }

    public void testSerializablePayload() throws MuleException
    {
        Map<String, String> map = new HashMap<String, String>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        
        MuleClient mc = new MuleClient();
        mc.dispatch("vm://in1", map, null);
        MuleMessage message = mc.request("vm://out1", FunctionalTestCase.RECEIVE_TIMEOUT);

        assertTrue(message.getPayload() instanceof ExceptionMessage);
        Object payload = ((ExceptionMessage) message.getPayload()).getPayload();
        assertTrue("payload shoud be a Map, but is " + payload.getClass().getName(), 
            payload instanceof Map<?, ?>);
        Map<?, ?> payloadMap = (Map<?, ?>) payload;
        assertEquals("value1", payloadMap.get("key1"));
        assertEquals("value2", payloadMap.get("key2"));
    }

    private void assertExceptionMessage(MuleMessage out)
    {
        assertTrue(out.getPayload() instanceof ExceptionMessage);
        ExceptionMessage exceptionMessage = (ExceptionMessage) out.getPayload();
        assertEquals(FunctionalTestException.class, exceptionMessage.getException().getCause().getClass());
        assertEquals("test", exceptionMessage.getPayload());
    }
    
    private void assertExceptionStrategyHasNumberOfEndpoints(String serviceName, int numberOfEndpoints)
    {
        Service service = muleContext.getRegistry().lookupService(serviceName);
        assertNotNull(service);
        assertNotNull(service.getExceptionListener());
        assertTrue(service.getExceptionListener() instanceof DefaultServiceExceptionStrategy);
        DefaultServiceExceptionStrategy exceptionListener = 
            (DefaultServiceExceptionStrategy) service.getExceptionListener();
        assertEquals(numberOfEndpoints, exceptionListener.getEndpoints().size());
    }
}
