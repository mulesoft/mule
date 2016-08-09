/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.config.builders.DefaultsConfigurationBuilder;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.context.DefaultMuleContextFactory;
import org.mule.runtime.core.security.MuleSecurityManager;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SmallTest
public class QueueManagerLifecycleOrderTestCase extends AbstractMuleTestCase {

  private List<Object> startStopOrder = new ArrayList<>();
  private RecordingTQM rtqm = new RecordingTQM();

  private MuleContext muleContext;

  @Before
  public void before() throws InitialisationException, ConfigurationException {
    muleContext = new DefaultMuleContextFactory().createMuleContext(new QueueManagerOnlyConfigurationBuilder());
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
      throw new NotImplementedException();
    }

    @Override
    public void setDefaultQueueConfiguration(QueueConfiguration config) {
      throw new NotImplementedException();
    }

    @Override
    public void setQueueConfiguration(String queueName, QueueConfiguration config) {
      throw new NotImplementedException();
    }
  }

  private class RecordingFlow extends Flow {

    public RecordingFlow(String name, MuleContext muleContext) {
      super(name, muleContext);
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
      muleContext.getRegistry().registerObject(MuleProperties.OBJECT_QUEUE_MANAGER, rtqm);
      muleContext.getRegistry().registerObject(MuleProperties.OBJECT_SECURITY_MANAGER, new MuleSecurityManager());

    }
  }
}
