/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.routing.filters.AcceptAllFilter;
import org.mule.runtime.core.routing.filters.logic.NotFilter;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class AbstractSplitterTestCase extends AbstractMuleContextTestCase {

  @Test
  public void simpleSplitter() throws Exception {
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

    MuleEvent inEvent = MuleEvent.builder(getTestEvent("")).message(MuleMessage.builder().payload(fruitBowl).build()).build();

    MuleEvent resultEvent = splitter.process(inEvent);

    assertEquals(3, listener.events.size());
    assertTrue(listener.events.get(0).getMessage().getPayload() instanceof Fruit);
    assertTrue(listener.events.get(1).getMessage().getPayload() instanceof Fruit);
    assertTrue(listener.events.get(2).getMessage().getPayload() instanceof Fruit);

    assertThat(resultEvent.getMessage().getPayload(), instanceOf(List.class));
    assertEquals(3, ((List<MuleMessage>) resultEvent.getMessage().getPayload()).size());
    assertTrue(((List<MuleMessage>) resultEvent.getMessage().getPayload()).get(0).getPayload() instanceof Fruit);
    assertTrue(((List<MuleMessage>) resultEvent.getMessage().getPayload()).get(1).getPayload() instanceof Fruit);
    assertTrue(((List<MuleMessage>) resultEvent.getMessage().getPayload()).get(2).getPayload() instanceof Fruit);
  }

  @Test
  public void allFilteredSplitter() throws Exception {
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

    MuleEvent inEvent = MuleEvent.builder(getTestEvent("")).message(MuleMessage.builder().payload(fruitBowl).build()).build();

    MuleEvent resultEvent = splitter.process(inEvent);

    assertThat(resultEvent, nullValue());
  }

  private static class MultipleEventSensingMessageProcessor implements MessageProcessor {

    List<MuleEvent> events = new ArrayList<>();

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
      events.add(event);
      return event;
    }
  }

  private static class TestSplitter extends AbstractSplitter {

    @Override
    protected List<MuleEvent> splitMessage(MuleEvent event) {
      FruitBowl bowl = (FruitBowl) event.getMessage().getPayload();
      List<MuleEvent> parts = new ArrayList<>();
      for (Fruit fruit : bowl.getFruit()) {
        parts.add(MuleEvent.builder(event).message(MuleMessage.builder().payload(fruit).build()).build());
      }
      return parts;
    }
  }

}
