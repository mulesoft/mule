/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.DataType.MULE_MESSAGE;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.internal.routing.Foreach.DEFAULT_COUNTER_VARIABLE;
import static org.mule.runtime.core.internal.routing.Foreach.DEFAULT_ROOT_MESSAGE_VARIABLE;
import static org.mule.runtime.core.internal.routing.ForeachRouter.MAP_NOT_SUPPORTED_MESSAGE;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import static org.mule.tck.junit4.matcher.DataTypeCompatibilityMatcher.assignableTo;
import static org.mule.tck.processor.ContextPropagationChecker.assertContextPropagation;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ForeachStory.FOR_EACH;
import static org.slf4j.LoggerFactory.getLogger;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.hamcrest.Matchers;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.ItemSequenceInfo;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.processor.InternalProcessor;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractReactiveProcessorTestCase;
import org.mule.tck.processor.ContextPropagationChecker;
import org.mule.tck.testmodels.mule.TestMessageProcessor;

import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicReference;

@Feature(ROUTERS)
@Story(FOR_EACH)
public class ForeachTestCase extends AbstractReactiveProcessorTestCase {

  private static final Logger LOGGER = getLogger(ForeachTestCase.class);
  private static final String MULE_FOREACH_CONTEXT_KEY = "mule.foreach.router.foreachContext";

  protected Foreach foreach;
  private Foreach simpleForeach;
  private Foreach nestedForeach;
  private ArrayList<CoreEvent> processedEvents;
  protected Map<String, TypedValue<?>> variables;

  private static String ERR_NUMBER_MESSAGES = "Not a correct number of messages processed";
  private static String ERR_PAYLOAD_TYPE = "Type error on processed payloads";
  private static String ERR_OUTPUT = "Messages processed incorrectly";
  private static final String ERR_INVALID_ITEM_SEQUENCE = "Null ItemSequence received";
  private static final String ERR_SEQUENCE_OVERRIDDEN = "Sequence should't be overridden after foreach";

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

  @After
  public void after() {
    disposeIfNeeded(nestedForeach, LOGGER);
    disposeIfNeeded(simpleForeach, LOGGER);
    disposeIfNeeded(foreach, LOGGER);
  }

  private List<Processor> getSimpleMessageProcessors(Processor innerProcessor) {
    List<Processor> lmp = new ArrayList<>();
    lmp.add(event -> {
      String payload;
      if (event.getMessage().getPayload().getValue() instanceof List) {
        // With batch size a simple list is not used, rather a list of typed values. This appears inconsistent but is transparent
        // to the user.
        payload = ((List<TypedValue>) event.getMessage().getPayload().getValue()).stream().map(TypedValue::getValue)
            .collect(toList()).toString();
      } else {
        payload = event.getMessage().getPayload().getValue().toString();
      }
      event = CoreEvent.builder(event).message(InternalMessage.builder(event.getMessage()).value(payload + ":foo").build())
          .build();
      return event;
    });
    lmp.add(innerProcessor);
    lmp.add(event -> {
      variables = event.getVariables();
      processedEvents.add(event);
      return event;
    });
    return lmp;
  }

  private List<Processor> getNestedMessageProcessors() {
    List<Processor> lmp = new ArrayList<>();
    Foreach internalForeach = createForeach();
    internalForeach.setMessageProcessors(getSimpleMessageProcessors(new TestMessageProcessor("zas")));
    lmp.add(internalForeach);
    return lmp;
  }

  private Foreach createForeach(List<Processor> mps) throws MuleException {
    Foreach foreachMp = createForeach();
    foreachMp.setMessageProcessors(mps);
    initialiseIfNeeded(foreachMp, muleContext);
    return foreachMp;
  }

  @Test
  public void arrayListPayload() throws Exception {
    List<String> arrayList = new ArrayList<>();
    arrayList.add("bar");
    arrayList.add("zip");
    process(simpleForeach, eventBuilder(muleContext).message(of(arrayList)).build());

    assertSimpleProcessedMessages();
  }

  @Test
  public void arrayPayload() throws Exception {
    String[] array = new String[2];
    array[0] = "bar";
    array[1] = "zip";
    process(simpleForeach, eventBuilder(muleContext).message(of(array)).build());

    assertSimpleProcessedMessages();
  }

  @Test
  public void muleMessageCollectionPayload() throws Exception {
    List<Message> list = new ArrayList<>();
    list.add(of("bar"));
    list.add(of("zip"));

    process(simpleForeach, eventBuilder(muleContext).message(of(list)).build());

    assertSimpleProcessedMessages();
  }

  @Test
  public void iterablePayload() throws Exception {
    Iterable<String> iterable = new DummySimpleIterableClass();
    final CoreEvent testEvent = eventBuilder(muleContext).message(of(iterable)).build();
    process(simpleForeach, testEvent);

    assertSimpleProcessedMessages();
  }

