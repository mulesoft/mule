/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.schedule;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.source.SchedulerMessageSource;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.source.scheduler.DefaultSchedulerMessageSource;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Test;

/**
 * This is a test for poll with schedulers. It validates that the polls can be executed, stopped, run.
 */
public class PollScheduleTestCase extends AbstractIntegrationTestCase {

  private static List<String> foo = new ArrayList<>();
  private static List<String> bar = new ArrayList<>();

  @ClassRule
  public static SystemProperty days = new SystemProperty("frequency.days", "4");

  @ClassRule
  public static SystemProperty millis = new SystemProperty("frequency.millis", "2000");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/schedule/polling-schedule-config.xml";
  }

  /**
   * This test validates that the polls can be stopped and run on demand.
   *
   * It checks correct functionality of polls. Stop the schedulers Waits for the polls to be executed (they shouldn't, as they are
   * stopped) Checks that the polls where not executed. Runs the polls on demand Checks that the polls where executed only once.
   */
  @Test
  public void test() throws Exception {
    new PollingProber(10000, 100l).check(new Probe() {

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

    int fooElementsAfterStopping = foo.size();

    waitForPollElements();

    assertThat(foo.size(), is(fooElementsAfterStopping));

    startSchedulers();
    runSchedulersOnce();

    new PollingProber(200, 10).check(new JUnitLambdaProbe(() -> {
      // One for the scheduler run and another for the on-demand one
      assertThat(foo.size(), is(fooElementsAfterStopping + 2));
      return true;
    }));
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
    MessageSource flowSource = flow.getMessageSource();
    if (flowSource instanceof DefaultSchedulerMessageSource) {
      ((SchedulerMessageSource) flowSource).trigger();
    }
  }

  private void stopSchedulers() throws MuleException {
    Flow flow = (Flow) (muleContext.getRegistry().lookupFlowConstruct("pollfoo"));
    flow.stop();
  }

  private void startSchedulers() throws MuleException {
    Flow flow = (Flow) (muleContext.getRegistry().lookupFlowConstruct("pollfoo"));
    flow.start();
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
