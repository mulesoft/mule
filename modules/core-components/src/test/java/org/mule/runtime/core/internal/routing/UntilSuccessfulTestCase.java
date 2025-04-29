/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static org.mule.runtime.api.component.location.ConfigurationComponentLocator.REGISTRY_KEY;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.getInstance;
import static org.mule.runtime.core.internal.routing.UntilSuccessfulRouter.RETRY_CTX_INTERNAL_PARAM_KEY;
import static org.mule.tck.MuleTestUtils.APPLE_FLOW;
import static org.mule.tck.MuleTestUtils.createAndRegisterFlow;
import static org.mule.tck.junit4.rule.DataWeaveExpressionLanguage.dataWeaveRule;
import static org.mule.tck.processor.ContextPropagationChecker.assertContextPropagation;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;
import static org.mule.test.allure.AllureConstants.ScopeFeature.SCOPE;
import static org.mule.test.allure.AllureConstants.ScopeFeature.UntilSuccessfulStory.UNTIL_SUCCESSFUL;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.internal.event.InternalEvent;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.privileged.exception.MessagingException;
import org.mule.runtime.core.privileged.processor.InternalProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.DataWeaveExpressionLanguage;
import org.mule.tck.processor.ContextPropagationChecker;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import reactor.core.publisher.Flux;

@Feature(SCOPE)
@Story(UNTIL_SUCCESSFUL)
@RunWith(Parameterized.class)
public class UntilSuccessfulTestCase extends AbstractMuleContextTestCase {

  private static final String MILLIS_BETWEEN_RETRIES = "100";
  private static final String RETRY_CTX_INTERNAL_PARAMETER_KEY = "untilSuccessful.router.retryContext";

  public static class ConfigurableMessageProcessor implements Processor, InternalProcessor {

    private volatile int eventCount;
    private volatile CoreEvent event;
    private volatile int numberOfFailuresToSimulate;

    @Override
    public CoreEvent process(final CoreEvent evt) throws MuleException {
      eventCount++;
      if (numberOfFailuresToSimulate-- > 0) {
        throw new RuntimeException("simulated problem");
      }
      this.event = evt;
      return evt;
    }

    public CoreEvent getEventReceived() {
      return event;
    }

    public int getEventCount() {
      return eventCount;
    }

    public void setNumberOfFailuresToSimulate(int numberOfFailuresToSimulate) {
      this.numberOfFailuresToSimulate = numberOfFailuresToSimulate;
    }
  }

  @Parameters(name = "tx: {0}")
  public static Collection<Boolean> modeParameters() {
    return asList(Boolean.TRUE, Boolean.FALSE);
  }

  @Rule
  public DataWeaveExpressionLanguage dw = dataWeaveRule();

  private Flow flow;
  private UntilSuccessful untilSuccessful;
  private ConfigurableMessageProcessor targetMessageProcessor;
  private final boolean tx;

  public UntilSuccessfulTestCase(boolean tx) {
    this.tx = tx;
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    return singletonMap(REGISTRY_KEY, componentLocator);
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    flow = createAndRegisterFlow(muleContext, APPLE_FLOW, componentLocator);
    untilSuccessful = buildUntilSuccessful(MILLIS_BETWEEN_RETRIES);
    if (tx) {
      getInstance().bindTransaction(mock(Transaction.class));
    }

  }

  @After
  public void doTeardown() throws Exception {
    untilSuccessful.dispose();
    super.doTearDown();
  }

  private UntilSuccessful buildUntilSuccessful(String millisBetweenRetries) throws Exception {
    targetMessageProcessor = new ConfigurableMessageProcessor();
    return buildUntilSuccessfulWithProcessors(millisBetweenRetries, "2", targetMessageProcessor);

  }

  private UntilSuccessful buildUntilSuccessfulWithProcessors(String millisBetweenRetries, String maxRetries,
                                                             Processor... processors)
      throws Exception {
    UntilSuccessful untilSuccessful = new UntilSuccessful();
    untilSuccessful.setMaxRetries(maxRetries);
    untilSuccessful.setAnnotations(getAppleFlowComponentLocationAnnotations());
    if (millisBetweenRetries != null) {
      untilSuccessful.setMillisBetweenRetries(millisBetweenRetries);
    }

    untilSuccessful.setMessageProcessors(asList(processors));
    muleContext.getInjector().inject(untilSuccessful);
    return untilSuccessful;
  }

