/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.modules.schedulers.cron;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.source.polling.PollingMessageSource;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;


public class StoppedCronSchedulerTestCase extends MuleArtifactFunctionalTestCase {

  private static List<String> foo = new ArrayList<>();

  @Override
  protected String getConfigFile() {
    return "cron-scheduler-stopped-config.xml";
  }

  @Test
  public void test() throws Exception {
    runSchedulersOnce();

    new PollingProber().check(new JUnitLambdaProbe(() -> {
      assertThat(foo.size(), greaterThanOrEqualTo(1));
      return true;
    }));
  }


  public static class FooComponent {

    public boolean process(String s) {
      synchronized (foo) {

        foo.add(s);

      }

      return false;
    }
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
}
