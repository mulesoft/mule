/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.core.api.scheduler.ThreadType.CPU_INTENSIVE;
import static org.mule.runtime.core.api.scheduler.ThreadType.CPU_LIGHT;
import static org.mule.runtime.core.api.scheduler.ThreadType.IO;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.processor.strategy.DefaultFlowProcessingStrategyFactory.DefaultFlowProcessingStrategy;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.tck.testmodels.mule.TestTransaction;

import ru.yandex.qatools.allure.annotations.Description;

public class DefaultProcessingStrategyTestCase extends ProactorProcessingStrategyTestCase {

  public DefaultProcessingStrategyTestCase(boolean reactive) {
    super(reactive);
  }

  @Override
  protected ProcessingStrategy createProcessingStrategy(MuleContext muleContext) {
    return new DefaultFlowProcessingStrategy(() -> cpuLight, () -> blocking, () -> cpuIntensive,
                                             scheduler -> {
                                             },
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
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_LIGHT.name())).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(IO.name())).count(), equalTo(0l));
    assertThat(threads.stream().filter(name -> name.startsWith(CPU_INTENSIVE.name())).count(), equalTo(0l));
  }

}
