/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.Flow.Builder;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class FlowProcessingStrategyTestCase extends AbstractMuleTestCase {

  protected MuleContext muleContext = mockContextWithServices();

  @Mock
  private MuleConfiguration configuration;

  private Flow flow;

  @Before
  public void before() throws RegistrationException {
    when(muleContext.getConfiguration()).thenReturn(configuration);
    createFlow(null);
  }

  @Test
  public void fixedProcessingStrategyIsHonoured() throws Exception {
    ProcessingStrategyFactory processingStrategyFactory = mock(ProcessingStrategyFactory.class);
    ProcessingStrategy processingStrategy = mock(ProcessingStrategy.class);
    when(processingStrategyFactory.create(any(), any())).thenReturn(processingStrategy);
    createFlow(processingStrategyFactory);
    flow.initialise();

    assertThat(flow.getProcessingStrategy(), is(sameInstance(processingStrategy)));
  }

  @Test
  public void defaultProcessingStrategyInConfigIsHonoured() throws Exception {
    ProcessingStrategy processingStrategy = mock(ProcessingStrategy.class);
    ProcessingStrategyFactory processingStrategyFactory = mock(ProcessingStrategyFactory.class);
    when(processingStrategyFactory.create(any(), any())).thenReturn(processingStrategy);
    when(configuration.getDefaultProcessingStrategyFactory()).thenReturn(processingStrategyFactory);

    createFlow(null);
    flow.initialise();
    assertThat(flow.getProcessingStrategy(), is(sameInstance(processingStrategy)));
  }

  @Test
  public void fixedProcessingStrategyTakesPrecedenceOverConfig() throws Exception {
    ProcessingStrategyFactory configProcessingStrategyFactory = mock(ProcessingStrategyFactory.class);
    when(configuration.getDefaultProcessingStrategyFactory()).thenReturn(configProcessingStrategyFactory);

    ProcessingStrategyFactory processingStrategyFactory = mock(ProcessingStrategyFactory.class);
    ProcessingStrategy processingStrategy = mock(ProcessingStrategy.class);
    when(processingStrategyFactory.create(any(), any())).thenReturn(processingStrategy);
    createFlow(processingStrategyFactory);
    flow.initialise();

    assertThat(flow.getProcessingStrategy(), is(sameInstance(processingStrategy)));
  }

  @Test
  public void createDefaultProcessingStrategyIfNoneSpecified() throws Exception {
    flow.initialise();
    assertThat(flow.getProcessingStrategy(),
               is(instanceOf(new TransactionAwareWorkQueueProcessingStrategyFactory().getProcessingStrategyType())));
  }

  @Test
  public void processingStrategySetBySystemPropertyOverridesDefault() throws Exception {
    testWithSystemProperty(ProcessingStrategyFactory.class.getName(),
                           TestProcessingStrategyFactory.class.getName(), () -> {
                             MuleConfiguration muleConfiguration = new DefaultMuleConfiguration();
                             when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
                             createFlow(null);
                             assertEquals(flow.getProcessingStrategy().getClass(), TestProcessingStrategy.class);
                           });
  }


  public static class TestProcessingStrategyFactory extends AbstractProcessingStrategyFactory {

    public TestProcessingStrategyFactory() {}

    @Override
    public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
      return new TestProcessingStrategy();
    }
  }

  private static class TestProcessingStrategy extends AbstractProcessingStrategy {
  }

  private void createFlow(ProcessingStrategyFactory configProcessingStrategyFactory) {
    Builder flowBuilder = builder("test", muleContext);
    if (configProcessingStrategyFactory != null) {
      flowBuilder = flowBuilder.processingStrategyFactory(configProcessingStrategyFactory);
    }

    flow = flowBuilder.build();
  }
}
