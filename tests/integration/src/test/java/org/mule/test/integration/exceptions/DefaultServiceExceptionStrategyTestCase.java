/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.service.Service;
import org.mule.exception.DefaultServiceExceptionStrategy;
import org.mule.message.ExceptionMessage;
import org.mule.module.client.MuleClient;
import org.mule.routing.outbound.MulticastingRouter;
import org.mule.tck.exceptions.FunctionalTestException;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DefaultServiceExceptionStrategyTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/default-service-exception-strategy-config.xml";
    }

    @Test
    public void testDefaultExceptionStrategySingleEndpoint() throws MuleException
    {
        Service service = muleContext.getRegistry().lookupService("testService1");
        assertNotNull(service);
        assertNotNull(service.getExceptionListener());
        assertTrue(service.getExceptionListener() instanceof DefaultServiceExceptionStrategy);
        assertEquals(1, ((DefaultServiceExceptionStrategy) service.getExceptionListener()).getMessageProcessors().size());

        MuleClient mc = new MuleClient(muleContext);
        mc.dispatch("vm://in1", "test", null);
        assertExceptionMessage(mc.request("vm://out1", RECEIVE_TIMEOUT));
        // request one more time to ensure that only one exception message was sent per exception
        assertNull(mc.request("vm://out1", RECEIVE_TIMEOUT));
    }

    @Test
    public void testDefaultExceptionStrategyMultipleEndpoints() throws MuleException
    {
        Service service = muleContext.getRegistry().lookupService("testService2");
        assertNotNull(service);
        assertNotNull(service.getExceptionListener());
        assertTrue(service.getExceptionListener() instanceof DefaultServiceExceptionStrategy);
        DefaultServiceExceptionStrategy exceptionListener = 
            (DefaultServiceExceptionStrategy) service.getExceptionListener();
        MessageProcessor mp = exceptionListener.getMessageProcessors().iterator().next();
        assertTrue(mp.getClass().getName(), mp instanceof MulticastingRouter);
        assertEquals(2, ((MulticastingRouter) mp).getRoutes().size());

        MuleClient mc = new MuleClient(muleContext);
        mc.dispatch("vm://in2", "test", null);
        MuleMessage out2 = mc.request("vm://out2", FunctionalTestCase.RECEIVE_TIMEOUT);
        MuleMessage out3 = mc.request("vm://out3", FunctionalTestCase.RECEIVE_TIMEOUT);
        assertExceptionMessage(out2);
        assertExceptionMessage(out3);
        assertNotSame(out2, out3);
        assertEquals(out2.getPayload(), out3.getPayload());
    }
    
    @Test
    public void testDefaultExceptionStrategyNonEndpoint() throws Exception
    {
        LocalMuleClient mc = muleContext.getClient();

        mc.dispatch("vm://in3", "test", null);

        MuleMessage out4 = mc.request("vm://out4", FunctionalTestCase.RECEIVE_TIMEOUT);
        assertEquals("ERROR!", out4.getPayloadAsString());
    }

    @Test
    public void testSerializablePayload() throws MuleException
    {
        Map<String, String> map = new HashMap<String, String>();
        map.put("key1", "value1");
        map.put("key2", "value2");

        MuleClient mc = new MuleClient(muleContext);
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

    @Test
    public void testStopsServiceOnException() throws MuleException, InterruptedException
    {
        final Service service = muleContext.getRegistry().lookupService("testService5");

        MuleClient mc = new MuleClient(muleContext);
        mc.dispatch("vm://in5", "test", null);

        assertExceptionMessage(mc.request("vm://out5", FunctionalTestCase.RECEIVE_TIMEOUT));

        Prober prober = new PollingProber(5000, 100);
        prober.check(new Probe()
        {
            public boolean isSatisfied()
            {
                return !service.isStarted();
            }

            public String describeFailure()
            {
                return "Service was not stopped after processing the exception";
            }
        });
    }

    private void assertExceptionMessage(MuleMessage out)
    {
        assertTrue(out.getPayload() instanceof ExceptionMessage);
        ExceptionMessage exceptionMessage = (ExceptionMessage) out.getPayload();
        assertEquals(FunctionalTestException.class, exceptionMessage.getException().getCause().getClass());
        assertEquals("test", exceptionMessage.getPayload());
    }
}