  private UntilSuccessful buildNestedUntilSuccessful() throws Exception {
    UntilSuccessful untilSuccessful = new UntilSuccessful();
    untilSuccessful.setMaxRetries("1");
    untilSuccessful.setAnnotations(getAppleFlowComponentLocationAnnotations());

    targetMessageProcessor = new ConfigurableMessageProcessor();
    untilSuccessful.setMessageProcessors(singletonList(buildUntilSuccessfulWithProcessors(MILLIS_BETWEEN_RETRIES, "1",
                                                                                          targetMessageProcessor)));
    muleContext.getInjector().inject(untilSuccessful);
    return untilSuccessful;
  }

  @Override
  protected void doTearDown() throws Exception {
    untilSuccessful.stop();
  }

  @Test
  public void testSuccessfulDelivery() throws Exception {
    untilSuccessful.initialise();
    untilSuccessful.start();

    assertLogicallyEqualEvents(testEvent(), untilSuccessful.process(testEvent()));
    assertTargetEventReceived(testEvent());
  }

  @Test
  public void testSuccessfulDeliveryStreamPayload() throws Exception {
    untilSuccessful.initialise();
    untilSuccessful.start();

    final CoreEvent testEvent =
        eventBuilder().message(of(new ByteArrayInputStream("test_data".getBytes()))).build();
    assertSame(testEvent.getMessage(), untilSuccessful.process(testEvent).getMessage());
    assertTargetEventReceived(testEvent);
  }

  @Test
  public void testPermanentDeliveryFailure() throws Exception {
    targetMessageProcessor.setNumberOfFailuresToSimulate(Integer.MAX_VALUE);
    untilSuccessful.initialise();
    untilSuccessful.start();

    final CoreEvent testEvent = eventBuilder().message(of("ERROR")).build();
    var thrown = assertThrows(MessagingException.class, () -> untilSuccessful.process(testEvent));
    assertThat(thrown.getCause(), instanceOf(RetryPolicyExhaustedException.class));

    assertThat(targetMessageProcessor.getEventCount(), is(1 + parseInt(untilSuccessful.getMaxRetries())));
  }

  @Test
  public void testTemporaryDeliveryFailure() throws Exception {
    targetMessageProcessor.setNumberOfFailuresToSimulate(parseInt(untilSuccessful.getMaxRetries()));
    untilSuccessful.initialise();
    untilSuccessful.start();

    final CoreEvent testEvent = eventBuilder().message(of("ERROR")).build();
    assertSame(testEvent.getMessage(), untilSuccessful.process(testEvent).getMessage());
    assertTargetEventReceived(testEvent);
    assertThat(targetMessageProcessor.getEventCount(), is(1 + parseInt(untilSuccessful.getMaxRetries())));
  }

  @Test
  public void testProcessingStrategyUsage() throws Exception {
    targetMessageProcessor.setNumberOfFailuresToSimulate(parseInt(untilSuccessful.getMaxRetries()));
    untilSuccessful.initialise();
    untilSuccessful.start();

    final CoreEvent testEvent = eventBuilder().message(of("ERROR")).build();
    untilSuccessful.process(testEvent).getMessage();

    ProcessingStrategy ps = flow.getProcessingStrategy();
    verify(ps, never()).onPipeline(any(ReactiveProcessor.class));
  }

  @Test
  public void testDefaultMillisWait() throws Exception {
    untilSuccessful = buildUntilSuccessful(null);
    untilSuccessful.initialise();
    untilSuccessful.start();
    assertEquals(60 * 1000, parseInt(untilSuccessful.getMillisBetweenRetries()));
  }

  @Test
  public void testWithExpressionRetries() throws Exception {
    targetMessageProcessor.setNumberOfFailuresToSimulate(4);
    untilSuccessful.setMaxRetries("#[2 + 2]");
    untilSuccessful.initialise();
    untilSuccessful.start();


    final CoreEvent testEvent = eventBuilder().message(of("ERROR")).build();
    assertSame(testEvent.getMessage(), untilSuccessful.process(testEvent).getMessage());
    assertTargetEventReceived(testEvent);
    assertEquals(targetMessageProcessor.getEventCount(), 5);
  }

