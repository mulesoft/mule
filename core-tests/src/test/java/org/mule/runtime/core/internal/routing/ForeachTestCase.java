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
import static java.util.Collections.synchronizedList;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.DataType.MULE_MESSAGE;
import static org.mule.runtime.api.metadata.DataType.NUMBER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.internal.routing.Foreach.DEFAULT_COUNTER_VARIABLE;
import static org.mule.runtime.core.internal.routing.Foreach.DEFAULT_ROOT_MESSAGE_VARIABLE;
import static org.mule.runtime.core.internal.routing.ForeachInternalContextManager.getContext;
import static org.mule.runtime.core.internal.streaming.CursorUtils.unwrap;
import static org.mule.runtime.core.internal.util.StringHashCodeCollisionGenerator.stringsWithSameHashCode;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChildContext;
import static org.mule.tck.junit4.matcher.DataTypeCompatibilityMatcher.assignableTo;
import static org.mule.tck.processor.ContextPropagationChecker.assertContextPropagation;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ForeachStory.FOR_EACH;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.ItemSequenceInfo;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.internal.streaming.ManagedCursorProvider;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.processor.InternalProcessor;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractReactiveProcessorTestCase;
import org.mule.tck.processor.ContextPropagationChecker;
import org.mule.tck.testmodels.mule.TestMessageProcessor;

import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(ROUTERS)
@Story(FOR_EACH)
public class ForeachTestCase extends AbstractReactiveProcessorTestCase {

  private static final Logger LOGGER = getLogger(ForeachTestCase.class);
  private static final String ERR_NUMBER_MESSAGES = "Not a correct number of messages processed";
  private static final String ERR_PAYLOAD_TYPE = "Type error on processed payloads";
  private static final String ERR_OUTPUT = "Messages processed incorrectly";
  private static final String ERR_INVALID_ITEM_SEQUENCE = "Null ItemSequence received";
  private static final String ERR_SEQUENCE_OVERRIDDEN = "Sequence should't be overridden after foreach";
  private static final int CONCURRENCY = 1000;
  private static final int CONCURRENCY_TIMEOUT_SECONDS = 20;

  private Foreach foreach;
  private Foreach chainedForeach;
  private Foreach simpleForeach;
  private Foreach nestedForeach;
  private ArrayList<CoreEvent> processedEvents;
  private Map<String, TypedValue<?>> variables;
  private ExecutorService executorService;

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
    disposeIfNeeded(chainedForeach, LOGGER);
    if (executorService != null) {
      executorService.shutdownNow();
    }
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
    // In newer versions of the runtime, there is a validation that prevents you from using a Map payload, but we
    // removed it from 4.4 in order to avoid breaking backwards compatibility. That's why we don't expect an exception
    // here...
    //
    // expectedException.expect(IllegalArgumentException.class);

