/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.getInstance;
import static org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategyTestCase.Mode.SOURCE;
import static org.mule.tck.util.MuleContextUtils.getNotificationDispatcher;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.DEFAULT;
import static reactor.util.concurrent.Queues.XS_BUFFER_SIZE;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategyTestCase.TransactionAwareProcessingStrategyTestCase;
import org.mule.runtime.core.internal.processor.strategy.ProactorStreamEmitterProcessingStrategyFactory.ProactorStreamEmitterProcessingStrategy;
import org.mule.tck.testmodels.mule.TestTransaction;

import java.util.Collection;

import org.junit.After;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@RunWith(Parameterized.class)
@Feature(PROCESSING_STRATEGIES)
@Story(DEFAULT)
public class TransactionAwareProactorStreamEmitterProcessingStrategyTestCase
    extends ProactorStreamEmitterProcessingStrategyTestCase
    implements TransactionAwareProcessingStrategyTestCase {

  public TransactionAwareProactorStreamEmitterProcessingStrategyTestCase(Mode mode) {
    super(mode);
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Mode> parameters() {
    return asList(new Mode[] {Mode.FLOW, SOURCE});
  }

  @After
  public void cleanUpTx() {
    getInstance().rollbackCurrentTransaction();
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix) {
    return new TransactionAwareStreamEmitterProcessingStrategyDecorator(
                                                                        new ProactorStreamEmitterProcessingStrategy(XS_BUFFER_SIZE,
                                                                                                                    2,
                                                                                                                    () -> cpuLight,
                                                                                                                    () -> cpuLight,
                                                                                                                    () -> blocking,
                                                                                                                    () -> cpuIntensive,
                                                                                                                    MAX_VALUE,
                                                                                                                    MAX_VALUE,
                                                                                                                    false,
                                                                                                                    false,
                                                                                                                    () -> muleContext
                                                                                                                        .getConfiguration()
                                                                                                                        .getShutdownTimeout()));
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix,
                                                        int maxConcurrency) {
    return new TransactionAwareStreamEmitterProcessingStrategyDecorator(
                                                                        new ProactorStreamEmitterProcessingStrategy(XS_BUFFER_SIZE,
                                                                                                                    2,
                                                                                                                    () -> cpuLight,
                                                                                                                    () -> cpuLight,
                                                                                                                    () -> blocking,
                                                                                                                    () -> cpuIntensive,
                                                                                                                    maxConcurrency,
                                                                                                                    maxConcurrency,
                                                                                                                    true,
                                                                                                                    false,
                                                                                                                    () -> muleContext
                                                                                                                        .getConfiguration()
                                                                                                                        .getShutdownTimeout()));
  }

  @Override
  @Description("Unlike with the MultiReactorProcessingStrategy, the TransactionAwareEmitterProcessingStrategy does not fail if a transaction "
      + "is active, but rather executes these events synchronously in the caller thread transparently.")
  public void tx() throws Exception {
    flow = flowBuilder.get().processors(cpuLightProcessor, cpuIntensiveProcessor, blockingProcessor).build();
    flow.initialise();
    flow.start();

    getInstance()
        .bindTransaction(new TestTransaction("appName", getNotificationDispatcher(muleContext)));

    processFlow(testEvent());

    assertThat(threads, hasSize(equalTo(1)));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }
}
