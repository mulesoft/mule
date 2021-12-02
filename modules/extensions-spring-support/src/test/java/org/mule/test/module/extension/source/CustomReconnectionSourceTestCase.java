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
import static org.mule.test.allure.AllureConstants.ReconnectionPolicyFeature.RECONNECTION_POLICIES;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;
import static org.mule.test.heisenberg.extension.ReconnectableHeisenbergSource.failedReconnections;
import static org.mule.test.heisenberg.extension.ReconnectableHeisenbergSource.succesfulReconnections;
import static org.mule.test.heisenberg.extension.ReconnectableHeisenbergSdkSource.failedReconnectionsSdk;
import static org.mule.test.heisenberg.extension.ReconnectableHeisenbergSdkSource.succesfulReconnectionsSdk;

import org.mule.runtime.core.api.construct.Flow;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;

@Features({@Feature(SOURCES), @Feature(RECONNECTION_POLICIES)})
public class CustomReconnectionSourceTestCase extends AbstractExtensionFunctionalTestCase {

  public static final int TIMEOUT_MILLIS = 50000;
  public static final int POLL_DELAY_MILLIS = 100;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "source/custom-reconnection-source-config.xml";
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

  @Test
  public void successfulCustomReconnectionSdkApi() throws Exception {
    startFlow("successfulCustomReconnectionSdkApi");
    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS, () -> succesfulReconnectionsSdk > 1);
    assertThat(failedReconnectionsSdk, is(0));
  }

  @Test
  public void failingCustomReconnectionSdkApi() throws Exception {
    startFlow("failingCustomReconnectionSdkApi");
    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS, () -> failedReconnectionsSdk > 0);
    assertThat(succesfulReconnectionsSdk, is(0));
  }

  private void startFlow(String flowName) throws Exception {
    ((Flow) getFlowConstruct(flowName)).start();
  }

  private void reset() {
    succesfulReconnections = 0;
    failedReconnections = 0;
    failedReconnectionsSdk = 0;
    succesfulReconnectionsSdk = 0;
  }
}
