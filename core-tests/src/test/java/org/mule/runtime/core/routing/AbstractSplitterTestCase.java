/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.message.Message.of;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Acceptor;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AbstractSplitterTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Test
  public void simpleSplitter() throws Exception {
    TestSplitter splitter = new TestSplitter(false);
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

    final Event inEvent = eventBuilder().message(of(fruitBowl)).build();

    Event resultEvent = splitter.process(inEvent);

    assertThat(listener.events, hasSize(3));
    assertThat(listener.events.get(0).getMessage().getPayload().getValue(), instanceOf(Fruit.class));
    assertThat(listener.events.get(1).getMessage().getPayload().getValue(), instanceOf(Fruit.class));
    assertThat(listener.events.get(2).getMessage().getPayload().getValue(), instanceOf(Fruit.class));

    assertThat(resultEvent.getMessage().getPayload().getValue(), instanceOf(List.class));
    assertThat(((List<InternalMessage>) resultEvent.getMessage().getPayload().getValue()), hasSize(3));
    assertThat(((List<InternalMessage>) resultEvent.getMessage().getPayload().getValue()).get(0).getPayload()
        .getValue(), instanceOf(Fruit.class));
    assertThat(((List<InternalMessage>) resultEvent.getMessage().getPayload().getValue()).get(1).getPayload()
        .getValue(), instanceOf(Fruit.class));
    assertThat(((List<InternalMessage>) resultEvent.getMessage().getPayload().getValue()).get(2).getPayload()
        .getValue(), instanceOf(Fruit.class));
  }

  @Test
  public void allFilteredSplitter() throws Exception {
    TestSplitter splitter = new TestSplitter(true);
    splitter.setListener(event -> {
      throw new MessagingException(createStaticMessage("Expected"), event, splitter);
    });
    splitter.setMuleContext(muleContext);

    Apple apple = new Apple();
    Banana banana = new Banana();
    Orange orange = new Orange();
    FruitBowl fruitBowl = new FruitBowl();
    fruitBowl.addFruit(apple);
    fruitBowl.addFruit(banana);
    fruitBowl.addFruit(orange);

    final Event inEvent = eventBuilder().message(of(fruitBowl)).build();

    Event resultEvent = splitter.process(inEvent);

    assertThat(resultEvent.getMessage().getPayload().getValue(), nullValue());
  }

  @Test
  public void failingNotFilteredSplitter() throws Exception {
    TestSplitter splitter = new TestSplitter(false);
    splitter.setListener(event -> {
      throw new MessagingException(createStaticMessage("Expected"), event, splitter);
    });
    splitter.setMuleContext(muleContext);

    Apple apple = new Apple();
    Banana banana = new Banana();
    Orange orange = new Orange();
    FruitBowl fruitBowl = new FruitBowl();
    fruitBowl.addFruit(apple);
    fruitBowl.addFruit(banana);
    fruitBowl.addFruit(orange);

    final Event inEvent = eventBuilder().message(of(fruitBowl)).build();

    expected.expect(MessagingException.class);
    expected.expectMessage("Expected");
    splitter.process(inEvent);
  }

  private static class MultipleEventSensingMessageProcessor implements Processor {

    List<Event> events = new ArrayList<>();

    @Override
    public Event process(Event event) throws MuleException {
      events.add(event);
      return event;
    }
  }

  private static class TestSplitter extends AbstractSplitter {

    public TestSplitter(boolean filtersErrors) {
      filterOnErrorTypeAcceptor = new Acceptor() {

        @Override
        public boolean acceptsAll() {
          return filtersErrors;
        }

        @Override
        public boolean accept(Event event) {
          return filtersErrors;
        }
      };
    }

    @Override
    protected List<Event> splitMessage(Event event) {
      FruitBowl bowl = (FruitBowl) event.getMessage().getPayload().getValue();
      List<Event> parts = new ArrayList<>();
      for (Fruit fruit : bowl.getFruit()) {
        parts.add(Event.builder(event).message(of(fruit)).build());
      }
      return parts;
    }
  }

}
