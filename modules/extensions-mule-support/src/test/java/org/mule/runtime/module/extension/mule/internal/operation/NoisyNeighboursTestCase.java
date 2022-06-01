/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.operation;

import static org.mule.runtime.core.privileged.processor.MessageProcessors.createDefaultProcessingStrategyFactory;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getDefaultProcessingStrategyFactory;
import static org.mule.tck.junit4.matcher.Eventually.eventually;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.mule.extension.mule.testing.processing.strategies.test.api.BarrierProvider;
import org.mule.extension.mule.testing.processing.strategies.test.api.BarrierProvider.Barrier;
import org.mule.functional.api.flow.FlowRunner;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.processor.strategy.AsyncProcessingStrategyFactory;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import javax.inject.Inject;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(REUSE)
@Story(OPERATIONS)
public class NoisyNeighboursTestCase extends MuleArtifactFunctionalTestCase {

  private static final PollingProber prober = new PollingProber(RECEIVE_TIMEOUT, 100);

  @Inject
  private BarrierProvider barrierProvider;

  @Override
  protected DefaultMuleConfiguration createMuleConfiguration() {
    DefaultMuleConfiguration configuration = super.createMuleConfiguration();
    ProcessingStrategyFactory factory = createDefaultProcessingStrategyFactory();
    setMaxConcurrency(factory, 1);
    configuration.setDefaultProcessingStrategyFactory(factory);
    return configuration;
  }

  @Override
  protected String getConfigFile() {
    return "noisy-neighbours-test-config.xml";
  }

  @Test
  public void callersAreNotNoisyNeighbours() throws Exception {
    try (Async caller1 = new Async(new FlowRunnerRunnable("caller1"))) {
      try (Async caller2 = new Async(new FlowRunnerRunnable("caller2"))) {
        Barrier barrier = barrierProvider.get("sharedBarrier");
        prober.check(new JUnitLambdaProbe(() -> {
          assertThat(barrier.blockedThreads(), is(2));
          return true;
        }));
        barrier.release();
      }
    }
  }

  private static void setMaxConcurrency(ProcessingStrategyFactory processingStrategyFactory, int maxConcurrency) {
    ((AsyncProcessingStrategyFactory) processingStrategyFactory).setMaxConcurrency(maxConcurrency);
  }

  private static class Async implements AutoCloseable {

    private final Thread thread;

    Async(Runnable runnable) {
      thread = new Thread(runnable);
      thread.start();
    }

    @Override
    public void close() throws Exception {
      thread.join(RECEIVE_TIMEOUT);
    }
  }

  private class FlowRunnerRunnable implements Runnable {

    private final FlowRunner flowRunner;

    private FlowRunnerRunnable(String flowName) {
      this.flowRunner = flowRunner(flowName);
    }

    @Override
    public void run() {
      try {
        flowRunner.run();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