    process(simpleForeach, eventBuilder(muleContext).message(of(singletonMap("foo", "bar"))).build());
  }

  @Test
  public void mapEntrySetExpression() throws Exception {
    simpleForeach.setCollectionExpression("#[dw::core::Objects::entrySet(payload)]");
    CoreEvent event = process(simpleForeach, eventBuilder(muleContext).message(of(singletonMap("foo", "bar"))).build());
    assertForEachContextConsumption((InternalEvent) event);
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
      assertForEachContextConsumption((InternalEvent) eventReference.get());
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

    MessageProcessorChain chain = createChain(foreach);
    try {
      expectNestedProcessorException(throwable, failingProcessor);
      processInChain(chain, eventBuilder(muleContext).message(of(new DummyNestedIterableClass().iterator())).build());
    } finally {
      assertThat(firstProcessor.invocations, equalTo(1));
      assertForEachContextConsumption((InternalEvent) eventReference.get());
      disposeIfNeeded(chain, LOGGER);
    }
  }

  @Test
  public void nestedForeachWithFailingExpression() throws Exception {
    Foreach internalForeach = createForeach();
    internalForeach.setCollectionExpression("!@INVALID");
    SensingNullMessageProcessor nestedEventProcessor = new SensingNullMessageProcessor();
    internalForeach.setMessageProcessors(singletonList(nestedEventProcessor));

    Foreach nestedForeach = createForeach();
    nestedForeach.setMessageProcessors(singletonList(internalForeach));
    initialiseIfNeeded(nestedForeach, muleContext);

    List<List<String>> payload = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      List<String> temp = new ArrayList<>();
      for (int j = 0; j < 2; j++) {
        temp.add(i + "-" + j);
      }
      payload.add(temp);
    }
    try {
      expectExpressionException(internalForeach);
      process(internalForeach, eventBuilder(muleContext).message(of(payload)).build(), false);
    } finally {
      assertThat(nestedEventProcessor.invocations, equalTo(0));
      disposeIfNeeded(nestedForeach, LOGGER);
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

    MessageProcessorChain chain = createChain(foreach);
    disposeIfNeeded(foreach, LOGGER);
    try {
      expectExpressionException(foreach);
      processInChain(chain, eventBuilder(muleContext).message(of(new DummyNestedIterableClass().iterator())).build());
    } finally {
      assertThat(firstProcessor.invocations, equalTo(0));
      disposeIfNeeded(chain, LOGGER);
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

    assertForEachContextConsumption((InternalEvent) processedEvents.get(0));
    assertForEachContextConsumption((InternalEvent) processedEvents.get(1));
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

    assertForEachContextConsumption((InternalEvent) processedEvents.get(0));
    assertForEachContextConsumption((InternalEvent) processedEvents.get(1));
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
               equalTo(NUMBER));
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
  @Issue("MULE-18573")
  public void muleMessageContainingACursorStreamShouldBeManagedByCursorManager() throws Exception {
    AtomicReference<CoreEvent> eventReference = new AtomicReference<>();
    foreach = createForeach();
    InternalTestProcessor capturedEventProcessor = event -> {
      eventReference.set(event);
      return event;
    };
    foreach.setMessageProcessors(asList(capturedEventProcessor));
    initialiseIfNeeded(foreach, muleContext);

    CursorProvider cursorProvider = mock(CursorStreamProvider.class);

    CoreEvent input = eventBuilder(muleContext).message(of(singletonList(of(cursorProvider)))).build();
    CoreEvent result = process(foreach, input);

    assertThat(result.getMessage(), equalTo(input.getMessage()));
    assertThat(eventReference.get().getMessage().getPayload().getValue(), is(instanceOf(ManagedCursorProvider.class)));
    ManagedCursorProvider managedCursorProvider =
        (ManagedCursorProvider) eventReference.get().getMessage().getPayload().getValue();
    assertThat(unwrap(managedCursorProvider), is(sameInstance(cursorProvider)));
  }

  @Test
  @Issue("MULE-18573")
  public void cursorStreamShouldBeManagedByCursorManager() throws Exception {
    AtomicReference<CoreEvent> eventReference = new AtomicReference<>();
    foreach = createForeach();
    InternalTestProcessor capturedEventProcessor = event -> {
      eventReference.set(event);
      return event;
    };
    foreach.setMessageProcessors(asList(capturedEventProcessor));
    initialiseIfNeeded(foreach, muleContext);

    CursorProvider cursorProvider = mock(CursorStreamProvider.class);
    CoreEvent input = eventBuilder(muleContext).message(of(singletonList(cursorProvider))).build();
    CoreEvent result = process(foreach, input);

    assertThat(result.getMessage(), equalTo(input.getMessage()));
    assertThat(eventReference.get().getMessage().getPayload().getValue(), is(instanceOf(ManagedCursorProvider.class)));
    ManagedCursorProvider managedCursorProvider =
        (ManagedCursorProvider) eventReference.get().getMessage().getPayload().getValue();
    assertThat(unwrap(managedCursorProvider), is(sameInstance(cursorProvider)));
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

    // Release resources already allocated for `simpleForeach`
    disposeIfNeeded(simpleForeach, LOGGER);
    simpleForeach = createForeach(singletonList(contextPropagationChecker));

    assertContextPropagation(eventBuilder(muleContext).message(of(asList("1", "2", "3"))).build(), simpleForeach,
                             contextPropagationChecker);
  }

  @Test
  @Issue("MULE-19143")
  public void multiplesThreadsUsingSameForeach() throws Exception {
    // Create and initialize 1st foreach
    foreach = createForeach();
    AtomicInteger firstForeachCounter = new AtomicInteger();
    InternalTestProcessor capturedEventProcessor = event -> {
      firstForeachCounter.incrementAndGet();
      return event;
    };

    foreach.setMessageProcessors(asList(capturedEventProcessor));
    initialiseIfNeeded(foreach, muleContext);

    // Create and initialize 2nd foreach
    AtomicInteger secondForeachCounter = new AtomicInteger();
    InternalTestProcessor secondCapturedEventProcessor = event -> {
      secondForeachCounter.incrementAndGet();
      return event;
    };
    chainedForeach = createForeach();
    chainedForeach.setMessageProcessors(asList(secondCapturedEventProcessor));
    initialiseIfNeeded(chainedForeach, muleContext);

    // Process 1st foreach
    CoreEvent parentEvent = foreach.process(eventBuilder(muleContext).message(of(asList(1, 2, 3))).build());
    assertThat(firstForeachCounter.get(), is(3));

    // Process 2nd foreach concurrently
    executorService = newFixedThreadPool(CONCURRENCY);

    CountDownLatch threadsLatch = new CountDownLatch(CONCURRENCY);
    Latch mainThreadLatch = new Latch();
    // Create collection of hash-code collision strings
    List<String> hashCodeCollisionContextsIds = stringsWithSameHashCode(CONCURRENCY);
    List<String> payload = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      payload.add("" + i);
    }

    final List<Throwable> exceptions = synchronizedList(new ArrayList<>());
    for (String id : hashCodeCollisionContextsIds) {
      // Create child context using hash code collision keys
      BaseEventContext childContext = newChildContext(parentEvent, Optional.empty());
      BaseEventContext baseEventContextMocked = spy(childContext);
      when(baseEventContextMocked.getId()).thenReturn(id);
      CoreEvent childEvent = CoreEvent.builder(baseEventContextMocked, parentEvent).message(of(payload)).build();
      executorService.submit(() -> {
        try {
          threadsLatch.countDown();
          mainThreadLatch.await();
          // Run chainedForeach
          chainedForeach.process(childEvent);
        } catch (Throwable e) {
          exceptions.add(e);
          LOGGER.error("An unexpected error processing events", e);
        }
      });
    }

    // Await all threads starts
    threadsLatch.await();
    mainThreadLatch.release();

    executorService.awaitTermination(CONCURRENCY_TIMEOUT_SECONDS, SECONDS);

    // All elements should be processed without exceptions
    assertTrue(exceptions.isEmpty());
    assertThat(secondForeachCounter.get(), is(CONCURRENCY * payload.size()));
  }

  private MessageProcessorChain createChain(Processor processor) throws InitialisationException {
    final MessageProcessorChain chain = newChain(Optional.empty(), processor);
    initialiseIfNeeded(chain, muleContext);
    return chain;
  }

  private CoreEvent processInChain(MessageProcessorChain chain, CoreEvent event) throws Exception {
    return process(chain, event, false);
  }

  private void assertSimpleProcessedMessages() {
    assertEquals(ERR_NUMBER_MESSAGES, 2, processedEvents.size());
    assertTrue(ERR_PAYLOAD_TYPE, processedEvents.get(0).getMessage().getPayload().getValue() instanceof String);
    assertTrue(ERR_PAYLOAD_TYPE, processedEvents.get(1).getMessage().getPayload().getValue() instanceof String);
    assertEquals(ERR_OUTPUT, "bar:foo:zas", processedEvents.get(0).getMessage().getPayload().getValue());
    assertEquals(ERR_OUTPUT, "zip:foo:zas", processedEvents.get(1).getMessage().getPayload().getValue());

    assertForEachContextConsumption((InternalEvent) processedEvents.get(0));
    assertForEachContextConsumption((InternalEvent) processedEvents.get(1));
  }

  private void assertNestedProcessedMessages() {
    String[] expectedOutputs = {"a1:foo:zas", "a2:foo:zas", "a3:foo:zas", "b1:foo:zas", "b2:foo:zas", "c1:foo:zas"};
    assertEquals(ERR_NUMBER_MESSAGES, 6, processedEvents.size());
    for (int i = 0; i < processedEvents.size(); i++) {
      assertTrue(ERR_PAYLOAD_TYPE, processedEvents.get(i).getMessage().getPayload().getValue() instanceof String);
      assertForEachContextConsumption((InternalEvent) processedEvents.get(i));
    }
    for (int i = 0; i < processedEvents.size(); i++) {
      assertEquals(ERR_OUTPUT, expectedOutputs[i], processedEvents.get(i).getMessage().getPayload().getValue());
      assertForEachContextConsumption((InternalEvent) processedEvents.get(i));
    }
  }

  private void assertForEachContextConsumption(InternalEvent event) {
    ForeachContext foreachContext = getContext(event);
    if (foreachContext != null) {
      assertThat(foreachContext.getIterator().hasNext(), is(false));
    }
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
