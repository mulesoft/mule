/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_QUEUE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SECURITY_MANAGER;
import static org.mule.runtime.core.api.processor.strategy.AsyncProcessingStrategyFactory.DEFAULT_MAX_CONCURRENCY;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.util.queue.QueueConfiguration;
import org.mule.runtime.core.api.util.queue.QueueManager;
import org.mule.runtime.core.api.util.queue.QueueSession;
import org.mule.runtime.core.api.config.builders.DefaultsConfigurationBuilder;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.internal.construct.DefaultFlowBuilder;
import org.mule.runtime.core.internal.security.DefaultMuleSecurityManager;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@SmallTest
public class QueueManagerLifecycleOrderTestCase extends AbstractMuleTestCase {

  private List<Object> startStopOrder = new ArrayList<>();
  private RecordingTQM rtqm = new RecordingTQM();

  @Rule
  public TestServicesConfigurationBuilder testServicesConfigurationBuilder = new TestServicesConfigurationBuilder();

  private MuleContext muleContext;

  @Before
  public void before() throws InitialisationException, ConfigurationException {
    muleContext = new DefaultMuleContextFactory().createMuleContext(testServicesConfigurationBuilder,
                                                                    new QueueManagerOnlyConfigurationBuilder());
    testServicesConfigurationBuilder.configure(muleContext);
  }

  @After
  public void after() {
    muleContext.dispose();
  }

  @Test
  public void testStartupOrder() throws Exception {
    FlowConstruct fc = new RecordingFlow("dummy", muleContext);
    muleContext.getRegistry().registerFlowConstruct(fc);
    muleContext.start();
    muleContext.stop();
    assertEquals(4, startStopOrder.size());
    assertSame(rtqm, startStopOrder.get(0));
    assertSame(fc, startStopOrder.get(1));
    assertSame(fc, startStopOrder.get(2));
    assertSame(rtqm, startStopOrder.get(3));

  }

  private class RecordingTQM implements QueueManager {

    @Override
    public void start() throws MuleException {
      startStopOrder.add(this);
    }

    @Override
    public void stop() throws MuleException {
      startStopOrder.add(this);
    }

    @Override
    public QueueSession getQueueSession() {
      throw notImplementedException();
    }

    @Override
    public void setDefaultQueueConfiguration(QueueConfiguration config) {
      throw notImplementedException();
    }

    @Override
    public void setQueueConfiguration(String queueName, QueueConfiguration config) {
      throw notImplementedException();
    }

    @Override
    public Optional<QueueConfiguration> getQueueConfiguration(String queueName) {
      throw notImplementedException();
    }

    private NotImplementedException notImplementedException() {
      return new NotImplementedException("This is test code");
    }
  }

  private class RecordingFlow extends DefaultFlowBuilder.DefaultFlow {

    public RecordingFlow(String name, MuleContext muleContext) {
      super(name, muleContext, null, emptyList(), empty(), empty(), INITIAL_STATE_STARTED, DEFAULT_MAX_CONCURRENCY, null);
    }

    @Override
    public void doStart() throws MuleException {
      startStopOrder.add(this);
    }

    @Override
    public void doStop() throws MuleException {
      startStopOrder.add(this);
    }
  }

  private class QueueManagerOnlyConfigurationBuilder extends DefaultsConfigurationBuilder {

    @Override
    protected void doConfigure(MuleContext muleContext) throws Exception {
      muleContext.getRegistry().registerObject(OBJECT_QUEUE_MANAGER, rtqm);
      muleContext.getRegistry().registerObject(OBJECT_SECURITY_MANAGER, new DefaultMuleSecurityManager());

    }
  }
}
