/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.api.message.Message.of;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.tck.junit4.AbstractReactiveProcessorTestCase;
import org.mule.tck.testmodels.mule.TestMessageProcessor;

import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ForeachTestCase extends AbstractReactiveProcessorTestCase {

  protected Foreach simpleForeach;
  protected Foreach nestedForeach;
  protected ArrayList<Event> processedEvents;

  private static String ERR_NUMBER_MESSAGES = "Not a correct number of messages processed";
  private static String ERR_PAYLOAD_TYPE = "Type error on processed payloads";
  private static String ERR_OUTPUT = "Messages processed incorrectly";

  @Rule
  public ExpectedException expectedException = none();

  public ForeachTestCase(Mode mode) {
    super(mode);
  }

  @Before
  public void initialise() throws MuleException {
    processedEvents = new ArrayList<>();
    simpleForeach = createForeach(getSimpleMessageProcessors(new TestMessageProcessor("zas")));
    nestedForeach = createForeach(getNestedMessageProcessors());
  }

  private List<Processor> getSimpleMessageProcessors(Processor innerProcessor) {
    List<Processor> lmp = new ArrayList<>();
    lmp.add(event -> {
      String payload = event.getMessage().getPayload().getValue().toString();
      event = Event.builder(event).message(InternalMessage.builder(event.getMessage()).value(payload + ":foo").build()).build();
      return event;
    });
    lmp.add(innerProcessor);
    lmp.add(event -> {
      processedEvents.add(event);
      return event;
    });
    return lmp;
  }

  private List<Processor> getNestedMessageProcessors() throws MuleException {
    List<Processor> lmp = new ArrayList<>();
    Foreach internalForeach = new Foreach();
    internalForeach.setMessageProcessors(getSimpleMessageProcessors(new TestMessageProcessor("zas")));
    lmp.add(internalForeach);
    return lmp;
  }

  private Foreach createForeach(List<Processor> mps) throws MuleException {
    Foreach foreachMp = new Foreach();
    foreachMp.setMessageProcessors(mps);
    foreachMp.setMuleContext(muleContext);
    foreachMp.initialise();
    return foreachMp;
  }

  @Test
  public void arrayListPayload() throws Exception {
    List<String> arrayList = new ArrayList<>();
    arrayList.add("bar");
    arrayList.add("zip");
    process(simpleForeach, eventBuilder().message(of(arrayList)).build());

    assertSimpleProcessedMessages();
  }

  @Test
  public void arrayPayload() throws Exception {
    String[] array = new String[2];
    array[0] = "bar";
    array[1] = "zip";
    process(simpleForeach, eventBuilder().message(of(array)).build());

    assertSimpleProcessedMessages();
  }

  @Test
  public void muleMessageCollectionPayload() throws Exception {
    List<Message> list = new ArrayList<>();
    list.add(of("bar"));
    list.add(of("zip"));
    process(simpleForeach, eventBuilder().message(of(list)).build());

    assertSimpleProcessedMessages();
  }

  @Test
  public void iterablePayload() throws Exception {
    Iterable<String> iterable = new DummySimpleIterableClass();
    final Event testEvent = eventBuilder().message(of(iterable)).build();
    process(simpleForeach, testEvent);

    assertSimpleProcessedMessages();
  }

  @Test
  public void iteratorPayload() throws Exception {
    Iterable<String> iterable = new DummySimpleIterableClass();
    process(simpleForeach, eventBuilder().message(of(iterable.iterator())).build());

    assertSimpleProcessedMessages();
  }

  @Test
  public void nestedArrayListPayload() throws Exception {
    List<List<String>> payload = new ArrayList<>();
    List<String> elem1 = new ArrayList<>();
    List<String> elem2 = new ArrayList<>();
    List<String> elem3 = new ArrayList<>();
    elem1.add("a1");
    elem1.add("a2");
    elem1.add("a3");
    elem2.add("b1");
    elem2.add("b2");
    elem3.add("c1");
    payload.add(elem1);
    payload.add(elem2);
    payload.add(elem3);

    process(nestedForeach, eventBuilder().message(of(payload)).build());

    assertNestedProcessedMessages();
  }

  @Test
  public void nestedArrayPayload() throws Exception {
    String[][] payload = new String[3][2];
    payload[0][0] = "a1";
    payload[0][1] = "a2";
    payload[1][0] = "a3";
    payload[1][1] = "b1";
    payload[2][0] = "b2";
    payload[2][1] = "c1";

    process(nestedForeach, eventBuilder().message(of(payload)).build());
    assertNestedProcessedMessages();
  }

  @Test
  public void nestedMuleMessageCollectionPayload() throws Exception {

    List<Message> parentList = new ArrayList<>();
    List<Message> list1 = new ArrayList<>();
    List<Message> list2 = new ArrayList<>();

    list1.add(of("a1"));
    list1.add(of("a2"));
    list1.add(of("a3"));

    list2.add(of("b1"));
    list2.add(of("b2"));
    list2.add(of("c1"));

    parentList.add(of(list1));
    parentList.add(of(list2));
    Message parentCollection = of(parentList);

    process(nestedForeach, eventBuilder().message(parentCollection).build());

    assertNestedProcessedMessages();
  }

  @Test
  public void nestedIterablePayload() throws Exception {
    Iterable<DummySimpleIterableClass> iterable = new DummyNestedIterableClass();

    process(nestedForeach, eventBuilder().message(of(iterable)).build());
    assertNestedProcessedMessages();
  }

  @Test
  public void nestedIteratorPayload() throws Exception {
    Iterable<DummySimpleIterableClass> iterable = new DummyNestedIterableClass();

    process(nestedForeach, eventBuilder().message(of(iterable)).build());
    assertNestedProcessedMessages();
  }

  @Test
  public void failingNestedProcessor() throws Exception {
    RuntimeException throwable = new BufferOverflowException();

    Foreach foreach = new Foreach();
    foreach.setMuleContext(muleContext);
    Processor failingProcessor = event -> {
      throw throwable;
    };
    foreach.setMessageProcessors(singletonList(failingProcessor));
    foreach.initialise();

    expectedException.expect(is(MessagingException.class));
    expectedException.expect(new FailingProcessorMatcher(failingProcessor));
    expectedException.expectCause(is(throwable));
    process(foreach, eventBuilder().message(of(new DummyNestedIterableClass().iterator())).build(),
            false);
  }

  @Test
  public void filteredErrors() throws Exception {
    Foreach foreach = new Foreach();
    foreach.setMuleContext(muleContext);
    foreach.setMessageProcessors(singletonList(event -> {
      throw new RuntimeException("Expected");
    }));
    foreach.setIgnoreErrorType("ANY");
    foreach.initialise();

    process(foreach, eventBuilder().message(of(new DummyNestedIterableClass().iterator())).build(),
            false);
  }

  @Test
  public void failingExpression() throws Exception {
    Foreach foreach = new Foreach();
    foreach.setMuleContext(muleContext);
    foreach.setCollectionExpression("!@INVALID");
    foreach.setMessageProcessors(getSimpleMessageProcessors(new TestMessageProcessor("zas")));
    foreach.initialise();

    expectedException.expect(instanceOf(MessagingException.class));
    expectedException.expect(new FailingProcessorMatcher(foreach));
    expectedException.expectCause(instanceOf(ExpressionRuntimeException.class));
    process(foreach, eventBuilder().message(of(new DummyNestedIterableClass().iterator())).build(),
            false);
  }

  @Test
  public void batchSize() throws Exception {
    Foreach foreachMp = new Foreach();
    foreachMp.setMuleContext(muleContext);
    List<Processor> processors = getSimpleMessageProcessors(new TestMessageProcessor("zas"));
    foreachMp.setMessageProcessors(processors);
    foreachMp.setBatchSize(2);
    foreachMp.initialise();

    foreachMp.process(eventBuilder().message(of(asList(1, 2, 3))).build());

    assertThat(processedEvents, hasSize(2));
    assertThat(processedEvents.get(0).getMessageAsString(muleContext), is("[1, 2]:foo:zas"));
    assertThat(processedEvents.get(1).getMessageAsString(muleContext), is("[3]:foo:zas"));
  }

  @Test
  public void batchSizeWithCollectionAttributes() throws Exception {
    Foreach foreachMp = new Foreach();
    foreachMp.setMuleContext(muleContext);
    List<Processor> processors = getSimpleMessageProcessors(new TestMessageProcessor("zas"));
    foreachMp.setMessageProcessors(processors);
    foreachMp.setBatchSize(2);
    foreachMp.setCollectionExpression("vars.collection");
    foreachMp.initialise();

    foreachMp
        .process(eventBuilder().addVariable("collection", asList(1, 2, 3)).message(of(null)).build());

    assertThat(processedEvents, hasSize(2));
    assertThat(processedEvents.get(0).getMessageAsString(muleContext), is("[1, 2]:foo:zas"));
    assertThat(processedEvents.get(1).getMessageAsString(muleContext), is("[3]:foo:zas"));
  }

  private void assertSimpleProcessedMessages() {
    assertEquals(ERR_NUMBER_MESSAGES, 2, processedEvents.size());
    assertTrue(ERR_PAYLOAD_TYPE, processedEvents.get(0).getMessage().getPayload().getValue() instanceof String);
    assertTrue(ERR_PAYLOAD_TYPE, processedEvents.get(1).getMessage().getPayload().getValue() instanceof String);
    assertEquals(ERR_OUTPUT, "bar:foo:zas", processedEvents.get(0).getMessage().getPayload().getValue());
    assertEquals(ERR_OUTPUT, "zip:foo:zas", processedEvents.get(1).getMessage().getPayload().getValue());
  }

  private void assertNestedProcessedMessages() {
    String[] expectedOutputs = {"a1:foo:zas", "a2:foo:zas", "a3:foo:zas", "b1:foo:zas", "b2:foo:zas", "c1:foo:zas"};
    assertEquals(ERR_NUMBER_MESSAGES, 6, processedEvents.size());
    for (int i = 0; i < processedEvents.size(); i++) {
      assertTrue(ERR_PAYLOAD_TYPE, processedEvents.get(i).getMessage().getPayload().getValue() instanceof String);
    }
    for (int i = 0; i < processedEvents.size(); i++) {
      assertEquals(ERR_OUTPUT, expectedOutputs[i], processedEvents.get(i).getMessage().getPayload().getValue());
    }
  }

  public class DummySimpleIterableClass implements Iterable<String> {

    public List<String> strings = new ArrayList<>();

    public DummySimpleIterableClass() {
      strings.add("bar");
      strings.add("zip");
    }

    @Override
    public Iterator<String> iterator() {
      return strings.iterator();
    }
  }

  private class DummyNestedIterableClass implements Iterable<DummySimpleIterableClass> {

    private List<DummySimpleIterableClass> iterables = new ArrayList<>();

    public DummyNestedIterableClass() {
      DummySimpleIterableClass dsi1 = new DummySimpleIterableClass();
      dsi1.strings = new ArrayList<>();
      dsi1.strings.add("a1");
      dsi1.strings.add("a2");
      DummySimpleIterableClass dsi2 = new DummySimpleIterableClass();
      dsi2.strings = new ArrayList<>();
      dsi2.strings.add("a3");
      dsi2.strings.add("b1");
      dsi2.strings.add("b2");
      dsi2.strings.add("c1");
      iterables.add(dsi1);
      iterables.add(dsi2);
    }

    @Override
    public Iterator<DummySimpleIterableClass> iterator() {
      return iterables.iterator();
    }
  }

  private static class FailingProcessorMatcher extends BaseMatcher<MessagingException> {

    private Processor expectedFailingProcessor;

    FailingProcessorMatcher(Processor processor) {
      this.expectedFailingProcessor = processor;
    }

    @Override
    public boolean matches(Object o) {
      return o instanceof MessagingException && ((MessagingException) o).getFailingMessageProcessor() == expectedFailingProcessor;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("Exception is not a MessagingException or failing processor does not match.");
    }
  }

}
