/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.schedule;


import static org.junit.Assert.assertEquals;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.source.polling.PollingMessageSource;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This is a test for poll with schedulers. It validates that the polls can be executed, stopped, run.
 */
public class PollScheduleTestCase extends AbstractIntegrationTestCase {

  private static List<String> foo = new ArrayList<>();
  private static List<String> bar = new ArrayList<>();

  Prober workingPollProber = new PollingProber(10000, 100l);

  @BeforeClass
  public static void setProperties() {
    System.setProperty("frequency.days", "4");
    System.setProperty("frequency.millis", "2000");
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/schedule/polling-schedule-config.xml";
  }


  /**
   * This test validate that the polls can be stopped and run on demand.
   *
   * It checks correct functionality of polls. Stop the schedulers Waits for the polls to be executed (they shouldn't, as they are
   * stopped) Checks that the polls where not executed. Runs the polls on demand Checks that the polls where executed only once.
   */
  @Test
  public void test() throws Exception {
    workingPollProber.check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return (foo.size() > 2 && checkCollectionValues(foo, "foo")) && (bar.size() > 2 && checkCollectionValues(bar, "bar"));
      }

      @Override
      public String describeFailure() {
        return "The collections foo and bar are not correctly filled";
      }
    });


    stopSchedulers();

    waitForPollElements();

    int fooElementsAfterStopping = foo.size();

    waitForPollElements();

    assertEquals(fooElementsAfterStopping, foo.size());

    runSchedulersOnce();

    Thread.sleep(200);

    assertEquals(fooElementsAfterStopping + 1, foo.size());
  }

  private void waitForPollElements() throws InterruptedException {
    Thread.sleep(2000);
  }



  private boolean checkCollectionValues(List<String> coll, String value) {
    for (String s : coll) {
      if (!s.equals(value)) {
        return false;
      }
    }

    return true;
  }


  private void runSchedulersOnce() throws Exception {
    Flow flow = (Flow) (muleContext.getRegistry().lookupFlowConstruct("pollfoo"));
    flow.start();
    try {
      MessageSource flowSource = flow.getMessageSource();
      if (flowSource instanceof PollingMessageSource) {
        ((PollingMessageSource) flowSource).performPoll();
      }
    } finally {
      flow.stop();
    }
  }

  private void stopSchedulers() throws MuleException {
    Flow flow = (Flow) (muleContext.getRegistry().lookupFlowConstruct("pollfoo"));
    flow.stop();
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
}
