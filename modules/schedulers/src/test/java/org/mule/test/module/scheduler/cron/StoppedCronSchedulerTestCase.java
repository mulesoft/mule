/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.scheduler.cron;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.source.SchedulerMessageSource;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Test;


public class StoppedCronSchedulerTestCase extends MuleArtifactFunctionalTestCase {

  private static List<String> foo = new ArrayList<>();

  @Override
  protected String getConfigFile() {
    return "cron-scheduler-stopped-config.xml";
  }

  @Test
  public void test() throws Exception {
    runSchedulersOnce(() -> {
      new PollingProber(RECEIVE_TIMEOUT, 200).check(new JUnitLambdaProbe(() -> {
        assertThat(foo.size(), greaterThanOrEqualTo(1));
        return true;
      }));
      return null;
    });
  }


  public static class FooComponent {

    public boolean process(String s) {
      synchronized (foo) {
        foo.add(s);
      }
      return false;
    }
  }

  private void runSchedulersOnce(Supplier<Void> assertionSupplier) throws Exception {
    Flow flow = (Flow) (muleContext.getRegistry().lookupFlowConstruct("pollfoo"));
    flow.start();
    try {
      MessageSource flowSource = flow.getMessageSource();
      if (flowSource instanceof SchedulerMessageSource) {
        ((SchedulerMessageSource) flowSource).trigger();
      }
      assertionSupplier.get();
    } finally {
      flow.stop();
    }
  }
}
