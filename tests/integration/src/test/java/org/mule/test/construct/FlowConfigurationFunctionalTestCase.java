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
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.construct.SimpleFlowConstruct;
import org.mule.endpoint.DefaultInboundEndpoint;
import org.mule.source.StartableCompositeMessageSource;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.List;

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
        final SimpleFlowConstruct flow = muleContext.getRegistry().lookupObject("flow");
        assertEquals(DefaultInboundEndpoint.class, flow.getMessageSource().getClass());
        assertEquals("vm://in", ((InboundEndpoint) flow.getMessageSource()).getEndpointURI()
            .getUri()
            .toString());
        assertEquals(5, flow.getMessageProcessors().size());
        assertNotNull(flow.getExceptionListener());

        assertEquals("012xyzabc3", muleContext.getClient().send("vm://in",
            new DefaultMuleMessage("0", muleContext)).getPayloadAsString());

    }

    public void testFlowCompositeSource() throws MuleException, Exception
    {
        final SimpleFlowConstruct flow = muleContext.getRegistry().lookupObject("flow2");
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

    public void testAsync() throws MuleException, Exception
    {
        muleContext.getClient().send("vm://async-in", new DefaultMuleMessage("0", muleContext));
        final MuleMessage result = muleContext.getClient().request("vm://async-out", RECEIVE_TIMEOUT);
        final MuleMessage asyncResult = muleContext.getClient().request("vm://async-async-out",
            RECEIVE_TIMEOUT);

        assertNotNull(result);
        assertNotNull(asyncResult);
        assertEquals("0ac", result.getPayloadAsString());
        assertEquals("0ab", asyncResult.getPayloadAsString());

    }

    public void testTransactional() throws MuleException, Exception
    {
        muleContext.getClient().dispatch("vm://transactional-in", new DefaultMuleMessage("", muleContext));

    }

    public void testTransactionalRollback() throws MuleException, Exception
    {
        muleContext.getClient().dispatch("vm://transactional-rollback-in",
            new DefaultMuleMessage("", muleContext));

    }

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
        assertEquals("foo Hello foo",muleContext.getClient().send("vm://choice2-in",
            new DefaultMuleMessage("foo", muleContext)).getPayloadAsString());
        assertEquals("bar Hello bar",muleContext.getClient().send("vm://choice2-in",
            new DefaultMuleMessage("bar", muleContext)).getPayloadAsString());
        assertEquals("egh Hello ?",muleContext.getClient().send("vm://choice2-in",
            new DefaultMuleMessage("egh", muleContext)).getPayloadAsString());
    }

    public void testFlowRef() throws MuleException, Exception
    {
        assertEquals("012xyzabc3", muleContext.getClient().send("vm://flow-ref-in",
            new DefaultMuleMessage("0", muleContext)).getPayloadAsString());

    }
    
}
