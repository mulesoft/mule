/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mule.runtime.core.processor.strategy.AbstractStreamProcessingStrategyFactory.DEFAULT_BUFFER_SIZE;
import static org.mule.runtime.core.processor.strategy.AbstractStreamProcessingStrategyFactory.DEFAULT_SUBSCRIBER_COUNT;
import static org.mule.runtime.core.processor.strategy.AbstractStreamProcessingStrategyFactory.DEFAULT_WAIT_STRATEGY;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.DEFAULT;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.processor.strategy.TransactionAwareProactorStreamProcessingStrategyFactory.TransactionAwareProactorStreamProcessingStrategy;
import org.mule.tck.junit4.AbstractReactiveProcessorTestCase;
import org.mule.tck.testmodels.mule.TestTransaction;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(PROCESSING_STRATEGIES)
@Stories(DEFAULT)
public class TransactionAwareProactorStreamProcessingStrategyTestCase extends ProactorStreamProcessingStrategyTestCase {

  public TransactionAwareProactorStreamProcessingStrategyTestCase(AbstractReactiveProcessorTestCase.Mode mode) {
    super(mode);
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix) {
    return new TransactionAwareProactorStreamProcessingStrategy(() -> blocking,
                                                                DEFAULT_BUFFER_SIZE,
                                                                DEFAULT_SUBSCRIBER_COUNT,
                                                                DEFAULT_WAIT_STRATEGY,
                                                                () -> cpuLight,
                                                                () -> blocking,
                                                                () -> cpuIntensive,
                                                                MAX_VALUE);
  }

  @Override
  @Description("Unlike with the MultiReactorProcessingStrategy, the TransactionAwareWorkQueueProcessingStrategy does not fail if a transaction "
      + "is active, but rather executes these events synchronously in the caller thread transparently.")
  public void tx() throws Exception {
    flow.setMessageProcessors(asList(cpuLightProcessor, cpuIntensiveProcessor, blockingProcessor));
    flow.initialise();
    flow.start();

    TransactionCoordination.getInstance().bindTransaction(new TestTransaction(muleContext));

    process(flow, testEvent());

    assertThat(threads, hasSize(equalTo(1)));
    assertThat(threads, not(hasItem(startsWith(CPU_LIGHT))));
    assertThat(threads, not(hasItem(startsWith(IO))));
    assertThat(threads, not(hasItem(startsWith(CPU_INTENSIVE))));
    assertThat(threads, not(hasItem(startsWith(CUSTOM))));
  }

}
