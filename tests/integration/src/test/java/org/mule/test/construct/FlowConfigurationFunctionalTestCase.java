/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.construct;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.PropertyScope;
import org.mule.construct.Flow;
import org.mule.endpoint.DefaultInboundEndpoint;
import org.mule.source.StartableCompositeMessageSource;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowConfigurationFunctionalTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/construct/flow.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.setDisposeManagerPerSuite(true);
        super.doSetUp();
    }

    public void testFlow() throws MuleException, Exception
    {
        final Flow flow = muleContext.getRegistry().lookupObject("flow");
        assertEquals(DefaultInboundEndpoint.class, flow.getMessageSource().getClass());
        assertEquals("vm://in", ((InboundEndpoint) flow.getMessageSource()).getEndpointURI()
            .getUri()
            .toString());
        assertEquals(5, flow.getMessageProcessors().size());
        assertNotNull(flow.getExceptionListener());

        assertEquals("012xyzabc3", muleContext.getClient().send("vm://in",
            new DefaultMuleMessage("0", muleContext)).getPayloadAsString());

    }
    
    public void testFlowSynchronous() throws MuleException
    {
        muleContext.getClient().send("vm://synchronous", new DefaultMuleMessage("0", muleContext));
        MuleMessage message = muleContext.getClient().request("vm://synchronous-out", RECEIVE_TIMEOUT);
        assertNotNull(message);
        Thread thread = (Thread) message.getPayload();
        assertNotNull(thread);
        assertEquals(Thread.currentThread(), thread);
    }

    public void testFlowAynchronous() throws MuleException
    {
        muleContext.getClient().send("vm://asynchronous", new DefaultMuleMessage("0", muleContext));
        MuleMessage message = muleContext.getClient().request("vm://asynchronous-out", RECEIVE_TIMEOUT);
        assertNotNull(message);
        Thread thread = (Thread) message.getPayload();
        assertNotNull(thread);
        assertNotSame(Thread.currentThread(), thread);
    }

    public void testFlowQueuedAsynchronous() throws MuleException
    {
        muleContext.getClient().send("vm://queued-asynchronous", new DefaultMuleMessage("0", muleContext));
        MuleMessage message = muleContext.getClient().request("vm://queued-asynchronous-out", RECEIVE_TIMEOUT);
        assertNotNull(message);
        Thread thread = (Thread) message.getPayload();
        assertNotNull(thread);
        assertNotSame(Thread.currentThread(), thread);
    }

    public void testFlowCompositeSource() throws MuleException, Exception
    {
        final Flow flow = muleContext.getRegistry().lookupObject("flow2");
        assertEquals(StartableCompositeMessageSource.class, flow.getMessageSource().getClass());
        assertEquals(2, flow.getMessageProcessors().size());

        assertEquals("01xyz", muleContext.getClient().send("vm://in2",
            new DefaultMuleMessage("0", muleContext)).getPayloadAsString());
        assertEquals("01xyz", muleContext.getClient().send("vm://in3",
            new DefaultMuleMessage("0", muleContext)).getPayloadAsString());

    }

    public void testInOutFlow() throws MuleException, Exception
    {
        muleContext.getClient().send("vm://inout-in", new DefaultMuleMessage("0", muleContext));
        assertEquals("0", muleContext.getClient()
            .request("vm://inout-out", RECEIVE_TIMEOUT)
            .getPayloadAsString());
    }

    public void testInOutAppendFlow() throws MuleException, Exception
    {
        muleContext.getClient().send("vm://inout-append-in", new DefaultMuleMessage("0", muleContext));
        assertEquals("0inout", muleContext.getClient()
            .request("vm://inout-append-out", RECEIVE_TIMEOUT)
            .getPayloadAsString());
    }

    public void testSplitAggregateFlow() throws MuleException, Exception
    {
        final Apple apple = new Apple();
        final Banana banana = new Banana();
        final Orange orange = new Orange();
        final FruitBowl fruitBowl = new FruitBowl(apple, banana);
        fruitBowl.addFruit(orange);

        muleContext.getClient().send("vm://split-aggregate-in",
            new DefaultMuleMessage(fruitBowl, muleContext));

        final MuleMessage result = muleContext.getClient().request("vm://split-aggregate-out",
            RECEIVE_TIMEOUT);

        assertNotNull(result);
        assertTrue(result instanceof MuleMessageCollection);
        final MuleMessageCollection coll = (MuleMessageCollection) result;
        assertEquals(3, coll.size());
        final List<?> results = (List<?>) coll.getPayload();

        assertTrue(apple.isBitten());
        assertTrue(banana.isBitten());
        assertTrue(orange.isBitten());

        assertTrue(results.contains(apple));
        assertTrue(results.contains(banana));
        assertTrue(results.contains(orange));
    }

    public void testSplitAggregateListFlow() throws MuleException, Exception
    {
        final Apple apple = new Apple();
        final Banana banana = new Banana();
        final Orange orange = new Orange();
        final FruitBowl fruitBowl = new FruitBowl(apple, banana);
        fruitBowl.addFruit(orange);

        muleContext.getClient().send("vm://split-aggregate-list-in",
            new DefaultMuleMessage(fruitBowl.getFruit(), muleContext));

        final MuleMessage result = muleContext.getClient().request("vm://split-aggregate-list-out",
            RECEIVE_TIMEOUT);

        assertNotNull(result);
        assertTrue(result instanceof MuleMessageCollection);
        final MuleMessageCollection coll = (MuleMessageCollection) result;
        assertEquals(3, coll.size());
        final List<?> results = (List<?>) coll.getPayload();

        assertTrue(apple.isBitten());
        assertTrue(banana.isBitten());
        assertTrue(orange.isBitten());

        assertTrue(results.contains(apple));
        assertTrue(results.contains(banana));
        assertTrue(results.contains(orange));
    }

    public void testSplitAggregateMapFlow() throws MuleException, Exception
    {
        Map map = new HashMap<String, Fruit>();
        final Apple apple = new Apple();
        final Banana banana = new Banana();
        final Orange orange = new Orange();
        map.put("apple", apple);
        map.put("banana", banana);
        map.put("orange", orange);

        MuleEvent result = ((Flow) muleContext.getRegistry().lookupFlowConstruct("split-map")).process(getTestEvent(map));

        assertNotNull(result);
        assertTrue(result.getMessage() instanceof MuleMessageCollection);

        final MuleMessageCollection coll = (MuleMessageCollection) result.getMessage();
        assertEquals(3, coll.size());
        final MuleMessage[] results = coll.getMessagesAsArray();

        assertTrue(apple.isBitten());
        assertTrue(banana.isBitten());
        assertTrue(orange.isBitten());

        assertNotNull(results[0].getProperty("key", PropertyScope.INVOCATION));
        assertNotNull(results[1].getProperty("key", PropertyScope.INVOCATION));
        assertNotNull(results[2].getProperty("key", PropertyScope.INVOCATION));
    }
    
    public void testSplitFilterAggregateFlow() throws MuleException, Exception
    {
        final Apple apple = new Apple();
        final Banana banana = new Banana();
        final Orange orange = new Orange();
        final FruitBowl fruitBowl = new FruitBowl(apple, banana);
        fruitBowl.addFruit(orange);

        muleContext.getClient().send("vm://split-filter-aggregate-in",
            new DefaultMuleMessage(fruitBowl, muleContext));

        final MuleMessage result = muleContext.getClient().request("vm://split-filter-aggregate-out",
            RECEIVE_TIMEOUT);

        assertNotNull(result);
        assertTrue(result instanceof MuleMessageCollection);
        final MuleMessageCollection coll = (MuleMessageCollection) result;
        assertEquals(1, coll.size());
        final List<?> results = (List<?>) coll.getPayload();

        assertTrue(results.contains(apple));
        assertFalse(results.contains(banana));
        assertFalse(results.contains(orange));
    }

    public void testMessageChunkSplitAggregateFlow() throws MuleException, Exception
    {
        String payload = "";
        for (int i = 0; i < 100; i++)
        {
            payload += TEST_MESSAGE;
        }

        muleContext.getClient().send("vm://message-chunk-split-aggregate-in",
            new DefaultMuleMessage(payload, muleContext));

        final MuleMessage result = muleContext.getClient().request("vm://message-chunk-split-aggregate-out",
            RECEIVE_TIMEOUT);

        assertNotNull(result);
        assertNotSame(payload, result.getPayload());
        assertEquals(payload, result.getPayloadAsString());
    }

    public void testComponentsFlow() throws MuleException, Exception
    {
        final MuleMessage result = muleContext.getClient().send("vm://components",
            new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertNotNull(result);
        assertNotSame(TEST_MESSAGE + "test", result.getPayload());
    }

    public void testWireTapFlow() throws MuleException, Exception
    {
        muleContext.getClient().send("vm://wiretap-in", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        final MuleMessage result = muleContext.getClient().request("vm://wiretap-out", RECEIVE_TIMEOUT);
        final MuleMessage tapResult = muleContext.getClient().request("vm://wiretap-tap", RECEIVE_TIMEOUT);

        assertNotNull(result);
        assertNotNull(tapResult);
        assertNotSame(result, tapResult);
        assertEquals(TEST_MESSAGE + "inout", result.getPayloadAsString());
        assertEquals(TEST_MESSAGE + "intap", tapResult.getPayloadAsString());
    }

    public void testResponseElement() throws MuleException, Exception
    {
        final MuleMessage result = muleContext.getClient().send("vm://response",
            new DefaultMuleMessage("", muleContext));

        assertNotNull(result);
        assertEquals("abcdefghi", result.getPayloadAsString());
    }

    public void testAsyncOneWayEndpoint() throws MuleException, Exception
    {
        muleContext.getClient().send("vm://async-oneway-in", new DefaultMuleMessage("0", muleContext));
        final MuleMessage result = muleContext.getClient().request("vm://async-oneway-out", RECEIVE_TIMEOUT);
        final MuleMessage asyncResult = muleContext.getClient().request("vm://async-async-oneway-out",
            RECEIVE_TIMEOUT);

        assertNotNull(result);
        assertNotNull(asyncResult);
        assertEquals("0ac", result.getPayloadAsString());
        assertEquals("0ab", asyncResult.getPayloadAsString());
    }

    public void testAsyncRequestResponseEndpoint() throws MuleException, Exception
    {
        muleContext.getClient().send("vm://async-requestresponse-in",
            new DefaultMuleMessage("0", muleContext));
        final MuleMessage result = muleContext.getClient().request("vm://async-requestresponse-out",
            RECEIVE_TIMEOUT);
        final MuleMessage asyncResult = muleContext.getClient().request(
            "vm://async-async-requestresponse-out", RECEIVE_TIMEOUT);

        assertNotNull(result);
        assertNotNull(asyncResult);
        assertEquals("0ac", result.getPayloadAsString());
        assertEquals("0ab", asyncResult.getPayloadAsString());
    }

    public void testAsyncTransactionalEndpoint() throws MuleException, Exception
    {
        try
        {
            muleContext.getClient().send("vm://async-tx-in", new DefaultMuleMessage("0", muleContext));
            fail("Exception expected");
        }
        catch (DispatchException e)
        {
            // expected
        }

        final MuleMessage result = muleContext.getClient().request("vm://async-requestresponse-out",
            RECEIVE_TIMEOUT);
        final MuleMessage asyncResult = muleContext.getClient().request("vm://async-async-oneway-out",
            RECEIVE_TIMEOUT);

        assertNull(result);
        assertNull(asyncResult);
    }

    // public void testTransactional() throws MuleException, Exception
    // {
    // muleContext.getClient().dispatch("vm://transactional-in", new
    // DefaultMuleMessage("", muleContext));
    //
    // }
    //
    // public void testTransactionalRollback() throws MuleException, Exception
    // {
    // muleContext.getClient().dispatch("vm://transactional-rollback-in",
    // new DefaultMuleMessage("", muleContext));
    //
    // }

    public void testMulticaster() throws MuleException, Exception
    {
        muleContext.getClient()
            .send("vm://multicaster-in", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        final MuleMessage result1 = muleContext.getClient().request("vm://multicaster-out1", RECEIVE_TIMEOUT);
        final MuleMessage result2 = muleContext.getClient().request("vm://multicaster-out2", RECEIVE_TIMEOUT);
        final MuleMessage result3 = muleContext.getClient().request("vm://multicaster-out3", RECEIVE_TIMEOUT);

        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertNotSame(result1, result2);
        assertNotSame(result1, result3);
        assertNotSame(result2, result3);

        assertEquals(TEST_MESSAGE, result1.getPayload());
        assertEquals(TEST_MESSAGE, result1.getPayload());
        assertEquals(TEST_MESSAGE, result1.getPayload());

    }

    public void testRecipientList() throws MuleException, Exception
    {
        muleContext.getClient().send("vm://recipient-list-in",
            new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        final MuleMessage result1 = muleContext.getClient().request("vm://recipient-list-out1",
            RECEIVE_TIMEOUT);
        final MuleMessage result2 = muleContext.getClient().request("vm://recipient-list-out2",
            RECEIVE_TIMEOUT);
        final MuleMessage result3 = muleContext.getClient().request("vm://recipient-list-out3",
            RECEIVE_TIMEOUT);

        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertNotSame(result1, result2);
        assertNotSame(result1, result3);
        assertNotSame(result2, result3);

        assertEquals(TEST_MESSAGE, result1.getPayload());
        assertEquals(TEST_MESSAGE, result2.getPayload());
        assertEquals(TEST_MESSAGE, result3.getPayload());

    }

    public void testChoiceWithoutOutboundEndpoints() throws MuleException, Exception
    {
        assertEquals("foo Hello foo", muleContext.getClient().send("vm://choice2-in",
            new DefaultMuleMessage("foo", muleContext)).getPayloadAsString());
        assertEquals("bar Hello bar", muleContext.getClient().send("vm://choice2-in",
            new DefaultMuleMessage("bar", muleContext)).getPayloadAsString());
        assertEquals("egh Hello ?", muleContext.getClient().send("vm://choice2-in",
            new DefaultMuleMessage("egh", muleContext)).getPayloadAsString());
    }

    public void testFlowRef() throws MuleException, Exception
    {
        assertEquals("012xyzabc3", muleContext.getClient().send("vm://flow-ref-in",
            new DefaultMuleMessage("0", muleContext)).getPayloadAsString());

    }

    public void testInvoke() throws MuleException, Exception
    {
        assertEquals("0recieved", muleContext.getClient().send("vm://invoke-in",
            new DefaultMuleMessage("0", muleContext)).getPayloadAsString());
    }

    public void testInvoke2() throws MuleException, Exception
    {
        MuleMessage message = new DefaultMuleMessage("0", muleContext);
        message.setOutboundProperty("one", "header1val");
        assertEquals("header1valrecieved", muleContext.getClient()
            .send("vm://invoke2-in", message)
            .getPayloadAsString());

    }

    public void testInvoke3() throws MuleException, Exception
    {
        // ensure multiple arguments work
        muleContext.getClient().send("vm://invoke3-in", new DefaultMuleMessage("0", muleContext));
    }

    public void testInvoke4() throws MuleException, Exception
    {
        // ensure no arguments work
        muleContext.getClient().send("vm://invoke4-in", new DefaultMuleMessage("0", muleContext));
    }
   
    public void testEnrichWithAttributes() throws MuleException, Exception
    {
        MuleMessage message = new DefaultMuleMessage("0", muleContext);
        assertEquals("0Hello", muleContext.getClient().send("vm://enrich-in", message).getProperty(
            "helloHeader", PropertyScope.INBOUND));
    }

    public void testEnrichWithElements() throws MuleException, Exception
    {
        MuleMessage message = new DefaultMuleMessage("0", muleContext);
        MuleMessage result = muleContext.getClient().send("vm://enrich2-in", message);

        assertEquals("0Hello", result.getProperty("helloHeader", PropertyScope.INBOUND));
        assertEquals("0Hello", result.getProperty("helloHeader2", PropertyScope.INBOUND));
    }
    
    public void testLoggerMessage() throws MuleException, Exception
    {
        MuleMessage message = new DefaultMuleMessage("0", muleContext);
        muleContext.getClient().send("vm://loggermessage-in", message);
    }
    
    public void testLoggerHeader() throws MuleException, Exception
    {
        MuleMessage message = new DefaultMuleMessage("0", muleContext);
        message.setOutboundProperty("toLog", "valueToLog");
        muleContext.getClient().send("vm://loggerheader-in", message);
    }

    public static class Pojo
    {

        public void method()
        {

        }
        
        public void method(Object arg1, Object arg2)
        {

        }
    }

    public void testFlowThreadingProfile() throws MuleException, Exception
    {
        Flow flow = muleContext.getRegistry().lookupObject("flow-threading-profile");
        assertTrue(flow.getThreadingProfile().isDoThreading());
        assertEquals(2, flow.getThreadingProfile().getMaxThreadsActive());
        assertEquals(1, flow.getThreadingProfile().getMaxThreadsIdle());
        assertEquals(ThreadingProfile.WHEN_EXHAUSTED_RUN, flow.getThreadingProfile().getPoolExhaustedAction());
    }
    
    public void testCustomMessageRouter() throws MuleException, Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleMessage result = muleContext.getClient().send("vm://customRouter-in", message);
        assertEquals("abc", result.getPayloadAsString());
    }
    
    public void testPoll() throws Exception
    {
        MuleMessage message = muleContext.getClient().request("vm://poll-out", RECEIVE_TIMEOUT);
        assertNotNull(message);
        assertEquals(" Hello fooout", message.getPayloadAsString());
    }

    public void testPollFlowRef() throws Exception
    {
        MuleMessage message = muleContext.getClient().request("vm://poll2-out", RECEIVE_TIMEOUT);
        assertNotNull(message);
        assertEquals("pollappendout", message.getPayloadAsString());
    }
    
    public static class ThreadSensingMessageProcessor implements MessageProcessor
    {
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            event.getMessage().setPayload(Thread.currentThread());
            return event;
        }
    }

}
