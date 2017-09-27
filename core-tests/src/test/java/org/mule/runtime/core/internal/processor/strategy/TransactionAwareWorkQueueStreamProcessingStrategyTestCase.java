/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.DEFAULT_SUBSCRIBER_COUNT;
import static org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.DEFAULT_WAIT_STRATEGY;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.WORK_QUEUE;
import static reactor.util.concurrent.Queues.XS_BUFFER_SIZE;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.processor.strategy.TransactionAwareWorkQueueStreamProcessingStrategyFactory.TransactionAwareWorkQueueStreamProcessingStrategy;
import org.mule.tck.testmodels.mule.TestTransaction;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Ignore;

@Feature(PROCESSING_STRATEGIES)
@Story(WORK_QUEUE)
@Ignore("MULE-13478")
public class TransactionAwareWorkQueueStreamProcessingStrategyTestCase extends WorkQueueStreamProcessingStrategyTestCase {

  public TransactionAwareWorkQueueStreamProcessingStrategyTestCase(Mode mode) {
    super(mode);
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix) {
    return new TransactionAwareWorkQueueStreamProcessingStrategy(() -> blocking,
                                                                 XS_BUFFER_SIZE,
                                                                 DEFAULT_SUBSCRIBER_COUNT,
                                                                 DEFAULT_WAIT_STRATEGY,
                                                                 () -> blocking,
                                                                 4);
  }

  @Override
  @Description("Unlike with the MultiReactorProcessingStrategy, the TransactionAwareWorkQueueProcessingStrategy does not fail if a transaction "
      + "is active, but rather executes these events synchronously in the caller thread transparently.")
  public void tx() throws Exception {
    flow = flowBuilder.get().processors(cpuLightProcessor, cpuIntensiveProcessor, blockingProcessor).build();
    flow.initialise();
    flow.start();

    TransactionCoordination.getInstance().bindTransaction(new TestTransaction(muleContext));

    processFlow(testEvent());

    assertThat(threads, hasSize(equalTo(1)));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

}
