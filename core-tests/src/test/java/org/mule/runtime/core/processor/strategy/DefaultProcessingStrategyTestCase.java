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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.core.processor.strategy.AbstractRingBufferProcessingStrategyFactory.DEFAULT_BUFFER_SIZE;
import static org.mule.runtime.core.processor.strategy.AbstractRingBufferProcessingStrategyFactory.DEFAULT_SUBSCRIBER_COUNT;
import static org.mule.runtime.core.processor.strategy.AbstractRingBufferProcessingStrategyFactory.DEFAULT_WAIT_STRATEGY;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.processor.strategy.DefaultFlowProcessingStrategyFactory.DefaultFlowProcessingStrategy;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.tck.testmodels.mule.TestTransaction;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Processing Strategies")
@Stories("Default Processing Strategy (used when no processing strategy is configured)")
public class DefaultProcessingStrategyTestCase extends ProactorProcessingStrategyTestCase {

  public DefaultProcessingStrategyTestCase(Mode mode) {
    super(mode);
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext, String schedulersNamePrefix) {
    return new DefaultFlowProcessingStrategy(() -> cpuLight,
                                             () -> blocking,
                                             () -> cpuIntensive,
                                             scheduler -> {
                                             },
                                             MAX_VALUE,
                                             () -> blocking,
                                             DEFAULT_BUFFER_SIZE,
                                             DEFAULT_SUBSCRIBER_COUNT,
                                             DEFAULT_WAIT_STRATEGY,
                                             muleContext);
  }

  @Override
  @Description("Unlike with the MultiReactorProcessingStrategy, the DefaultFlowProcessingStrategy does not fail if a transaction "
      + "is active, but rather executes these events synchonrously in the caller thread transparently.")
  public void tx() throws Exception {
    flow.setMessageProcessors(asList(cpuLightProcessor, cpuIntensiveProcessor, blockingProcessor));
    flow.initialise();
    flow.start();

    TransactionCoordination.getInstance().bindTransaction(new TestTransaction(muleContext));

    process(flow, testEvent());

    assertThat(threads.size(), equalTo(1));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT)).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(IO)).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE)).count(), equalTo(0l));
  }

}
