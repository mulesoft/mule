/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.streaming;

import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.functional.junit4.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mule.tck.junit4.matcher.Eventually.eventually;
import static org.mule.test.allure.AllureConstants.ForkJoinStrategiesFeature.FORK_JOIN_STRATEGIES;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.BYTES_STREAMING;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.apache.commons.lang3.RandomStringUtils.insecure;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.isA;
import static org.junit.Assert.assertThrows;

import org.mule.runtime.api.exception.ComposedErrorException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.event.EventContextService;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.exception.MessagingException;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.inject.Inject;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@Features({@Feature(STREAMING), @Feature(FORK_JOIN_STRATEGIES)})
@Story(BYTES_STREAMING)
@RunnerDelegateTo(Parameterized.class)
public class ScatterGatherTimeoutWithBytesStreamingExtensionTestCase extends AbstractExtensionFunctionalTestCase {

  private static final String DATA = insecure().nextAlphabetic(2048);

  @Inject
  private EventContextService eventContextService;

  @Rule
  public SystemProperty configName;

  @Parameters(name = "config used: `{0}`")
  public static Iterable<String> configs() {
    return asList("drStrange", "poolingDrStrange");
  }

  public ScatterGatherTimeoutWithBytesStreamingExtensionTestCase(String configName) {
    this.configName = new SystemProperty("configName", configName);
  }

  @Override
  protected String getConfigFile() {
    return "streaming/scatter-gather-bytes-streaming-extension-config.xml";
  }

  @Test
  @Issue("W-16941297")
  @Description("A Scatter Gather router will time out while an operation is still executing. The operation then finishes and generates a stream which should eventually be closed.")
  public void whenScatterGatherTimesOutThenStreamsAreNotLeaked() throws InterruptedException {
    runScatterGatherFlowAndAwaitStreamClosed("scatterGatherWithTimeout");
    // Check that no EventContexts are leaked
    assertThat(eventContextService.getCurrentlyActiveFlowStacks(), is(eventually(empty())));
  }

  @Test
  @Issue("W-16941297")
  @Description("A Scatter Gather router with collect-list strategy will time out while an operation is still executing. The operation then finishes and generates a stream which should eventually be closed.")
  public void whenScatterGatherWithCollectListTimesOutThenStreamsAreNotLeaked() throws InterruptedException {
    runScatterGatherFlowAndAwaitStreamClosed("scatterGatherWithTimeoutCollectList");
    // Check that no EventContexts are leaked
    assertThat(eventContextService.getCurrentlyActiveFlowStacks(), is(eventually(empty())));
  }

  @Test
  @Issue("W-16941297")
  @Description("A Scatter Gather router will time out while an operation inside a referenced flow is still executing. The operation then finishes and generates a stream which should eventually be closed.")
  public void whenScatterGatherWithFlowRefTimesOutThenStreamsAreNotLeaked() throws InterruptedException {
    runScatterGatherFlowAndAwaitStreamClosed("scatterGatherWithTimeoutFlowRef");
    // Check that no EventContexts are leaked
    assertThat(eventContextService.getCurrentlyActiveFlowStacks(), is(eventually(empty())));
  }

  @Test
  @Issue("W-16941297")
  @Description("A Scatter Gather router will time out while an operation inside another nested Scatter Gather is still executing. The operation then finishes and generates a stream which should eventually be closed.")
  public void whenScatterGatherWithNestedTimesOutThenStreamsAreNotLeaked() throws InterruptedException {
    runScatterGatherFlowAndAwaitStreamClosed("scatterGatherWithNestedRoute");
    // Check that no EventContexts are leaked
    assertThat(eventContextService.getCurrentlyActiveFlowStacks(), is(eventually(empty())));
  }

  @Test
  @Issue("W-16941297")
  public void scatterGatherTimeoutStress() throws InterruptedException, ExecutionException {
    String flowName = "scatterGatherWithTimeout";
    runScatterGatherFlowAndAwaitStreamClosed(flowName);
    ExecutorService executorService = newFixedThreadPool(3);
    List<Future<?>> futures = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      futures.add(executorService.submit(() -> {
        try {
          runScatterGatherFlowAndAwaitStreamClosed(flowName);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }));
    }

    for (Future<?> future : futures) {
      future.get();
    }
    executorService.shutdown();
    assertThat(executorService.awaitTermination(10, SECONDS), is(true));
    runScatterGatherFlowAndAwaitStreamClosed(flowName);
    runScatterGatherFlowAndAwaitStreamClosed(flowName);

    // Check that no EventContexts are leaked
    assertThat(eventContextService.getCurrentlyActiveFlowStacks(), is(eventually(empty()).atMostIn(20, SECONDS)));
  }

  private void runScatterGatherFlowAndAwaitStreamClosed(String flowName) throws InterruptedException {
    CountDownLatch sgTimedOutLatch = new CountDownLatch(1);
    CountDownLatch pagingProviderClosedLatch = new CountDownLatch(1);
    MessagingException e = assertThrows(MessagingException.class, () -> flowRunner(flowName)
        .withPayload(singletonList(DATA))
        .withVariable("latch", sgTimedOutLatch)
        .withVariable("providerClosedLatch", pagingProviderClosedLatch)
        .run());

    // Control test that the execution really ended with timeout
    assertThat(e,
               hasCause(allOf(isA(ComposedErrorException.class),
                              hasMessage(containsString("Route 1: java.util.concurrent.TimeoutException: "
                                  + "Timeout while processing route/part: '1'")))));

    // If we are here it means the Scatter Gather has already timed out, so now we allow the operation to proceed
    sgTimedOutLatch.countDown();

    // And wait until the paging provider is closed
    pagingProviderClosedLatch.await();
  }


  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }

  public static class AssertPayloadIsIteratorProvider implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      assertThat(event.getMessage().getPayload().getValue(), instanceOf(CursorIteratorProvider.class));
      return event;
    }
  }

}
