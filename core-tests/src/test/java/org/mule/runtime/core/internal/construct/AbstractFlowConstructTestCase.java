/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.util.ObjectUtils;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public abstract class AbstractFlowConstructTestCase extends AbstractMuleContextTestCase {

  protected static class DirectInboundMessageSource extends AbstractComponent implements MessageSource {

    private Processor listener;

    @Override
    public void setListener(Processor listener) {
      this.listener = listener;
    }

    public Processor getListener() {
      return listener;
    }

    @Override
    public String toString() {
      return ObjectUtils.toString(this);
    }
  }

  protected DirectInboundMessageSource directInboundMessageSource;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    directInboundMessageSource = new DirectInboundMessageSource();
  }

  protected abstract AbstractFlowConstruct getFlowConstruct() throws Exception;

  protected abstract AbstractFlowConstruct getStoppedFlowConstruct() throws Exception;

  @Test
  public void testStart() throws Exception {
    try {
      getFlowConstruct().start();
      fail("Exception expected: Cannot start an uninitialised service");
    } catch (final Exception e) {
      // expected
    }

    getFlowConstruct().initialise();
    getFlowConstruct().start();

    try {
      getFlowConstruct().initialise();
      fail("Exception expected: Cannot initialise an already initialised service");
    } catch (final IllegalStateException e) {
      // expected
    }
    getFlowConstruct().dispose();

  }

  @Test
  public void testStop() throws Exception {
    assertFalse(getFlowConstruct().isStarted());

    try {
      getFlowConstruct().stop();
      fail("Exception expected: Cannot stop an uninitialised service");
    } catch (final IllegalStateException e) {
      // expected
    }

    getFlowConstruct().initialise();
    assertFalse(getFlowConstruct().isStarted());

    // Can stop a service that is not started
    getFlowConstruct().stop();

    assertFalse(getFlowConstruct().isStarted());
    getFlowConstruct().start();
    assertTrue(getFlowConstruct().isStarted());
    getFlowConstruct().stop();
    assertFalse(getFlowConstruct().isStarted());
    try {
      getFlowConstruct().stop();
      fail("Exception expected: Cannot stop a service that is not started");
    } catch (final IllegalStateException e) {
      // expected
    }
    assertFalse(getFlowConstruct().isStarted());
    getFlowConstruct().dispose();

  }

  @Test
  public void testDispose() throws Exception {
    assertFalse(getFlowConstruct().isStarted());
    getFlowConstruct().dispose();

    try {
      getFlowConstruct().dispose();
      fail("Exception expected: Cannot dispose a service that is already disposed");
    } catch (final IllegalStateException e) {
      // expected
    }

    try {
      getFlowConstruct().initialise();
      fail("Exception expected: Cannot invoke initialise (or any lifecycle) on an object once it is disposed");
    } catch (final IllegalStateException e) {
      // expected
    }
  }

  @Test
  public void testRegisterUnregister() throws MuleException, Exception {
    FlowConstruct construct = getFlowConstruct();
    ((MuleContextWithRegistries) muleContext).getRegistry().registerFlowConstruct(construct);
    assertNotNull(((MuleContextWithRegistries) muleContext).getRegistry().lookupFlowConstruct(construct.getName()));
  }

  @Test
  public void testInitialStateStopped() throws Exception {
    AbstractFlowConstruct flow = getStoppedFlowConstruct();
    assertFalse(flow.isStarted());
    assertFalse(flow.isStopped());

    flow.initialise();
    assertFalse(flow.isStarted());
    assertFalse(flow.isStopped());

    // This should not actually start the flow
    flow.start();
    assertFalse(flow.isStarted());
    assertTrue(flow.isStopped());

    // Finally the flow is actually started
    flow.start();
    assertTrue(flow.isStarted());
    assertFalse(flow.isStopped());

    // Try to start again
    try {
      flow.start();
      fail("Exception expected: Cannot start an already started flow");
    } catch (final IllegalStateException e) {
      // expected
    }
  }

}
