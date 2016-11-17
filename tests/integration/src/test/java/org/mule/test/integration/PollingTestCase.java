/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.core.api.Event.getCurrentEvent;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.source.polling.PollingMessageSource;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class PollingTestCase extends AbstractIntegrationTestCase {

  private static List<String> foo;
  private static List<String> bar;
  private static List<Event> events;
  private static List<String> eventIds;

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    super.doSetUpBeforeMuleContextCreation();
    foo = new ArrayList<>();
    bar = new ArrayList<>();
    events = new ArrayList<>();
    eventIds = new ArrayList<>();
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/polling-config.xml";
  }

  @Test
  public void testPolling() throws Exception {
    int schedulers = 0;
    for (FlowConstruct flowConstruct : muleContext.getRegistry().lookupFlowConstructs()) {
      Flow flow = (Flow) flowConstruct;
      MessageSource flowSource = flow.getMessageSource();
      if (flowSource instanceof PollingMessageSource) {
        schedulers++;
      }
    }
    assertEquals(4, schedulers);

    Thread.sleep(5000);
    synchronized (foo) {
      assertTrue(foo.size() > 0);
      for (String s : foo) {
        assertEquals("foo", s);
      }
    }
    synchronized (bar) {
      assertTrue(bar.size() > 0);
      for (String s : bar) {
        assertEquals("bar", s);
      }
    }

    synchronized (events) {
      assertTrue(events.size() > 0);
      assertEquals(events.size(), eventIds.size());

      for (int i = 0; i < events.size(); i++) {
        assertNotNull(events.get(i));
        assertThat(eventIds.get(i), equalTo(events.get(i).getContext().getId()));
      }
    }
  }

  public static class FooComponent {

    public boolean process(String s) {
      synchronized (foo) {

        if (foo.size() < 10) {
          foo.add(s);
          return true;
        }
      }
      return false;
    }
  }

  public static class BarComponent {

    public boolean process(String s) {
      synchronized (bar) {
        if (bar.size() < 10) {
          bar.add(s);
          return true;
        }
      }
      return false;
    }
  }

  public static class EventWireTrap implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      synchronized (events) {
        events.add(getCurrentEvent());
        eventIds.add(event.getContext().getId());
      }
      return event;
    }
  }

}
