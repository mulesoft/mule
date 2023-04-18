/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.internal.routing.ForeachInternalContextManager.getContext;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import static org.mule.test.allure.AllureConstants.ScopeFeature.ForeachStory.FOR_EACH;
import static org.mule.test.allure.AllureConstants.ScopeFeature.SCOPE;

import static java.util.stream.Collectors.toList;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.rules.ExpectedException.none;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.processor.InternalProcessor;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.tck.junit4.AbstractReactiveProcessorTestCase;
import org.mule.tck.testmodels.mule.TestMessageProcessor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;

@Feature(SCOPE)
@Story(FOR_EACH)
public abstract class AbstractForeachTestCase extends AbstractReactiveProcessorTestCase {

  private static final Logger LOGGER = getLogger(AbstractForeachTestCase.class);
  protected static final String ERR_NUMBER_MESSAGES = "Not a correct number of messages processed";
  protected static final String ERR_PAYLOAD_TYPE = "Type error on processed payloads";
  protected static final String ERR_OUTPUT = "Messages processed incorrectly";
  protected static final String ERR_INVALID_ITEM_SEQUENCE = "Null ItemSequence received";
  protected static final String ERR_SEQUENCE_OVERRIDDEN = "Sequence shouldn't be overridden after foreach";
  protected static final int CONCURRENCY = 1000;
  protected static final int CONCURRENCY_TIMEOUT_SECONDS = 20;

  protected Foreach foreach;
  protected Foreach chainedForeach;
  protected Foreach simpleForeach;
  protected Foreach nestedForeach;
  protected List<CoreEvent> processedEvents;
  protected Map<String, TypedValue<?>> variables;
  protected ExecutorService executorService;

  @Rule
  public ExpectedException expectedException = none();

  public AbstractForeachTestCase(Mode mode) {
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

  protected List<Processor> getSimpleMessageProcessors(Processor innerProcessor) {
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

  protected Foreach createForeach(List<Processor> mps) throws MuleException {
    Foreach foreachMp = createForeach();
    foreachMp.setMessageProcessors(mps);
    initialiseIfNeeded(foreachMp, muleContext);
    return foreachMp;
  }

  protected void expectNestedProcessorException(RuntimeException throwable, InternalTestProcessor failingProcessor) {
    expectedException.expect(MessagingException.class);
    expectedException.expect(new FailingProcessorMatcher(failingProcessor));
    expectedException.expectCause(is(throwable));
  }

  protected Foreach createForeach() {
    Foreach foreach = new Foreach();
    foreach.setAnnotations(getAppleFlowComponentLocationAnnotations());
    return foreach;
  }

  protected void expectExpressionException(Foreach foreach) {
    expectedException.expect(instanceOf(MessagingException.class));
    expectedException.expect(new FailingProcessorMatcher(foreach));
    expectedException.expectCause(instanceOf(ExpressionRuntimeException.class));
  }

  protected MessageProcessorChain createChain(Processor processor) throws InitialisationException {
    final MessageProcessorChain chain = newChain(Optional.empty(), processor);
    initialiseIfNeeded(chain, muleContext);
    return chain;
  }

  protected CoreEvent processInChain(MessageProcessorChain chain, CoreEvent event) throws Exception {
    return process(chain, event, false);
  }

  protected void assertSimpleProcessedMessages() {
    assertEquals(ERR_NUMBER_MESSAGES, 2, processedEvents.size());
    assertTrue(ERR_PAYLOAD_TYPE, processedEvents.get(0).getMessage().getPayload().getValue() instanceof String);
    assertTrue(ERR_PAYLOAD_TYPE, processedEvents.get(1).getMessage().getPayload().getValue() instanceof String);
    assertEquals(ERR_OUTPUT, "bar:foo:zas", processedEvents.get(0).getMessage().getPayload().getValue());
    assertEquals(ERR_OUTPUT, "zip:foo:zas", processedEvents.get(1).getMessage().getPayload().getValue());

    assertForEachContextConsumption((InternalEvent) processedEvents.get(0));
    assertForEachContextConsumption((InternalEvent) processedEvents.get(1));
  }

  protected void assertNestedProcessedMessages() {
    String[] expectedOutputs = {"a1:foo:zas", "a2:foo:zas", "a3:foo:zas", "b1:foo:zas", "b2:foo:zas", "c1:foo:zas"};
    assertEquals(ERR_NUMBER_MESSAGES, 6, processedEvents.size());
    for (CoreEvent processedEvent : processedEvents) {
      assertTrue(ERR_PAYLOAD_TYPE, processedEvent.getMessage().getPayload().getValue() instanceof String);
      assertForEachContextConsumption((InternalEvent) processedEvent);
    }
    for (int i = 0; i < processedEvents.size(); i++) {
      assertEquals(ERR_OUTPUT, expectedOutputs[i], processedEvents.get(i).getMessage().getPayload().getValue());
      assertForEachContextConsumption((InternalEvent) processedEvents.get(i));
    }
  }

  protected void assertForEachContextConsumption(InternalEvent event) {
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

  protected class DummyNestedIterableClass implements Iterable<DummySimpleIterableClass> {

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

  protected static class FailingProcessorMatcher extends BaseMatcher<MessagingException> {

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
  protected interface InternalTestProcessor extends Processor, InternalProcessor {

  }

}
