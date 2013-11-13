/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.CompositeMessageSource;
import org.mule.api.source.MessageSource;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.PropertyScope;
import org.mule.construct.Flow;
import org.mule.endpoint.DefaultInboundEndpoint;
import org.mule.source.StartableCompositeMessageSource;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transformer.simple.StringAppendTransformer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

public class FlowConfigurationFunctionalTestCase extends FunctionalTestCase
{
    public FlowConfigurationFunctionalTestCase()
    {
        setDisposeContextPerClass(true);
    }

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/construct/flow.xml";
    }

    @Test
    public void testFlow() throws Exception
    {
        final Flow flow = muleContext.getRegistry().lookupObject("flow");
        assertEquals(DefaultInboundEndpoint.class, flow.getMessageSource().getClass());
        assertEquals("vm://in", ((InboundEndpoint) flow.getMessageSource()).getEndpointURI()
            .getUri()
            .toString());
        assertEquals(5, flow.getMessageProcessors().size());
        assertNotNull(flow.getExceptionListener());

        assertEquals("012xyzabc3",
            muleContext.getClient()
                .send("vm://in", new DefaultMuleMessage("0", muleContext))
                .getPayloadAsString());

    }

    @Test
    public void testFlowSynchronous() throws MuleException
    {
        muleContext.getClient().send("vm://synchronous", new DefaultMuleMessage("0", muleContext));
        MuleMessage message = muleContext.getClient().request("vm://synchronous-out", RECEIVE_TIMEOUT);
        assertNotNull(message);
        Thread thread = (Thread) message.getPayload();
        assertNotNull(thread);
        assertEquals(Thread.currentThread(), thread);
    }

    @Test
    public void testFlowAynchronous() throws MuleException
    {
        muleContext.getClient().send("vm://asynchronous", new DefaultMuleMessage("0", muleContext));
        MuleMessage message = muleContext.getClient().request("vm://asynchronous-out", RECEIVE_TIMEOUT);
        assertNotNull(message);
        Thread thread = (Thread) message.getPayload();
        assertNotNull(thread);
        assertNotSame(Thread.currentThread(), thread);
    }

    @Test
    public void testFlowQueuedAsynchronous() throws MuleException
    {
        muleContext.getClient().send("vm://queued-asynchronous", new DefaultMuleMessage("0", muleContext));
        MuleMessage message = muleContext.getClient()
            .request("vm://queued-asynchronous-out", RECEIVE_TIMEOUT);
        assertNotNull(message);
        Thread thread = (Thread) message.getPayload();
        assertNotNull(thread);
        assertNotSame(Thread.currentThread(), thread);
    }

    @Test
    public void testAsyncAynchronous() throws MuleException
    {
        muleContext.getClient().dispatch("vm://asynchronous-async", new DefaultMuleMessage("0", muleContext));
        MuleMessage message = muleContext.getClient().request("vm://asynchronous-async-out", RECEIVE_TIMEOUT);
        assertNotNull(message);
        Thread thread = (Thread) message.getPayload();
        assertNotNull(thread);
        assertNotSame(Thread.currentThread(), thread);
    }

    @Test
    public void testAsyncQueuedAsynchronous() throws MuleException
    {
        muleContext.getClient().dispatch("vm://queued-asynchronous-async",
            new DefaultMuleMessage("0", muleContext));
        MuleMessage message = muleContext.getClient().request("vm://queued-asynchronous-async-out",
            RECEIVE_TIMEOUT);
        assertNotNull(message);
        Thread thread = (Thread) message.getPayload();
        assertNotNull(thread);
        assertNotSame(Thread.currentThread(), thread);
    }

    @Test
    public void testFlowCompositeSource() throws Exception
    {
        final Flow flow = muleContext.getRegistry().lookupObject("flow2");
        assertEquals(StartableCompositeMessageSource.class, flow.getMessageSource().getClass());
        assertEquals(2, flow.getMessageProcessors().size());

        assertEquals("01xyz",
            muleContext.getClient()
                .send("vm://in2", new DefaultMuleMessage("0", muleContext))
                .getPayloadAsString());
        assertEquals("01xyz",
            muleContext.getClient()
                .send("vm://in3", new DefaultMuleMessage("0", muleContext))
                .getPayloadAsString());
    }

    @Test
    public void testInOutFlow() throws Exception
    {
        muleContext.getClient().send("vm://inout-in", new DefaultMuleMessage("0", muleContext));
        assertEquals("0", muleContext.getClient()
            .request("vm://inout-out", RECEIVE_TIMEOUT)
            .getPayloadAsString());
    }

    @Test
    public void testInOutAppendFlow() throws Exception
    {
        muleContext.getClient().send("vm://inout-append-in", new DefaultMuleMessage("0", muleContext));
        assertEquals("0inout", muleContext.getClient()
            .request("vm://inout-append-out", RECEIVE_TIMEOUT)
            .getPayloadAsString());
    }

    @Test
    public void testSplitAggregateFlow() throws Exception
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

    @Test
    public void testSplitNoParts() throws Exception
    {
        String MESSAGE = "<Order></Order>";
        MuleMessage result = muleContext.getClient().send("vm://split-no-parts-in", MESSAGE, null);

        assertNotNull(result);
        assertEquals(result.getPayload(), MESSAGE);
    }

    @Test
    public void testSplitAggregateListFlow() throws Exception
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

    @Test
    public void testSplitAggregateListFlowSingleItem() throws Exception
    {
        final Apple apple = new Apple();
        final FruitBowl fruitBowl = new FruitBowl();
        fruitBowl.addFruit(apple);

        muleContext.getClient().send("vm://split-aggregate-singleton-list-in",
            new DefaultMuleMessage(fruitBowl.getFruit(), muleContext));

        final MuleMessage result = muleContext.getClient().request("vm://split-aggregate-singleton-list-out",
            RECEIVE_TIMEOUT);

        assertNotNull(result);
        assertTrue(result instanceof MuleMessageCollection);
        final MuleMessageCollection coll = (MuleMessageCollection) result;
        assertEquals(1, coll.size());
        final List<?> results = (List<?>) coll.getPayload();

        assertTrue(apple.isBitten());

        assertTrue(results.contains(apple));
    }
    
    @Test
    public void testSplitAggregateResponseListFlow() throws Exception
    {
        final Apple apple = new Apple();
        final Banana banana = new Banana();
        final Orange orange = new Orange();
        final FruitBowl fruitBowl = new FruitBowl(apple, banana);
        fruitBowl.addFruit(orange);

        final MuleMessage result  = muleContext.getClient().send("vm://split-aggregate-response-list-in",
            new DefaultMuleMessage(fruitBowl.getFruit(), muleContext));

        assertNotNull(result);
        assertTrue(result instanceof MuleMessageCollection);
        final MuleMessageCollection coll = (MuleMessageCollection) result;
        assertEquals(3, coll.size());
        final List<?> results = (List<?>) coll.getPayload();
        assertEquals(3, results.size());

        assertTrue(apple.isBitten());
        assertTrue(banana.isBitten());
        assertTrue(orange.isBitten());

        assertTrue(results.contains(apple));
        assertTrue(results.contains(banana));
        assertTrue(results.contains(orange));
    }

    @Test
    public void testSplitAggregateResponseListFlowSingleItem() throws Exception
    {
        final Apple apple = new Apple();
        final FruitBowl fruitBowl = new FruitBowl();
        fruitBowl.addFruit(apple);

        final MuleMessage result  = muleContext.getClient().send("vm://split-aggregate-response-singleton-list-in",
            new DefaultMuleMessage(fruitBowl.getFruit(), muleContext));

        assertNotNull(result);
        assertTrue(result instanceof MuleMessageCollection);
        final MuleMessageCollection coll = (MuleMessageCollection) result;
        assertEquals(1, coll.size());
        final List<?> results = (List<?>) coll.getPayload();
        assertEquals(1, results.size());

        assertTrue(apple.isBitten());
        assertTrue(results.contains(apple));
    }

    @Test
    public void testSplitAggregateMapFlow() throws Exception
    {
        Map<String, Fruit> map = new HashMap<String, Fruit>();
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

    @Test
    public void testSplitFilterAggregateFlow() throws Exception
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

    @Test
    public void testMessageChunkSplitAggregateFlow() throws Exception
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

    @Test
    public void testComponentsFlow() throws Exception
    {
        final MuleMessage result = muleContext.getClient().send("vm://components",
            new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertNotNull(result);
        assertNotSame(TEST_MESSAGE + "test", result.getPayload());
    }

    @Test
    public void testWireTapFlow() throws Exception
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

    @Test
    public void testResponseElement() throws Exception
    {
        final MuleMessage result = muleContext.getClient().send("vm://response",
            new DefaultMuleMessage("", muleContext));

        assertNotNull(result);
        assertEquals("abcdefghi", result.getPayloadAsString());
    }

    @Test
    public void testAsyncOneWayEndpoint() throws Exception
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

    @Test
    public void testAsyncSedaOneWayEndpoint() throws Exception
    {
        muleContext.getClient().send("vm://async-seda-oneway-in", new DefaultMuleMessage("0", muleContext));
        final MuleMessage result = muleContext.getClient().request("vm://async-seda-oneway-out",
            RECEIVE_TIMEOUT);
        final MuleMessage asyncResult = muleContext.getClient().request("vm://async-async-seda-oneway-out",
            RECEIVE_TIMEOUT);

        assertNotNull(result);
        assertNotNull(asyncResult);
        assertEquals("0ac", result.getPayloadAsString());
        assertEquals("0ab", asyncResult.getPayloadAsString());
    }

    @Test
    public void testAsyncRequestResponseEndpoint() throws Exception
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

    @Test
    public void testAsyncTransactionalEndpoint() throws Exception
    {
        MuleMessage message = muleContext.getClient().send("vm://async-tx-in",
            new DefaultMuleMessage("0", muleContext));
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertEquals(MessagingException.class, message.getExceptionPayload().getException().getClass());

        final MuleMessage result = muleContext.getClient().request("vm://async-requestresponse-out",
            RECEIVE_TIMEOUT);
        final MuleMessage asyncResult = muleContext.getClient().request("vm://async-async-oneway-out",
            RECEIVE_TIMEOUT);

        assertNull(result);
        assertNull(asyncResult);
    }

    @Ignore
    @Test
    public void testTransactional() throws Exception
    {
        muleContext.getClient().dispatch("vm://transactional-in", new DefaultMuleMessage("", muleContext));
    }

    @Ignore
    @Test
    public void testTransactionalRollback() throws Exception
    {
        muleContext.getClient().dispatch("vm://transactional-rollback-in",
            new DefaultMuleMessage("", muleContext));
    }

    @Test
    public void testMulticaster() throws Exception
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

    @Test
    public void testRecipientList() throws Exception
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

    @Test
    public void testChoiceWithoutOutboundEndpoints() throws Exception
    {
        assertEquals("foo Hello foo",
            muleContext.getClient()
                .send("vm://choice2-in", new DefaultMuleMessage("foo", muleContext))
                .getPayloadAsString());
        assertEquals("bar Hello bar",
            muleContext.getClient()
                .send("vm://choice2-in", new DefaultMuleMessage("bar", muleContext))
                .getPayloadAsString());
        assertEquals("egh Hello ?",
            muleContext.getClient()
                .send("vm://choice2-in", new DefaultMuleMessage("egh", muleContext))
                .getPayloadAsString());
    }

    @Test
    public void testFlowRef() throws Exception
    {
        assertEquals("012xyzabc312xyzabc3",
            muleContext.getClient()
                .send("vm://flow-ref-in", new DefaultMuleMessage("0", muleContext))
                .getPayloadAsString());
    }

    @Test
    public void testInvoke() throws Exception
    {
        assertEquals("0recieved",
            muleContext.getClient()
                .send("vm://invoke-in", new DefaultMuleMessage("0", muleContext))
                .getPayloadAsString());
    }

    @Test
    public void testInvoke2() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("0", muleContext);
        message.setOutboundProperty("one", "header1val");
        assertEquals("header1valrecieved", muleContext.getClient()
            .send("vm://invoke2-in", message)
            .getPayloadAsString());
    }

    @Test
    public void testInvoke3() throws Exception
    {
        // ensure multiple arguments work
        muleContext.getClient().send("vm://invoke3-in", new DefaultMuleMessage("0", muleContext));
    }

    @Test
    public void testInvoke4() throws Exception
    {
        // ensure no arguments work
        muleContext.getClient().send("vm://invoke4-in", new DefaultMuleMessage("0", muleContext));
    }

    @Test
    public void testEnrichWithAttributes() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("0", muleContext);
        assertEquals(
            "0Hello",
            muleContext.getClient()
                .send("vm://enrich-in", message)
                .getProperty("helloHeader", PropertyScope.INBOUND));
    }

    @Test
    public void testEnrichWithElements() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("0", muleContext);
        MuleMessage result = muleContext.getClient().send("vm://enrich2-in", message);

        assertEquals("0Hello", result.getProperty("helloHeader", PropertyScope.INBOUND));
        assertEquals("0Hello", result.getProperty("helloHeader2", PropertyScope.INBOUND));
    }

    @Test
    public void testEnrichUsingComponent() throws Exception
    {
        // MULE-5544
        MuleMessage message = new DefaultMuleMessage("0", muleContext);
        MuleMessage result = muleContext.getClient().send("vm://enrichcomponent-in", message);

        assertEquals("0", result.getProperty("echoHeader", PropertyScope.INBOUND));
    }

    @Test
    public void testEnrichUsingComponent2() throws Exception
    {
        // MULE-5544
        MuleMessage message = new DefaultMuleMessage("0", muleContext);
        MuleMessage result = muleContext.getClient().send("vm://enrichcomponent2-in", message);

        assertEquals("0", result.getProperty("echoHeader", PropertyScope.INBOUND));
    }

    @Test
    public void testLoggerMessage() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("0", muleContext);
        muleContext.getClient().send("vm://loggermessage-in", message);
    }

    @Test
    public void testLoggerHeader() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("0", muleContext);
        message.setOutboundProperty("toLog", "valueToLog");
        muleContext.getClient().send("vm://loggerheader-in", message);
    }

    public static class Pojo
    {
        public void method()
        {
            // does nothing
        }

        public void method(Object arg1, Object arg2)
        {
            // does nothing
        }
    }

    @Test
    public void testCustomMessageRouter() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleMessage result = muleContext.getClient().send("vm://customRouter-in", message);
        assertEquals("abc", result.getPayloadAsString());
    }

    @Test
    public void testPoll() throws Exception
    {
        MuleMessage message = muleContext.getClient().request("vm://poll-out", RECEIVE_TIMEOUT);
        assertNotNull(message);
        assertEquals(" Hello fooout", message.getPayloadAsString());
    }

    @Test
    public void testPollFlowRef() throws Exception
    {
        MuleMessage message = muleContext.getClient().request("vm://poll2-out", RECEIVE_TIMEOUT);
        assertNotNull(message);
        assertEquals("pollappendout", message.getPayloadAsString());
    }

    @Test
    public void testSubFlowMessageFilter() throws Exception
    {
        muleContext.getClient().dispatch("vm://messagefiltersubflow-in",
            new DefaultMuleMessage("0", muleContext));
        MuleMessage message = muleContext.getClient().request("vm://messagefiltersubflow-out", 5000);
        assertNotNull(message);
    }

    @Test
    public void testCustomMessageSource() throws Exception
    {
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("customMessageSource");
        TestMessageSource source = (TestMessageSource) flow.getMessageSource();

        MuleEvent result = source.fireEvent(getTestEvent("a"));
        assertEquals("abcd", result.getMessageAsString());
    }

    @Test
    public void testCustomMessageSourceInComposite() throws Exception
    {
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("customMessageSourceInComposite");
        CompositeMessageSource compositeSource = (CompositeMessageSource) flow.getMessageSource();
        TestMessageSource source = (TestMessageSource) compositeSource.getSources().get(0);

        MuleEvent result = source.fireEvent(getTestEvent("a"));
        assertEquals("abcd", result.getMessageAsString());
    }

    
    public static class TestMessageSource implements MessageSource
    {
        private MessageProcessor listener;
        private String appendBefore;
        private String appendAfter;
       
        MuleEvent fireEvent(MuleEvent event) throws MuleException
        {
            Transformer before = new StringAppendTransformer(appendBefore);
            Transformer after = new StringAppendTransformer(appendAfter);
            return after.process(listener.process(before.process(event)));
        }

        public void setAppendBefore(String appendBefore)
        {
            this.appendBefore = appendBefore;
        }

        public void setAppendAfter(String appendAfter)
        {
            this.appendAfter = appendAfter;
        }

        @Override
        public void setListener(MessageProcessor listener)
        {
            this.listener = listener;
        }

    }

    public static class ThreadSensingMessageProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            event.getMessage().setPayload(Thread.currentThread());
            return event;
        }
    }

}