  @Test
  public void iteratorPayload() throws Exception {
    Iterable<String> iterable = new DummySimpleIterableClass();
    process(simpleForeach, eventBuilder(muleContext).message(of(iterable.iterator())).build());

    assertSimpleProcessedMessages();
  }

  @Test
  public void mapPayload() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(MAP_NOT_SUPPORTED_MESSAGE);
    process(simpleForeach, eventBuilder(muleContext).message(of(singletonMap("foo", "bar"))).build());
  }

  @Test
  public void mapEntrySetExpression() throws Exception {
    simpleForeach.setCollectionExpression("#[dw::core::Objects::entrySet(payload)]");
    CoreEvent event = process(simpleForeach, eventBuilder(muleContext).message(of(singletonMap("foo", "bar"))).build());
    assertNoForEachContext((InternalEvent) event);
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

    process(nestedForeach, eventBuilder(muleContext).message(of(payload)).build());

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

    process(nestedForeach, eventBuilder(muleContext).message(of(payload)).build());
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

    process(nestedForeach, eventBuilder(muleContext).message(parentCollection).build());

    assertNestedProcessedMessages();
  }

  @Test
  public void nestedIterablePayload() throws Exception {
    Iterable<DummySimpleIterableClass> iterable = new DummyNestedIterableClass();

    process(nestedForeach, eventBuilder(muleContext).message(of(iterable)).build());
    assertNestedProcessedMessages();
  }

  @Test
  public void nestedIteratorPayload() throws Exception {
    Iterable<DummySimpleIterableClass> iterable = new DummyNestedIterableClass();

    process(nestedForeach, eventBuilder(muleContext).message(of(iterable)).build());
    assertNestedProcessedMessages();
  }

  @Test
  public void failingNestedProcessor() throws Exception {
    AtomicReference<CoreEvent> eventReference = new AtomicReference<>();
    RuntimeException throwable = new BufferOverflowException();
    foreach = createForeach();
    SensingNullMessageProcessor firstProcessor = new SensingNullMessageProcessor();
    InternalTestProcessor failingProcessor = event -> {
      eventReference.set(event);
      throw throwable;
    };
    foreach.setMessageProcessors(asList(firstProcessor, failingProcessor));
    initialiseIfNeeded(foreach, muleContext);
    try {
      expectNestedProcessorException(throwable, failingProcessor);
      process(foreach, eventBuilder(muleContext).message(of(new DummyNestedIterableClass().iterator())).build(), false);
    } finally {
      assertThat(firstProcessor.invocations, equalTo(1));
      assertNoForEachContext((InternalEvent) eventReference.get());
    }
  }

  private void expectNestedProcessorException(RuntimeException throwable, InternalTestProcessor failingProcessor) {
    expectedException.expect(MessagingException.class);
    expectedException.expect(new FailingProcessorMatcher(failingProcessor));
    expectedException.expectCause(is(throwable));
  }

  @Test
  public void failingNestedProcessorInChain() throws Exception {
    AtomicReference<CoreEvent> eventReference = new AtomicReference<>();
    RuntimeException throwable = new BufferOverflowException();
    foreach = createForeach();
    SensingNullMessageProcessor firstProcessor = new SensingNullMessageProcessor();
    InternalTestProcessor failingProcessor = event -> {
      eventReference.set(event);
      throw throwable;
    };
    foreach.setMessageProcessors(asList(firstProcessor, failingProcessor));
    initialiseIfNeeded(foreach, muleContext);
    try {
      expectNestedProcessorException(throwable, failingProcessor);
      processInChain(foreach, eventBuilder(muleContext).message(of(new DummyNestedIterableClass().iterator())).build());
    } finally {
      assertThat(firstProcessor.invocations, equalTo(1));
      assertNoForEachContext((InternalEvent) eventReference.get());
    }
  }

  private Foreach createForeach() {
    Foreach foreach = new Foreach();
    foreach.setAnnotations(getAppleFlowComponentLocationAnnotations());
    return foreach;
  }

  @Test
  public void failingExpression() throws Exception {
    foreach = createForeach();
    foreach.setCollectionExpression("!@INVALID");
    SensingNullMessageProcessor firstProcessor = new SensingNullMessageProcessor();
    List<Processor> processors = getSimpleMessageProcessors(new TestMessageProcessor("zas"));
    processors.add(0, firstProcessor);
    foreach.setMessageProcessors(processors);
    initialiseIfNeeded(foreach, muleContext);

    try {
      expectExpressionException(foreach);
      process(foreach, eventBuilder(muleContext).message(of(new DummyNestedIterableClass().iterator())).build(),
              false);
    } finally {
      assertThat(firstProcessor.invocations, equalTo(0));
    }
  }

  @Test
  public void failingExpressionInChain() throws Exception {
    foreach = createForeach();
    foreach.setCollectionExpression("!@INVALID");
    SensingNullMessageProcessor firstProcessor = new SensingNullMessageProcessor();
    List<Processor> processors = getSimpleMessageProcessors(new TestMessageProcessor("zas"));
    processors.add(0, firstProcessor);
    foreach.setMessageProcessors(processors);
    initialiseIfNeeded(foreach, muleContext);

    try {
      expectExpressionException(foreach);
      processInChain(foreach, eventBuilder(muleContext).message(of(new DummyNestedIterableClass().iterator())).build());
    } finally {
      assertThat(firstProcessor.invocations, equalTo(0));
    }
  }

  private void expectExpressionException(Foreach foreach) {
    expectedException.expect(instanceOf(MessagingException.class));
    expectedException.expect(new FailingProcessorMatcher(foreach));
    expectedException.expectCause(instanceOf(ExpressionRuntimeException.class));
  }

  @Test
  public void batchSize() throws Exception {
    foreach = createForeach();
    List<Processor> processors = getSimpleMessageProcessors(new TestMessageProcessor("zas"));
    foreach.setMessageProcessors(processors);
    foreach.setBatchSize(2);
    initialiseIfNeeded(foreach, muleContext);

    foreach.process(eventBuilder(muleContext).message(of(asList(1, 2, 3))).build());

    assertThat(processedEvents, hasSize(2));
    assertThat(((PrivilegedEvent) processedEvents.get(0)).getMessageAsString(muleContext), is("[1, 2]:foo:zas"));
    assertThat(((PrivilegedEvent) processedEvents.get(1)).getMessageAsString(muleContext), is("[3]:foo:zas"));

    assertNoForEachContext((InternalEvent) processedEvents.get(0));
    assertNoForEachContext((InternalEvent) processedEvents.get(1));
  }

  @Test
  public void batchSizeWithCollectionAttributes() throws Exception {
    foreach = createForeach();
    List<Processor> processors = getSimpleMessageProcessors(new TestMessageProcessor("zas"));
    foreach.setMessageProcessors(processors);
    foreach.setBatchSize(2);
    foreach.setCollectionExpression("vars.collection");
    initialiseIfNeeded(foreach, muleContext);
    foreach.process(eventBuilder(muleContext).addVariable("collection", asList(1, 2, 3)).message(of(null)).build());

    assertThat(processedEvents, hasSize(2));
    assertThat(((PrivilegedEvent) processedEvents.get(0)).getMessageAsString(muleContext), is("[1, 2]:foo:zas"));
    assertThat(((PrivilegedEvent) processedEvents.get(1)).getMessageAsString(muleContext), is("[3]:foo:zas"));

    assertNoForEachContext((InternalEvent) processedEvents.get(0));
    assertNoForEachContext((InternalEvent) processedEvents.get(1));
  }

  @Test
  public void variables() throws Exception {
    List<String> arrayList = new ArrayList<>();
    arrayList.add("bar");
    arrayList.add("zip");
    CoreEvent in = eventBuilder(muleContext).message(of(arrayList)).build();
    process(simpleForeach, in);

    assertSimpleProcessedMessages();
    assertThat(variables.keySet(), hasSize(2));
    assertThat(variables.keySet(), hasItems(DEFAULT_ROOT_MESSAGE_VARIABLE, DEFAULT_COUNTER_VARIABLE));

    assertThat(variables.get(DEFAULT_ROOT_MESSAGE_VARIABLE).getDataType(), is(assignableTo(MULE_MESSAGE)));
    assertThat(variables.get(DEFAULT_ROOT_MESSAGE_VARIABLE).getValue(), equalTo(in.getMessage()));

    assertThat(variables.get(DEFAULT_COUNTER_VARIABLE).getDataType(),
               equalTo(DataType.builder().type(Integer.class).build()));
    assertThat(variables.get(DEFAULT_COUNTER_VARIABLE).getValue(), equalTo(2));
  }

  @Test
  public void empty() throws Exception {
    CoreEvent input = eventBuilder(muleContext).message(of(emptyList())).build();
    CoreEvent result = process(simpleForeach, input);

    assertThat(result.getMessage(), equalTo(input.getMessage()));
    assertThat(processedEvents, hasSize(0));
  }

  @Test
  @Issue("MULE-16764")
  @io.qameta.allure.Description("ForEach should set itemSequenceInfo")
  public void itemSequences() throws Exception {
    List<String> payload = new ArrayList<>();
    payload.add("one");
    payload.add("two");
    payload.add("three");
    payload.add("four");

    CoreEvent in = eventBuilder(muleContext).message(of(payload)).build();
    process(simpleForeach, in);

    List<Integer> sequences = processedEvents.stream()
        .map(e -> e.getItemSequenceInfo().map(i -> i.getPosition()).orElse(-1))
        .collect(toList());

    assertThat(ERR_INVALID_ITEM_SEQUENCE, sequences, is(asList(0, 1, 2, 3)));
  }

  @Test
  @Issue("MULE-16764")
  @io.qameta.allure.Description("ForEach doesn't override any itemSequence after completion")
  public void notOverrideParentSequence() throws Exception {
    List<String> payload = new ArrayList<>();
    Optional<ItemSequenceInfo> inEventItemSequence = Optional.of(ItemSequenceInfo.of(666));

    payload.add("A");
    payload.add("B");
    payload.add("C");

    CoreEvent in = eventBuilder(muleContext).itemSequenceInfo(inEventItemSequence).message(of(payload)).build();

    CoreEvent processedEvent = process(simpleForeach, in);

    assertThat(ERR_SEQUENCE_OVERRIDDEN, processedEvent.getItemSequenceInfo(), is(inEventItemSequence));
  }

  @Test
  public void subscriberContextPropagation() throws MuleException {
    final ContextPropagationChecker contextPropagationChecker = new ContextPropagationChecker();

    simpleForeach = createForeach(singletonList(contextPropagationChecker));

    assertContextPropagation(eventBuilder(muleContext).message(of(asList("1", "2", "3"))).build(), simpleForeach,
                             contextPropagationChecker);
  }

  private CoreEvent processInChain(Processor processor, CoreEvent event) throws Exception {
    final MessageProcessorChain chain = newChain(Optional.empty(), processor);
    initialiseIfNeeded(chain, muleContext);
    return process(chain, event, false);
  }

  private void assertSimpleProcessedMessages() {
    assertEquals(ERR_NUMBER_MESSAGES, 2, processedEvents.size());
    assertTrue(ERR_PAYLOAD_TYPE, processedEvents.get(0).getMessage().getPayload().getValue() instanceof String);
    assertTrue(ERR_PAYLOAD_TYPE, processedEvents.get(1).getMessage().getPayload().getValue() instanceof String);
    assertEquals(ERR_OUTPUT, "bar:foo:zas", processedEvents.get(0).getMessage().getPayload().getValue());
    assertEquals(ERR_OUTPUT, "zip:foo:zas", processedEvents.get(1).getMessage().getPayload().getValue());

    assertNoForEachContext((InternalEvent) processedEvents.get(0));
    assertNoForEachContext((InternalEvent) processedEvents.get(1));
  }

  private void assertNestedProcessedMessages() {
    String[] expectedOutputs = {"a1:foo:zas", "a2:foo:zas", "a3:foo:zas", "b1:foo:zas", "b2:foo:zas", "c1:foo:zas"};
    assertEquals(ERR_NUMBER_MESSAGES, 6, processedEvents.size());
    for (int i = 0; i < processedEvents.size(); i++) {
      assertTrue(ERR_PAYLOAD_TYPE, processedEvents.get(i).getMessage().getPayload().getValue() instanceof String);
      assertNoForEachContext((InternalEvent) processedEvents.get(i));
    }
    for (int i = 0; i < processedEvents.size(); i++) {
      assertEquals(ERR_OUTPUT, expectedOutputs[i], processedEvents.get(i).getMessage().getPayload().getValue());
      assertNoForEachContext((InternalEvent) processedEvents.get(i));
    }
  }

  private void assertNoForEachContext(InternalEvent event) {
    Map<String, Object> forEachContext = event.getInternalParameter(MULE_FOREACH_CONTEXT_KEY);
    assertThat(forEachContext.isEmpty(), Matchers.is(true));
  }

  public class DummySimpleIterableClass implements Iterable<String> {

    List<String> strings = new ArrayList<>();

    DummySimpleIterableClass() {
      strings.add("bar");
      strings.add("zip");
    }

    @Override
    public Iterator<String> iterator() {
      return strings.iterator();
    }
  }

  private class DummyNestedIterableClass implements Iterable<DummySimpleIterableClass> {

    private final List<DummySimpleIterableClass> iterables = new ArrayList<>();

    DummyNestedIterableClass() {
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

    private final Processor expectedFailingProcessor;

    FailingProcessorMatcher(Processor processor) {
      this.expectedFailingProcessor = processor;
    }

    @Override
    public boolean matches(Object o) {
      return o instanceof MessagingException && ((MessagingException) o).getFailingComponent() == expectedFailingProcessor;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("Exception is not a MessagingException or failing processor does not match.");
    }
  }

  @FunctionalInterface
  private interface InternalTestProcessor extends Processor, InternalProcessor {

  }

}
