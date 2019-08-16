/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.Integer.MAX_VALUE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mule.runtime.core.internal.processor.strategy.AbstractStreamWorkQueueProcessingStrategyFactory.DEFAULT_WAIT_STRATEGY;
import static org.mule.tck.util.MuleContextUtils.getNotificationDispatcher;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static reactor.util.concurrent.Queues.XS_BUFFER_SIZE;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategyTestCase.TransactionAwareProcessingStrategyTestCase;
import org.mule.runtime.core.internal.processor.strategy.TransactionAwareProactorStreamWorkQueueProcessingStrategyFactory.TransactionAwareProactorStreamWorkQueueProcessingStrategy;
import org.mule.tck.testmodels.mule.TestTransaction;

import org.junit.After;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;

@Feature(PROCESSING_STRATEGIES)
public class TransactionAwareProactorStreamWorkQueueProcessingStrategyTestCase
    extends ProactorStreamWorkQueueProcessingStrategyTestCase
    implements TransactionAwareProcessingStrategyTestCase {

  public TransactionAwareProactorStreamWorkQueueProcessingStrategyTestCase(AbstractProcessingStrategyTestCase.Mode mode) {
    super(mode);
  }

  @After
  public void cleanUpTx() {
    TransactionCoordination.getInstance().rollbackCurrentTransaction();
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix) {
    return new TransactionAwareProactorStreamWorkQueueProcessingStrategy(() -> blocking,
                                                                         XS_BUFFER_SIZE,
                                                                         1,
                                                                         DEFAULT_WAIT_STRATEGY,
                                                                         () -> cpuLight,
                                                                         () -> blocking,
                                                                         () -> cpuIntensive,
                                                                         MAX_VALUE, true);
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix,
                                                        int maxConcurrency) {
    return new TransactionAwareProactorStreamWorkQueueProcessingStrategy(() -> blocking,
                                                                         XS_BUFFER_SIZE,
                                                                         1,
                                                                         DEFAULT_WAIT_STRATEGY,
                                                                         () -> cpuLight,
                                                                         () -> blocking,
                                                                         () -> cpuIntensive,
                                                                         maxConcurrency, true);
  }

  @Override
  @Description("Unlike with the MultiReactorProcessingStrategy, the TransactionAwareWorkQueueProcessingStrategy does not fail if a transaction "
      + "is active, but rather executes these events synchronously in the caller thread transparently.")
  public void tx() throws Exception {
    flow = flowBuilder.get().processors(cpuLightProcessor, cpuIntensiveProcessor, blockingProcessor).build();
    flow.initialise();
    flow.start();

    TransactionCoordination.getInstance()
        .bindTransaction(new TestTransaction("appName", getNotificationDispatcher(muleContext), 5));

    processFlow(testEvent());

    assertThat(threads, hasSize(equalTo(1)));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }
}
