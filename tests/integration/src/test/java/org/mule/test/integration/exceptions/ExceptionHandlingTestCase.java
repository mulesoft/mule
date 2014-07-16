/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.exception.MessagingExceptionHandlerAware;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.PropertyScope;
import org.mule.exception.CatchMessagingExceptionStrategy;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.mule.exception.MessagingExceptionHandlerToSystemAdapter;
import org.mule.exception.RollbackMessagingExceptionStrategy;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class ExceptionHandlingTestCase extends FunctionalTestCase
{
    public static final String MESSAGE = "some message";

    private static MessagingExceptionHandler injectedMessagingExceptionHandler;
    private static CountDownLatch latch;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/exceptions/exception-handling-test.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        injectedMessagingExceptionHandler = null;
    }

    @Test
    public void testCustomProcessorInFlow() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://inFlow1", MESSAGE, null);

        assertNotNull(response);
        assertTrue((Boolean) response.getProperty("expectedHandler", PropertyScope.SESSION));
        assertTrue(injectedMessagingExceptionHandler instanceof DefaultMessagingExceptionStrategy);
    }

    @Test
    public void testOutboundEndpointInFlow() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://inFlow2", MESSAGE, null);
        MuleMessage response = client.request("vm://outFlow2", 3000);
        assertNotNull(response);
    }

    @Test
    public void testOutboundDynamicEndpointInFlow() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://inFlow3", MESSAGE, getMessageProperties());
        MuleMessage response = client.request("vm://outFlow3", 3000);
        assertNotNull(response);
    }

    @Test
    public void testAsyncInFlow() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://inFlow4", MESSAGE, null);
        MuleMessage response = client.request("vm://outFlow4", 3000);
        assertNotNull(response);
        assertTrue(injectedMessagingExceptionHandler instanceof CatchMessagingExceptionStrategy);
    }

    @Test
    public void testUntilSuccessfulInFlow() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://inFlow5", MESSAGE, null);
        MuleMessage response = client.request("vm://outFlow5", 3000);

        assertNotNull(response);
        assertTrue(injectedMessagingExceptionHandler instanceof CatchMessagingExceptionStrategy);
    }

    @Test
    public void testCustomProcessorInScope() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        LinkedList<String> list = new LinkedList<String>();
        list.add(MESSAGE);
        MuleMessage response = client.send("vm://inScope1", list, null);

        assertNotNull(response);
        assertTrue((Boolean) response.getProperty("expectedHandler", PropertyScope.SESSION));
        assertTrue(injectedMessagingExceptionHandler instanceof RollbackMessagingExceptionStrategy);
    }

    @Test
    public void testOutboundEndpointInScope() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        LinkedList<String> list = new LinkedList<String>();
        list.add(MESSAGE);
        client.send("vm://inScope2", list, null);
        MuleMessage response = client.request("vm://outScope2", 3000);

        assertNotNull(response);
        assertTrue((Boolean) response.getProperty("expectedHandler", PropertyScope.SESSION));
        assertTrue(injectedMessagingExceptionHandler instanceof RollbackMessagingExceptionStrategy);
    }

    @Test
    public void testOutboundDynamicEndpointInScope() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        LinkedList<String> list = new LinkedList<String>();
        list.add(MESSAGE);
        client.send("vm://inScope3", list, getMessageProperties());
        MuleMessage response = client.request("vm://outScope3", 3000);

        assertNotNull(response);
        assertTrue((Boolean) response.getProperty("expectedHandler", PropertyScope.SESSION));
        assertTrue(injectedMessagingExceptionHandler instanceof RollbackMessagingExceptionStrategy);
    }

    @Test
    public void testCustomProcessorInTransactionalScope() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://inTransactional1", MESSAGE, null);
        MuleMessage response = client.request("vm://outTransactional1", 3000);

        assertNotNull(response);
        assertFalse((Boolean) response.getProperty("expectedHandler", PropertyScope.SESSION));
        assertTrue(injectedMessagingExceptionHandler instanceof CatchMessagingExceptionStrategy);
    }

    @Test
    public void testOutboundEndpointInTransactionalScope() throws Exception
    {
        testTransactionalScope("vm://inTransactional2", "vm://outTransactional2", null);
    }

    @Test
    public void testOutboundDynamicEndpointInTransactionalScope() throws Exception
    {
        testTransactionalScope("vm://inTransactional3", "vm://outTransactional3", getMessageProperties());
    }

    @Test
    public void testAsyncInTransactionalScope() throws Exception
    {
        testTransactionalScope("vm://inTransactional4", "vm://outTransactional4", null);
    }

    @Test
    public void testUntilSuccessfulInTransactionalScope() throws Exception
    {
        testTransactionalScope("vm://inTransactional5", "vm://outTransactional5", null);
        assertTrue(injectedMessagingExceptionHandler instanceof CatchMessagingExceptionStrategy);
    }

    @Test
    public void testCustomProcessorInExceptionStrategy() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://inStrategy1", MESSAGE, null);
        MuleMessage response = client.request("vm://outStrategy1",3000);

        assertNotNull(response);
        assertFalse((Boolean) response.getProperty("expectedHandler", PropertyScope.SESSION));
        assertTrue(injectedMessagingExceptionHandler instanceof MessagingExceptionHandlerToSystemAdapter);
    }

    @Test
    public void testOutboundEndpointInExceptionStrategy() throws Exception
    {

        testExceptionStrategy("vm://inStrategy2", null);
    }

    @Test
    public void testOutboundDynamicEndpointInExceptionStrategy() throws Exception
    {
        testExceptionStrategy("vm://inStrategy3", getMessageProperties());
    }

    @Test
    public void testAsyncInExceptionStrategy() throws Exception
    {
        testExceptionStrategy("vm://inStrategy4", null);
        assertTrue(injectedMessagingExceptionHandler instanceof MessagingExceptionHandlerToSystemAdapter);
    }

    @Test
    public void testUntilSuccessfulInExceptionStrategy() throws Exception
    {
        testExceptionStrategy("vm://inStrategy5", null);
        assertTrue(injectedMessagingExceptionHandler instanceof MessagingExceptionHandlerToSystemAdapter);
    }

    @Test
    public void testUntilSuccessfulInExceptionStrategyRollback() throws Exception
    {
        testExceptionStrategy("vm://inStrategy6", null);
        assertTrue(injectedMessagingExceptionHandler instanceof MessagingExceptionHandlerToSystemAdapter);
    }

    private Map<String, Object> getMessageProperties()
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("host", "localhost");
        return props;
    }

    private void testTransactionalScope(String destination, String expected, Map<String, Object> messageProperties) throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send(destination, MESSAGE, messageProperties);
        MuleMessage response = client.request(expected, 3000);

        assertNotNull(response);
    }

    private void testExceptionStrategy(String destination, Map<String, Object> messageProperties) throws MuleException, InterruptedException
    {
        latch = spy(new CountDownLatch(2));
        LocalMuleClient client = muleContext.getClient();
        client.dispatch(destination, MESSAGE, messageProperties);

        assertFalse(latch.await(3, TimeUnit.SECONDS));
        verify(latch).countDown();
    }

    public static class ExecutionCountProcessor implements MessageProcessor
    {
        @Override
        public synchronized MuleEvent process(MuleEvent event) throws MuleException
        {
            latch.countDown();
            return event;
        }
    }

    public static class ExceptionHandlerVerifierProcessor implements MessageProcessor, MessagingExceptionHandlerAware
    {

        private MessagingExceptionHandler messagingExceptionHandler;

        @Override
        public synchronized MuleEvent process(MuleEvent event) throws MuleException
        {
            Boolean expectedHandler = messagingExceptionHandler != null;
            if (expectedHandler)
            {
                expectedHandler = messagingExceptionHandler.equals(event.getFlowConstruct().getExceptionListener());
            }
            event.getMessage().setProperty("expectedHandler",expectedHandler, PropertyScope.SESSION);
            injectedMessagingExceptionHandler = messagingExceptionHandler;
            return event;
        }

        @Override
        public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler)
        {
            if (this.messagingExceptionHandler == null)
            {
                this.messagingExceptionHandler = messagingExceptionHandler;
            }
        }

    }
}
