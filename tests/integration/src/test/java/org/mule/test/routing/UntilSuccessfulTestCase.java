/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.functional.functional.InvocationCountMessageProcessor.getNumberOfInvocationsFor;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.runtime.api.message.Error;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.functional.junit4.runners.RunnerDelegateTo;
import org.mule.runtime.core.api.ExceptionPayload;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.retry.RetryPolicyExhaustedException;
import org.mule.runtime.core.util.store.AbstractPartitionedObjectStore;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class UntilSuccessfulTestCase extends AbstractIntegrationTestCase {

  private final String configFile;

  private FunctionalTestComponent targetMessageProcessor;

  @Parameterized.Parameters
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][] {{"until-successful-test.xml"}, {"until-successful-seconds-test.xml"}});
  }

  public UntilSuccessfulTestCase(String configFile) {
    this.configFile = configFile;
  }

  @Override
  protected String getConfigFile() {
    return configFile;
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    targetMessageProcessor = getFunctionalTestComponent("target-mp");

    final AbstractPartitionedObjectStore<Serializable> objectStore = muleContext.getRegistry().lookupObject("objectStore");
    objectStore.disposePartition("DEFAULT_PARTITION");
  }

  @Test
  public void testDefaultConfiguration() throws Exception {
    final String payload = RandomStringUtils.randomAlphanumeric(20);
    flowRunner("minimal-config").withPayload(payload).asynchronously().run();

    final List<Object> receivedPayloads = ponderUntilMessageCountReceivedByTargetMessageProcessor(1);
    assertThat(receivedPayloads, hasSize(1));
    assertThat(receivedPayloads.get(0), is(payload));
  }

  @Test
  public void testFullConfigurationMP() throws Exception {
    final String payload = RandomStringUtils.randomAlphanumeric(20);
    final MuleMessage response = flowRunner("full-config-with-mp").withPayload(payload).run().getMessage();
    assertThat(getPayloadAsString(response), is("ACK"));

    final List<Object> receivedPayloads = ponderUntilMessageCountReceivedByTargetMessageProcessor(3);
    assertThat(receivedPayloads, hasSize(3));
    for (int i = 0; i <= 2; i++) {
      assertThat(receivedPayloads.get(i), is(payload));
    }

    ponderUntilMessageCountReceivedByCustomMP(1);

    Error error = CustomMP.getProcessedEvents().get(0).getError();
    assertThat(error, is(notNullValue()));
    assertThat(error.getException(), instanceOf(RetryPolicyExhaustedException.class));
    assertThat(error.getException().getMessage(),
               containsString("until-successful retries exhausted. Last exception message was: Failure expression positive when processing event"));

    assertThat(error.getException().getCause(), instanceOf(MuleRuntimeException.class));
    assertThat(error.getException().getMessage(),
               containsString("Failure expression positive when processing event"));
  }

  @Test
  public void testRetryOnEndpoint() throws Exception {
    final String payload = RandomStringUtils.randomAlphanumeric(20);
    flowRunner("retry-endpoint-config").withPayload(payload).asynchronously().run();

    final List<Object> receivedPayloads = ponderUntilMessageCountReceivedByTargetMessageProcessor(3);
    assertThat(receivedPayloads, hasSize(3));
    for (int i = 0; i <= 2; i++) {
      assertThat(receivedPayloads.get(i), is(payload));
    }
  }

  @Test(expected = RoutingException.class)
  public void executeSynchronously() throws Exception {
    final String payload = RandomStringUtils.randomAlphanumeric(20);
    throw flowRunner("synchronous").withPayload(payload).runExpectingException();
  }

  @Test
  public void executeSynchronouslyDoingRetries() throws Exception {
    final String payload = RandomStringUtils.randomAlphanumeric(20);
    flowRunner("synchronous-with-retry").withPayload(payload).runExpectingException();
    assertThat(getNumberOfInvocationsFor("untilSuccessful"), is(4));
    assertThat(getNumberOfInvocationsFor("exceptionStrategy"), is(1));
  }

  /**
   * Verifies that the synchronous wait time is consistent with that requested
   */
  @Test
  public void measureSynchronousWait() throws Exception {
    final String payload = RandomStringUtils.randomAlphanumeric(20);
    flowRunner("measureSynchronousWait").withPayload(payload).runExpectingException();
    assertThat(WaitMeasure.totalWait >= 1000, is(true));
  }

  @Test
  public void executeAsynchronouslyDoingRetries() throws Exception {
    final String payload = RandomStringUtils.randomAlphanumeric(20);
    final int expectedCounterExecutions = 4;
    final int expectedCounterInExceptionStrategyExecutions = 1;
    flowRunner("asynchronous-using-threading-profile").withPayload(payload).run();
    new PollingProber(10000, 100).check(new Probe() {

      private int executionOfCountInUntilSuccessful;
      private int executionOfCountInExceptionStrategy;

      @Override
      public boolean isSatisfied() {
        executionOfCountInUntilSuccessful = getNumberOfInvocationsFor("untilSuccessful2");
        executionOfCountInExceptionStrategy = getNumberOfInvocationsFor("exceptionStrategy2");
        return executionOfCountInUntilSuccessful == expectedCounterExecutions
            && executionOfCountInExceptionStrategy == expectedCounterInExceptionStrategyExecutions;
      }

      @Override
      public String describeFailure() {
        return String.format(
                             "Expecting %d executions of counter in until-successful and got %d \n "
                                 + "Expecting %d execution of counter in exception strategy and got %d",
                             expectedCounterExecutions, executionOfCountInUntilSuccessful,
                             expectedCounterInExceptionStrategyExecutions, executionOfCountInExceptionStrategy);
      }
    });
  }

  @Test
  public void executeAsynchronouslyDoingRetriesAfterRestart() throws Exception {
    Flow flow = (Flow) getFlowConstruct("asynchronous-using-threading-profile");
    flow.stop();
    flow.start();
    executeAsynchronouslyDoingRetries();

  }

  private List<Object> ponderUntilMessageCountReceivedByTargetMessageProcessor(final int expectedCount)
      throws InterruptedException {
    return ponderUntilMessageCountReceived(expectedCount, targetMessageProcessor);
  }

  private List<Object> ponderUntilMessageCountReceived(final int expectedCount, final FunctionalTestComponent ftc)
      throws InterruptedException {
    final List<Object> results = new ArrayList<Object>();

    while (ftc.getReceivedMessagesCount() < expectedCount) {
      Thread.yield();
      Thread.sleep(100L);
    }

    for (int i = 0; i < ftc.getReceivedMessagesCount(); i++) {
      results.add(ftc.getReceivedMessage(1 + i));
    }
    return results;
  }

  private void ponderUntilMessageCountReceivedByCustomMP(final int expectedCount) throws InterruptedException {
    while (CustomMP.getCount() < expectedCount) {
      Thread.yield();
      Thread.sleep(100L);
    }
  }

  static class CustomMP implements MessageProcessor {

    private static List<MuleEvent> processedEvents = new ArrayList<>();

    public static void clearCount() {
      processedEvents.clear();
    }

    public static int getCount() {
      return processedEvents.size();
    }

    public static List<MuleEvent> getProcessedEvents() {
      return processedEvents;
    }

    @Override
    public MuleEvent process(final MuleEvent event) throws MuleException {
      processedEvents.add(event);
      return null;
    }
  }

  static class WaitMeasure implements MessageProcessor {

    public static long totalWait;
    private long firstAttemptTime = 0;

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
      if (firstAttemptTime == 0) {
        firstAttemptTime = System.currentTimeMillis();
      } else {
        totalWait = System.currentTimeMillis() - firstAttemptTime;
      }

      return event;
    }
  }
}
