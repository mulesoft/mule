/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;

import org.junit.Rule;
import org.junit.Test;

public class HttpDispatcherLifecycleTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort port = new DynamicPort("httpPort");

  private Prober prober = new PollingProber(3000, 500);

  @Override
  protected String getConfigFile() {
    return "http-dispatcher-lifecycle-config.xml";
  }

  @Test
  public void dispatcherThreadFinishesAfterDispose() throws Exception {
    MuleClient client = muleContext.getClient();

    MuleMessage response = client.send("http://localhost:" + port.getValue(), TEST_MESSAGE, null).getRight();
    assertThat(getPayloadAsString(response), equalTo(TEST_MESSAGE));

    muleContext.dispose();

    prober.check(new Probe() {

      @Override
      public boolean isSatisfied() {
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
          if (thread.getName().startsWith("http.request.dispatch")) {
            return false;
          }
        }
        return true;
      }

      @Override
      public String describeFailure() {
        return "Dispatcher thread was not stopped";
      }
    });

  }

}
