/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.flow;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.AbstractProcessingStrategy;
import org.mule.runtime.core.processor.strategy.BlockingProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.DefaultFlowProcessingStrategyFactory;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class FlowProcessingStrategyConfigTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/config/spring/flow/flow-processing-strategies.xml";
  }

  @Test
  public void testDefault() throws Exception {
    assertThat(getFlowProcessingStrategyFactory("defaultFlow"), instanceOf(DefaultFlowProcessingStrategyFactory.class));
  }

  @Test
  public void testSynchronous() throws Exception {
    assertThat(getFlowProcessingStrategyFactory("synchronousFlow"),
               instanceOf(BlockingProcessingStrategyFactory.class));
  }

  @Test
  public void testCustom() throws Exception {
    ProcessingStrategyFactory processingStrategy = getFlowProcessingStrategyFactory("customProcessingStrategyFlow");
    assertThat(processingStrategy, instanceOf(CustomProcessingStrategyFactory.class));

    assertThat(((CustomProcessingStrategyFactory) processingStrategy).foo, is("bar"));
  }

  @Test
  public void testDefaultAsync() throws Exception {
    assertThat(getFlowProcessingStrategyFactory("defaultAsync"), instanceOf(DefaultFlowProcessingStrategyFactory.class));
  }

  private ProcessingStrategyFactory getFlowProcessingStrategyFactory(String flowName) throws Exception {
    Flow flow = (Flow) getFlowConstruct(flowName);
    return flow.getProcessingStrategyFactory();
  }

  public static class CustomProcessingStrategyFactory extends AbstractProcessingStrategy implements ProcessingStrategyFactory {

    String foo;

    public void setFoo(String foo) {
      this.foo = foo;
    }

    @Override
    public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
      return this;
    }
  }

}