  @Test
  public void testWithExpressionRetriesMultipleExecutions() throws Exception {
    targetMessageProcessor.setNumberOfFailuresToSimulate(2);
    untilSuccessful.setMaxRetries("#[payload + 2]");
    untilSuccessful.initialise();
    untilSuccessful.start();


    final CoreEvent testEvent = eventBuilder().message(of(4)).build();
    assertSame(testEvent.getMessage(), untilSuccessful.process(testEvent).getMessage());
    assertSame(testEvent.getMessage(), untilSuccessful.process(testEvent).getMessage());
    assertEquals(targetMessageProcessor.getEventCount(), 4);
  }

  @Test
  public void testWithExpressionRetriesUsingPayload() throws Exception {
    targetMessageProcessor.setNumberOfFailuresToSimulate(10);
    untilSuccessful.setMaxRetries("#[payload + 2]");
    untilSuccessful.initialise();
    untilSuccessful.start();
    final CoreEvent testEvent = eventBuilder().message(of(1)).build();

    var thrown = assertThrows(MessagingException.class, () -> untilSuccessful.process(testEvent));
    assertThat(thrown.getCause(), instanceOf(RetryPolicyExhaustedException.class));
    assertThat(targetMessageProcessor.getEventCount(), is(4));
  }

  @Test
  public void testWithWrongExpressionRetry() throws Exception {
    targetMessageProcessor.setNumberOfFailuresToSimulate(10);
    untilSuccessful.setMaxRetries("#[payload + 2]");
    untilSuccessful.initialise();
    untilSuccessful.start();

    final CoreEvent testEvent = eventBuilder().message(of("queso")).build();
    var thrown = assertThrows(Exception.class, () -> untilSuccessful.process(testEvent));
    assertThat(thrown.getCause(), instanceOf(ExpressionRuntimeException.class));
    assertThat(thrown.getMessage(), containsString("You called the function '+' with these arguments"));
  }

  @Test
  public void testRetryContextIsClearedAfterSuccessfulScopeExecution() throws Exception {
    targetMessageProcessor.setNumberOfFailuresToSimulate(1);
    untilSuccessful.setMaxRetries("1");
    untilSuccessful.setMillisBetweenRetries(MILLIS_BETWEEN_RETRIES);
    untilSuccessful.initialise();
    untilSuccessful.start();

    final CoreEvent testEvent = eventBuilder().message(of("queso")).build();
    CoreEvent response = untilSuccessful.process(testEvent);
    assertThat(getPayloadAsString(response.getMessage()), is("queso"));
    Map<String, Object> retryCtxContainer = ((InternalEvent) response).getInternalParameter(RETRY_CTX_INTERNAL_PARAMETER_KEY);
    assertThat(retryCtxContainer.isEmpty(), is(true));
  }

  @Test
  public void testRetryContextIsClearedAfterNestedSuccessfulScopeExecution() throws Exception {
    untilSuccessful = buildNestedUntilSuccessful();
    targetMessageProcessor.setNumberOfFailuresToSimulate(1);
    untilSuccessful.setMaxRetries("1");
    untilSuccessful.setMillisBetweenRetries(MILLIS_BETWEEN_RETRIES);
    untilSuccessful.initialise();
    untilSuccessful.start();

    final CoreEvent testEvent = eventBuilder().message(of("queso")).build();
    CoreEvent response = untilSuccessful.process(testEvent);
    assertThat(getPayloadAsString(response.getMessage()), is("queso"));
    Map<String, Object> retryCtxContainer = ((InternalEvent) response).getInternalParameter(RETRY_CTX_INTERNAL_PARAMETER_KEY);
    assertThat(retryCtxContainer.isEmpty(), is(true));
  }

  @Test
  public void testRetryContextIsClearedAfterExhaustedScopeExecution() throws Exception {
    targetMessageProcessor.setNumberOfFailuresToSimulate(2);
    untilSuccessful.setMaxRetries("1");
    untilSuccessful.setMillisBetweenRetries(MILLIS_BETWEEN_RETRIES);
    untilSuccessful.initialise();
    untilSuccessful.start();

    assertNoRetryContextAfterScopeExecutions(2);
  }

