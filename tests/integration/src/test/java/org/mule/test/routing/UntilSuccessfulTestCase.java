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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.functional.functional.InvocationCountMessageProcessor.getNumberOfInvocationsFor;
import static org.mule.functional.junit4.TestLegacyMessageUtils.getExceptionPayload;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.retry.RetryPolicyExhaustedException;
import org.mule.runtime.core.util.store.AbstractPartitionedObjectStore;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class UntilSuccessfulTestCase extends AbstractIntegrationTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

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
    flowRunner("minimal-config").withPayload(payload).run();

    final List<Object> receivedPayloads = ponderUntilMessageCountReceivedByTargetMessageProcessor(1);
    assertThat(receivedPayloads, hasSize(1));
    assertThat(receivedPayloads.get(0), is(payload));
  }

  @Test
  public void testFullConfigurationMP() throws Exception {
    final String payload = RandomStringUtils.randomAlphanumeric(20);
    final Message response = flowRunner("full-config-with-mp").withPayload(payload).run().getMessage();
    assertThat(getPayloadAsString(response), is("ACK"));

    final List<Object> receivedPayloads = ponderUntilMessageCountReceivedByTargetMessageProcessor(3);
    assertThat(receivedPayloads, hasSize(3));
    for (int i = 0; i <= 2; i++) {
      assertThat(receivedPayloads.get(i), is(payload));
    }

    ponderUntilMessageCountReceivedByCustomMP(1);

    Throwable error = getExceptionPayload(CustomMP.getProcessedEvents().get(0).getMessage()).getException();
    assertThat(error, is(notNullValue()));
    assertThat(error.getCause(), instanceOf(RetryPolicyExhaustedException.class));
    assertThat(error.getCause().getMessage(),
               containsString("until-successful retries exhausted. Last exception message was: Failure expression positive when processing event"));

    assertThat(error.getCause().getCause(), instanceOf(MuleRuntimeException.class));
    assertThat(error.getCause().getMessage(),
               containsString("Failure expression positive when processing event"));
  }

  @Test
  public void testRetryOnEndpoint() throws Exception {
    final String payload = RandomStringUtils.randomAlphanumeric(20);
    flowRunner("retry-endpoint-config").withPayload(payload).run();

    final List<Object> receivedPayloads = ponderUntilMessageCountReceivedByTargetMessageProcessor(3);
    assertThat(receivedPayloads, hasSize(3));
    for (int i = 0; i <= 2; i++) {
      assertThat(receivedPayloads.get(i), is(payload));
    }
  }

  @Test
  public void executeSynchronously() throws Exception {
    final String payload = RandomStringUtils.randomAlphanumeric(20);
    expectedException.expectCause(instanceOf(RoutingException.class));
    flowRunner("synchronous").withPayload(payload).run();
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

  private List<Object> ponderUntilMessageCountReceivedByTargetMessageProcessor(final int expectedCount)
      throws InterruptedException {
    return ponderUntilMessageCountReceived(expectedCount, targetMessageProcessor);
  }

  private List<Object> ponderUntilMessageCountReceived(final int expectedCount, final FunctionalTestComponent ftc)
      throws InterruptedException {
    final List<Object> results = new ArrayList<>();

    new PollingProber(DEFAULT_TEST_TIMEOUT_SECS * 1000, 1000).check(new JUnitLambdaProbe(() -> {
      assertThat(ftc.getReceivedMessagesCount(), greaterThanOrEqualTo(expectedCount));
      return true;
    }));

    for (int i = 0; i < ftc.getReceivedMessagesCount(); i++) {
      results.add(ftc.getReceivedMessage(1 + i));
    }
    return results;
  }

  private void ponderUntilMessageCountReceivedByCustomMP(final int expectedCount) throws InterruptedException {
    new PollingProber(DEFAULT_TEST_TIMEOUT_SECS * 1000, 1000).check(new JUnitLambdaProbe(() -> {
      assertThat(CustomMP.getCount(), greaterThanOrEqualTo(expectedCount));
      return true;
    }));
  }

  static class CustomMP implements Processor {

    private static List<Event> processedEvents = new ArrayList<>();

    public static void clearCount() {
      processedEvents.clear();
    }

    public static int getCount() {
      return processedEvents.size();
    }

    public static List<Event> getProcessedEvents() {
      return processedEvents;
    }

    @Override
    public Event process(final Event event) throws MuleException {
      processedEvents.add(event);
      return null;
    }
  }

  static class WaitMeasure implements Processor {

    public static long totalWait;
    private long firstAttemptTime = 0;

    @Override
    public Event process(Event event) throws MuleException {
      if (firstAttemptTime == 0) {
        firstAttemptTime = System.currentTimeMillis();
      } else {
        totalWait = System.currentTimeMillis() - firstAttemptTime;
      }

      return event;
    }
  }
}
