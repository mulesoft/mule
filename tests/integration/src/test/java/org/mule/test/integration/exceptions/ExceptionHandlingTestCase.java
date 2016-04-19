/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.LocalMuleClient;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAware;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.exception.CatchMessagingExceptionStrategy;
import org.mule.runtime.core.exception.DefaultMessagingExceptionStrategy;
import org.mule.runtime.core.exception.MessagingExceptionHandlerToSystemAdapter;
import org.mule.runtime.core.exception.RollbackMessagingExceptionStrategy;
import org.mule.functional.functional.FlowAssert;
import org.mule.functional.junit4.FunctionalTestCase;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.Mockito;

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
        final MuleEvent muleEvent = runFlow("customProcessorInFlow");
        MuleMessage response = muleEvent.getMessage();

        assertNotNull(response);
        assertTrue(muleEvent.getFlowVariable("expectedHandler"));
        assertTrue(injectedMessagingExceptionHandler instanceof DefaultMessagingExceptionStrategy);
    }

    @Test
    public void testOutboundEndpointInFlow() throws Exception
    {
        flowRunner("outboundEndpointInFlow").withPayload(TEST_MESSAGE).asynchronously().run();

        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.request("test://outFlow2", 3000);
        assertNotNull(response);
    }

    @Test
    public void testOutboundDynamicEndpointInFlow() throws Exception
    {
        flowRunner("outboundDynamicEndpointInFlow").withPayload(MESSAGE).asynchronously().run();

        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.request("test://outFlow3", 3000);
        assertNotNull(response);
    }

    @Test
    public void testAsyncInFlow() throws Exception
    {
        flowRunner("asyncInFlow").withPayload(MESSAGE).asynchronously().run();

        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.request("test://outFlow4", 3000);
        assertNotNull(response);
        assertTrue(injectedMessagingExceptionHandler instanceof CatchMessagingExceptionStrategy);
    }

    @Test
    public void testUntilSuccessfulInFlow() throws Exception
    {
        flowRunner("untilSuccessfulInFlow").withPayload(MESSAGE).asynchronously().run();

        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.request("test://outFlow5", 3000);

        assertNotNull(response);
        assertTrue(injectedMessagingExceptionHandler instanceof CatchMessagingExceptionStrategy);
    }

    @Test
    public void testCustomProcessorInScope() throws Exception
    {
        LinkedList<String> list = new LinkedList<>();
        list.add(MESSAGE);
        final MuleEvent muleEvent = flowRunner("customProcessorInScope").withPayload(list).run();
        MuleMessage response = muleEvent.getMessage();

        assertNotNull(response);
        assertTrue(muleEvent.getFlowVariable("expectedHandler"));
        assertTrue(injectedMessagingExceptionHandler instanceof RollbackMessagingExceptionStrategy);
    }

    @Test
    public void testOutboundEndpointInScope() throws Exception
    {
        LinkedList<String> list = new LinkedList<String>();
        list.add(MESSAGE);
        flowRunner("outboundEndpointInScope").withPayload(list).asynchronously().run();

        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.request("test://outScope2", 3000);

        assertNotNull(response);

        FlowAssert.verify("outboundEndpointInScope");

        assertTrue(injectedMessagingExceptionHandler instanceof RollbackMessagingExceptionStrategy);
    }

    @Test
    public void testOutboundDynamicEndpointInScope() throws Exception
    {
        LinkedList<String> list = new LinkedList<String>();
        list.add(MESSAGE);
        flowRunner("outboundDynamicEndpointInScope").withPayload(list)
                                                    .withInboundProperties(getMessageProperties())
                                                    .asynchronously()
                                                    .run();

        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.request("test://outScope3", 3000);

        assertNotNull(response);

        FlowAssert.verify("outboundDynamicEndpointInScope");

        assertTrue(injectedMessagingExceptionHandler instanceof RollbackMessagingExceptionStrategy);
    }

    @Test
    public void testCustomProcessorInTransactionalScope() throws Exception
    {
        flowRunner("customProcessorInTransactionalScope").withPayload(MESSAGE).asynchronously().run();

        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.request("test://outTransactional1", 3000);

        assertNotNull(response);

        FlowAssert.verify("customProcessorInTransactionalScope");

        assertTrue(injectedMessagingExceptionHandler instanceof CatchMessagingExceptionStrategy);
    }

    @Test
    public void testOutboundEndpointInTransactionalScope() throws Exception
    {
        testTransactionalScope("outboundEndpointInTransactionalScope", "test://outTransactional2", emptyMap());
    }

    @Test
    public void testOutboundDynamicEndpointInTransactionalScope() throws Exception
    {
        testTransactionalScope("outboundDynamicEndpointInTransactionalScope", "test://outTransactional3", getMessageProperties());
    }

    @Test
    public void testAsyncInTransactionalScope() throws Exception
    {
        testTransactionalScope("asyncInTransactionalScope", "test://outTransactional4", emptyMap());
    }

    @Test
    public void testUntilSuccessfulInTransactionalScope() throws Exception
    {
        testTransactionalScope("untilSuccessfulInTransactionalScope", "test://outTransactional5", emptyMap());
        assertTrue(injectedMessagingExceptionHandler instanceof CatchMessagingExceptionStrategy);
    }

    @Test
    public void testCustomProcessorInExceptionStrategy() throws Exception
    {
        flowRunner("customProcessorInExceptionStrategy").withPayload(MESSAGE).asynchronously().run();

        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.request("test://outStrategy1",3000);

        assertNotNull(response);

        FlowAssert.verify("customProcessorInExceptionStrategy");

        assertTrue(injectedMessagingExceptionHandler instanceof MessagingExceptionHandlerToSystemAdapter);
    }

    @Test
    public void testOutboundEndpointInExceptionStrategy() throws Exception
    {
        testExceptionStrategy("outboundEndpointInExceptionStrategy", emptyMap());
    }

    @Test
    public void testOutboundDynamicEndpointInExceptionStrategy() throws Exception
    {
        testExceptionStrategy("outboundDynamicEndpointInExceptionStrategy", getMessageProperties());
    }

    @Test
    public void testAsyncInExceptionStrategy() throws Exception
    {
        testExceptionStrategy("asyncInExceptionStrategy", emptyMap());
        assertTrue(injectedMessagingExceptionHandler instanceof MessagingExceptionHandlerToSystemAdapter);
    }

    @Test
    public void testUntilSuccessfulInExceptionStrategy() throws Exception
    {
        testExceptionStrategy("untilSuccessfulInExceptionStrategy", emptyMap());
        assertTrue(injectedMessagingExceptionHandler instanceof MessagingExceptionHandlerToSystemAdapter);
    }

    @Test
    public void testUntilSuccessfulInExceptionStrategyRollback() throws Exception
    {
        testExceptionStrategy("untilSuccessfulInExceptionStrategyRollback", emptyMap());
        assertTrue(injectedMessagingExceptionHandler instanceof MessagingExceptionHandlerToSystemAdapter);
    }

    private Map<String, Object> getMessageProperties()
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("host", "localhost");
        return props;
    }

    private void testTransactionalScope(String flowName, String expected, Map<String, Object> messageProperties) throws Exception
    {
        flowRunner(flowName).withPayload(MESSAGE)
                            .withInboundProperties(messageProperties)
                            .asynchronously()
                            .run();

        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.request(expected, 3000);

        assertNotNull(response);
    }

    private void testExceptionStrategy(String flowName, Map<String, Object> messageProperties) throws Exception
    {
        latch = spy(new CountDownLatch(2));
        flowRunner(flowName).withPayload(MESSAGE)
                            .withInboundProperties(messageProperties)
                            .asynchronously()
                            .run();

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
            event.setFlowVariable("expectedHandler",expectedHandler);
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
