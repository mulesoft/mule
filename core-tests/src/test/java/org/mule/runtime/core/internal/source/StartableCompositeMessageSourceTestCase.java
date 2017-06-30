/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.util.ObjectUtils;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class StartableCompositeMessageSourceTestCase extends AbstractMuleContextTestCase {

  protected SensingNullMessageProcessor listener;
  protected SensingNullMessageProcessor listener2;
  protected StartableCompositeMessageSource compositeSource;
  protected NullMessageSource source;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    listener = getSensingNullMessageProcessor();
    listener2 = getSensingNullMessageProcessor();
    compositeSource = getCompositeSource();
    source = new NullMessageSource(testEvent());
  }

  protected StartableCompositeMessageSource getCompositeSource() {
    return new StartableCompositeMessageSource();
  }

  @Test
  public void testAddSourceStopped() throws MuleException {
    compositeSource.setListener(listener);
    compositeSource.addSource(source);

    source.triggerSource();
    assertNull(listener.event);

    source.start();
    try {
      source.triggerSource();
      fail("Exception expected");
    } catch (Exception e) {
    }
    assertNull(listener.event);

    compositeSource.start();
    source.triggerSource();
    assertEquals(testEvent(), listener.event);
  }

  @Test
  public void testAddSourceStarted() throws MuleException {
    compositeSource.setListener(listener);
    compositeSource.start();

    compositeSource.addSource(source);

    source.triggerSource();
    assertEquals(testEvent(), listener.event);
  }

  @Test
  public void testRemoveSource() throws MuleException {
    compositeSource.setListener(listener);
    compositeSource.addSource(source);
    compositeSource.start();

    source.triggerSource();
    assertEquals(testEvent(), listener.event);
    listener.clear();

    compositeSource.removeSource(source);
    source.triggerSource();
    assertNull(listener.event);
  }

  @Test
  public void testSetListenerStarted() throws MuleException {
    compositeSource.addSource(source);
    compositeSource.setListener(listener);
    compositeSource.start();

    source.triggerSource();
    assertEquals(testEvent(), listener.event);

    listener.clear();
    compositeSource.setListener(listener2);

    source.triggerSource();
    assertNull(listener.event);
    assertEquals(testEvent(), listener2.event);
  }

  @Test
  public void testStart() throws MuleException {
    compositeSource.setListener(listener);
    compositeSource.addSource(source);

    source.triggerSource();
    assertNull(listener.event);

    compositeSource.start();
    source.triggerSource();
    assertEquals(testEvent(), listener.event);
  }

  @Test
  public void testStartNoListener() throws MuleException {
    compositeSource.addSource(source);
    try {
      compositeSource.start();
      fail("Exception excepted");
    } catch (Exception e) {
    }

  }

  @Test
  public void testStop() throws MuleException {
    compositeSource.setListener(listener);
    compositeSource.addSource(source);
    compositeSource.start();

    compositeSource.stop();
    source.triggerSource();
    assertNull(listener.event);
  }

  protected class NullMessageSource extends AbstractAnnotatedObject implements MessageSource, Startable, Stoppable {

    Event event;
    Processor listener;
    boolean started = false;

    public NullMessageSource(Event event) {
      this.event = event;
    }

    @Override
    public void setListener(Processor listener) {
      this.listener = listener;
    }

    public void triggerSource() throws MuleException {
      if (started && listener != null) {
        listener.process(event);
      }
    }

    @Override
    public void start() throws MuleException {
      started = true;
    }

    @Override
    public void stop() throws MuleException {
      started = false;
    }

    @Override
    public String toString() {
      return ObjectUtils.toString(this);
    }
  }
}
