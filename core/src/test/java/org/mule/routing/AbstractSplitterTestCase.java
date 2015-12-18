/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMessageCollection;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.processor.MessageProcessor;
import org.mule.routing.filters.AcceptAllFilter;
import org.mule.routing.filters.logic.NotFilter;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class AbstractSplitterTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void simpleSplitter() throws Exception
    {
        TestSplitter splitter = new TestSplitter();
        MultipleEventSensingMessageProcessor listener = new MultipleEventSensingMessageProcessor();
        splitter.setListener(listener);
        splitter.setMuleContext(muleContext);

        Apple apple = new Apple();
        Banana banana = new Banana();
        Orange orange = new Orange();
        FruitBowl fruitBowl = new FruitBowl();
        fruitBowl.addFruit(apple);
        fruitBowl.addFruit(banana);
        fruitBowl.addFruit(orange);

        MuleEvent inEvent = new DefaultMuleEvent(new DefaultMuleMessage(fruitBowl, muleContext),
            getTestEvent(""));

        MuleEvent resultEvent = splitter.process(inEvent);

        assertEquals(3, listener.events.size());
        assertTrue(listener.events.get(0).getMessage().getPayload() instanceof Fruit);
        assertTrue(listener.events.get(1).getMessage().getPayload() instanceof Fruit);
        assertTrue(listener.events.get(2).getMessage().getPayload() instanceof Fruit);

        assertEquals(DefaultMessageCollection.class, resultEvent.getMessage().getClass());
        assertEquals(3, ((MuleMessageCollection) resultEvent.getMessage()).size());
        assertTrue(((MuleMessageCollection) resultEvent.getMessage()).getMessage(0).getPayload() instanceof Fruit);
        assertTrue(((MuleMessageCollection) resultEvent.getMessage()).getMessage(1).getPayload() instanceof Fruit);
        assertTrue(((MuleMessageCollection) resultEvent.getMessage()).getMessage(2).getPayload() instanceof Fruit);
    }

    @Test
    public void allFilteredSplitter() throws Exception
    {
        TestSplitter splitter = new TestSplitter();
        splitter.setListener(new MessageFilter(new NotFilter(new AcceptAllFilter())));
        splitter.setMuleContext(muleContext);

        Apple apple = new Apple();
        Banana banana = new Banana();
        Orange orange = new Orange();
        FruitBowl fruitBowl = new FruitBowl();
        fruitBowl.addFruit(apple);
        fruitBowl.addFruit(banana);
        fruitBowl.addFruit(orange);

        MuleEvent inEvent = new DefaultMuleEvent(new DefaultMuleMessage(fruitBowl, muleContext),
                getTestEvent(""));

        MuleEvent resultEvent = splitter.process(inEvent);

        assertThat(resultEvent, nullValue());
    }

    private static class MultipleEventSensingMessageProcessor implements MessageProcessor
    {
        List<MuleEvent> events = new ArrayList<MuleEvent>();

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            events.add(event);
            return event;
        }
    }

    private static class TestSplitter extends AbstractSplitter
    {
        @Override
        protected List<MuleMessage> splitMessage(MuleEvent event)
        {
            FruitBowl bowl = (FruitBowl) event.getMessage().getPayload();
            List<MuleMessage> parts = new ArrayList<MuleMessage>();
            for (Fruit fruit : bowl.getFruit())
            {
                parts.add(new DefaultMuleMessage(fruit, muleContext));
            }
            return parts;
        }
    }

}