  @Test
  public void testRetryContextIsClearedAfterNestedExhaustedScopeExecution() throws Exception {
    untilSuccessful = buildNestedUntilSuccessful();
    targetMessageProcessor.setNumberOfFailuresToSimulate(4);
    untilSuccessful.setMaxRetries("1");
    untilSuccessful.setMillisBetweenRetries(MILLIS_BETWEEN_RETRIES);
    untilSuccessful.initialise();
    untilSuccessful.start();

    assertNoRetryContextAfterScopeExecutions(4);
  }

  @Test
  public void subscriberContextPropagation() throws MuleException {
    final ContextPropagationChecker contextPropagationChecker = new ContextPropagationChecker();

    untilSuccessful = new UntilSuccessful();
    untilSuccessful.setAnnotations(getAppleFlowComponentLocationAnnotations());
    untilSuccessful.setMessageProcessors(singletonList(contextPropagationChecker));
    muleContext.getInjector().inject(untilSuccessful);

    untilSuccessful.initialise();
    untilSuccessful.start();

    assertContextPropagation(eventBuilder().message(of("1")).build(), untilSuccessful, contextPropagationChecker);
  }

  @Test
  public void routerFluxesLifecycle() throws MuleException {
    untilSuccessful.initialise();

    final FluxSinkRecorder<CoreEvent> emitter = new FluxSinkRecorder<>();
    final ProcessingStrategy ps = mock(ProcessingStrategy.class);

    AtomicReference<CoreEvent> innerEventRef = new AtomicReference<>();

    when(ps.configureInternalPublisher(any())).thenAnswer(inv -> {
      Flux<CoreEvent> innerFlux = inv.getArgument(0);
      return innerFlux.doOnNext(innerEventRef::set);
    });

    final UntilSuccessfulRouter router = new UntilSuccessfulRouter(flow, emitter.flux(), e -> e, ps,
                                                                   dw.getExpressionManager(), null, null,
                                                                   "1", MILLIS_BETWEEN_RETRIES, true);
    // Assert that the inner flux was registered in the ps.
    verify(ps).configureInternalPublisher(any());

    final Flux<CoreEvent> downstreamPublisher = Flux.from(router.getDownstreamPublisher());
    final Runnable completableConsumer = mock(Runnable.class);
    downstreamPublisher.subscribe(null, null, completableConsumer);

    emitter.next(testEvent());
    assertThat("Event peeked in the innerFlux does not have the retry context.",
               ((InternalEvent) innerEventRef.get()).getInternalParameters(), hasKey(RETRY_CTX_INTERNAL_PARAM_KEY));

    emitter.complete();
    // Verify that completion reached downstream
    verify(completableConsumer).run();
  }

  protected void assertNoRetryContextAfterScopeExecutions(int expectedExecutions) throws MuleException {
    final CoreEvent testEvent = eventBuilder().message(of("queso")).build();
    try {
      untilSuccessful.process(testEvent);
      fail("An exhaustion error was expected from an until successful scope");
    } catch (Exception e) {
      MessagingException messagingException = (MessagingException) e;
      Map<String, Object> retryCtxContainer =
          ((InternalEvent) messagingException.getEvent()).getInternalParameter(RETRY_CTX_INTERNAL_PARAMETER_KEY);
      assertThat(retryCtxContainer.isEmpty(), is(true));
      assertThat(targetMessageProcessor.eventCount, is(expectedExecutions));
    }
  }

  private void assertTargetEventReceived(CoreEvent request) throws MuleException {
    assertThat(targetMessageProcessor.getEventReceived(), not(nullValue()));
    assertLogicallyEqualEvents(request, targetMessageProcessor.getEventReceived());
  }

  private void assertLogicallyEqualEvents(final CoreEvent testEvent, CoreEvent eventReceived) throws MuleException {
    // events have been rewritten so are different but the correlation ID has been carried around
    assertEquals(testEvent.getCorrelationId(), eventReceived.getCorrelationId());
    // and their payload
    assertEquals(testEvent.getMessage(), eventReceived.getMessage());
  }

}
