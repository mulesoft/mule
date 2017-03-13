/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.modules.schedulers.cron;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.source.scheduler.SchedulerMessageSource;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.util.ArrayList;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Test;


/**
 * <p>
 * Uses the API to stop and start cron schedulers.
 * </p>
 */
public class CronsSchedulerTestCase extends MuleArtifactFunctionalTestCase {

  private static List<String> foo = new ArrayList<>();
  private static List<String> bar = new ArrayList<>();

  @ClassRule
  public static SystemProperty days = new SystemProperty("expression.property", "0/1 * * * * ?");

  @Override
  protected String getConfigFile() {
    return "cron-scheduler-config.xml";
  }

  @Test
  public void test() throws Exception {
    waitForPollElements();

    checkForFooCollectionToBeFilled();
    checkForBarCollectionToBeFilled();

    stopSchedulers();

    int fooElementsAfterStopping = foo.size();

    waitForPollElements();

    assertThat(foo.size(), is(fooElementsAfterStopping));

    startSchedulers();
    runSchedulersOnce();

    new PollingProber(2000, 100).check(new JUnitLambdaProbe(() -> {
      // One for the scheduler run and another for the on-demand one
      assertThat(foo.size(), is(fooElementsAfterStopping + 2));
      return true;
    }));
  }

  private void waitForPollElements() throws InterruptedException {
    Thread.sleep(2000);
  }

  private void checkForFooCollectionToBeFilled() {
    synchronized (foo) {
      foo.size();
      assertTrue(foo.size() > 0);
      for (String s : foo) {
        assertEquals("foo", s);
      }
    }
  }

  private void checkForBarCollectionToBeFilled() {
    synchronized (bar) {
      bar.size();
      assertTrue(bar.size() > 0);
      for (String s : bar) {
        assertEquals("bar", s);
      }
    }
  }

  private void runSchedulersOnce() throws Exception {
    Flow flow = (Flow) (muleContext.getRegistry().lookupFlowConstruct("pollfoo"));
    MessageSource flowSource = flow.getMessageSource();
    if (flowSource instanceof SchedulerMessageSource) {
      ((SchedulerMessageSource) flowSource).performPoll();
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
