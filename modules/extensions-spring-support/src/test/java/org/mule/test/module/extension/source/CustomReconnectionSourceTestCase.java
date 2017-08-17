/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.heisenberg.extension.ReconnectableHeisenbergSource.failedReconnections;
import static org.mule.test.heisenberg.extension.ReconnectableHeisenbergSource.succesfulReconnections;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CustomReconnectionSourceTestCase extends AbstractExtensionFunctionalTestCase {

  public static final int TIMEOUT_MILLIS = 50000;
  public static final int POLL_DELAY_MILLIS = 100;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "custom-reconnection-source-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    reset();
  }

  @Override
  protected void doTearDown() throws Exception {
    super.doTearDown();
    reset();
  }

  @Test
  public void successfulCustomReconnection() throws Exception {
    startFlow("successfulCustomReconnection");
    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS, () -> succesfulReconnections > 1);
    assertThat(failedReconnections, is(0));
  }

  @Test
  public void failingCustomReconnection() throws Exception {
    startFlow("failingCustomReconnection");
    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS, () -> failedReconnections > 0);
    assertThat(succesfulReconnections, is(0));
  }

  private void startFlow(String flowName) throws Exception {
    ((Flow) getFlowConstruct(flowName)).start();
  }

  private void reset() {
    succesfulReconnections = 0;
    failedReconnections = 0;
  }
}
