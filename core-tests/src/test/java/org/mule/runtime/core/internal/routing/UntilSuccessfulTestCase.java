/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.api.component.location.ConfigurationComponentLocator.REGISTRY_KEY;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.getInstance;
import static org.mule.runtime.core.internal.exception.ErrorTypeRepositoryFactory.createDefaultErrorTypeRepository;
import static org.mule.tck.MuleTestUtils.APPLE_FLOW;
import static org.mule.tck.MuleTestUtils.OBJECT_ERROR_TYPE_REPO_REGISTRY_KEY;
import static org.mule.tck.MuleTestUtils.createAndRegisterFlow;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.processor.InternalProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Map;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class UntilSuccessfulTestCase extends AbstractMuleContextTestCase {

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
    return asList(new Boolean[] {Boolean.TRUE, Boolean.FALSE});
  }

  @Rule
  public ExpectedException expected = ExpectedException.none();
  private Flow flow;
  private UntilSuccessful untilSuccessful;
  private ConfigurableMessageProcessor targetMessageProcessor;
  private final boolean tx;

  public UntilSuccessfulTestCase(boolean tx) {
    this.tx = tx;
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    return of(REGISTRY_KEY, componentLocator,
              OBJECT_ERROR_TYPE_REPO_REGISTRY_KEY, createDefaultErrorTypeRepository());
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    flow = createAndRegisterFlow(muleContext, APPLE_FLOW, componentLocator);
    untilSuccessful = buildUntilSuccessful("1000");
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
    untilSuccessful.setMessageProcessors(singletonList(buildUntilSuccessfulWithProcessors("100", "1", targetMessageProcessor)));
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
        eventBuilder(muleContext).message(of(new ByteArrayInputStream("test_data".getBytes()))).build();
    assertSame(testEvent.getMessage(), untilSuccessful.process(testEvent).getMessage());
    assertTargetEventReceived(testEvent);
  }

  @Test
  public void testPermanentDeliveryFailure() throws Exception {
    targetMessageProcessor.setNumberOfFailuresToSimulate(Integer.MAX_VALUE);
    untilSuccessful.initialise();
    untilSuccessful.start();

    final CoreEvent testEvent = eventBuilder(muleContext).message(of("ERROR")).build();
    expected.expect(MessagingException.class);
    expected.expectCause(instanceOf(RetryPolicyExhaustedException.class));
    try {
      untilSuccessful.process(testEvent);
    } finally {
      assertEquals(1 + Integer.parseInt(untilSuccessful.getMaxRetries()), targetMessageProcessor.getEventCount());
    }
  }

  @Test
  public void testTemporaryDeliveryFailure() throws Exception {
    targetMessageProcessor.setNumberOfFailuresToSimulate(Integer.parseInt(untilSuccessful.getMaxRetries()));
    untilSuccessful.initialise();
    untilSuccessful.start();

    final CoreEvent testEvent = eventBuilder(muleContext).message(of("ERROR")).build();
    assertSame(testEvent.getMessage(), untilSuccessful.process(testEvent).getMessage());
    assertTargetEventReceived(testEvent);
    assertEquals(targetMessageProcessor.getEventCount(), Integer.parseInt(untilSuccessful.getMaxRetries()) + 1);
  }

  @Test
  public void testProcessingStrategyUsage() throws Exception {
    targetMessageProcessor.setNumberOfFailuresToSimulate(Integer.parseInt(untilSuccessful.getMaxRetries()));
    untilSuccessful.initialise();
    untilSuccessful.start();

    final CoreEvent testEvent = eventBuilder(muleContext).message(of("ERROR")).build();
    untilSuccessful.process(testEvent).getMessage();

    ProcessingStrategy ps = flow.getProcessingStrategy();
    verify(ps, never()).onPipeline(any(ReactiveProcessor.class));
  }

  @Test
  public void testDefaultMillisWait() throws Exception {
    untilSuccessful = buildUntilSuccessful(null);
    untilSuccessful.initialise();
    untilSuccessful.start();
    assertEquals(60 * 1000, Integer.parseInt(untilSuccessful.getMillisBetweenRetries()));
  }

  @Test
  public void testWithExpressionRetries() throws Exception {
    targetMessageProcessor.setNumberOfFailuresToSimulate(4);
    untilSuccessful.setMaxRetries("#[2 + 2]");
    untilSuccessful.initialise();
    untilSuccessful.start();


    final CoreEvent testEvent = eventBuilder(muleContext).message(of("ERROR")).build();
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


    final CoreEvent testEvent = eventBuilder(muleContext).message(of(4)).build();
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
    final CoreEvent testEvent = eventBuilder(muleContext).message(of(1)).build();
    expected.expect(MessagingException.class);
    expected.expectCause(instanceOf(RetryPolicyExhaustedException.class));
    try {
      untilSuccessful.process(testEvent);
    } finally {
      assertEquals(targetMessageProcessor.getEventCount(), 4);
    }
  }

  @Test
  public void testWithWrongExpressionRetry() throws Exception {
    targetMessageProcessor.setNumberOfFailuresToSimulate(10);
    untilSuccessful.setMaxRetries("#[payload + 2]");
    untilSuccessful.initialise();
    untilSuccessful.start();

    final CoreEvent testEvent = eventBuilder(muleContext).message(of("queso")).build();
    expected.expect(ExpressionRuntimeException.class);
    expected.expectMessage(containsString("You called the function '+' with these arguments"));
    untilSuccessful.process(testEvent);
  }

  @Test
  public void testRetryContextIsClearedAfterSuccessfulScopeExecution() throws Exception {
    targetMessageProcessor.setNumberOfFailuresToSimulate(1);
    untilSuccessful.setMaxRetries("1");
    untilSuccessful.setMillisBetweenRetries("100");
    untilSuccessful.initialise();
    untilSuccessful.start();

    final CoreEvent testEvent = eventBuilder(muleContext).message(of("queso")).build();
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
    untilSuccessful.setMillisBetweenRetries("100");
    untilSuccessful.initialise();
    untilSuccessful.start();

    final CoreEvent testEvent = eventBuilder(muleContext).message(of("queso")).build();
    CoreEvent response = untilSuccessful.process(testEvent);
    assertThat(getPayloadAsString(response.getMessage()), is("queso"));
    Map<String, Object> retryCtxContainer = ((InternalEvent) response).getInternalParameter(RETRY_CTX_INTERNAL_PARAMETER_KEY);
    assertThat(retryCtxContainer.isEmpty(), is(true));
  }

  @Test
  public void testRetryContextIsClearedAfterExhaustedScopeExecution() throws Exception {
    targetMessageProcessor.setNumberOfFailuresToSimulate(2);
    untilSuccessful.setMaxRetries("1");
    untilSuccessful.setMillisBetweenRetries("100");
    untilSuccessful.initialise();
    untilSuccessful.start();

    assertNoRetryContextAfterScopeExecutions(2);
  }

  @Test
  public void testRetryContextIsClearedAfterNestedExhaustedScopeExecution() throws Exception {
    untilSuccessful = buildNestedUntilSuccessful();
    targetMessageProcessor.setNumberOfFailuresToSimulate(4);
    untilSuccessful.setMaxRetries("1");
    untilSuccessful.setMillisBetweenRetries("100");
    untilSuccessful.initialise();
    untilSuccessful.start();

    assertNoRetryContextAfterScopeExecutions(4);
  }

  protected void assertNoRetryContextAfterScopeExecutions(int expectedExecutions) throws MuleException {
    final CoreEvent testEvent = eventBuilder(muleContext).message(of("queso")).build();
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
