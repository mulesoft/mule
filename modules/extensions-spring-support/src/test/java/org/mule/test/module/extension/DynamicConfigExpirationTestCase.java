/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import javax.inject.Inject;
import javax.inject.Named;
import org.junit.Test;

public class DynamicConfigExpirationTestCase extends AbstractExtensionFunctionalTestCase {

  @Inject
  @Named("sourceWithDynamicConfig")
  public Flow sourceWithDynamicConfig;

  @Override
  protected String getConfigFile() {
    return "dynamic-config-expiration.xml";
  }

  @Test
  public void expireDynamicConfig() throws Exception {
    HeisenbergExtension config = invokeDynamicConfig("dynamic", "heisenberg", "Walt");

    //force cache cleanUp
    Thread.sleep(6000);
    HeisenbergExtension anotherConfig = invokeDynamicConfig("dynamic", "heisenberg", "Walt");

    assertExpired(config, 5000, 1000);

    assertInitialised(config);
  }

  @Test
  public void expireDynamicConfigWithCustomExpiration() throws Exception {
    HeisenbergExtension config =
        invokeDynamicConfig("dynamicWithCustomExpiration", "heisenbergWithCustomExpiration", "Walter Jr.");

    //force cache cleanUp
    Thread.sleep(6000);
    HeisenbergExtension anotherConfig =
        invokeDynamicConfig("dynamicWithCustomExpiration", "heisenbergWithCustomExpiration", "Walter Jr.");

    assertExpired(config, 5000, 1000);
    assertInitialised(config);
  }

  @Test
  public void doNotExpireDynamicConfigWithCustomExpirationUsedBySource() throws Exception {
    HeisenbergExtension config =
        invokeDynamicConfig("dynamicWithCustomExpirationForSource", "heisenbergWithCustomExpirationForSource", "Walter Blanco");

    try {
      assertExpired(config, 10000, 1000);
      throw new IllegalStateException("Config should not have been expired");
    } catch (AssertionError e) {
      //all good
    }

    assertInitialised(config);

    sourceWithDynamicConfig.stop();

    assertExpired(config, 6000, 100);

  }

  @Test
  public void expirationWorksAfterRestartingSource() throws Exception {
    doNotExpireDynamicConfigWithCustomExpirationUsedBySource();
    sourceWithDynamicConfig.start();
    doNotExpireDynamicConfigWithCustomExpirationUsedBySource();
  }

  private void assertInitialised(HeisenbergExtension config) {
    assertThat(config.getInitialise(), is(1));
    assertThat(config.getStart(), is(1));
  }

  private void assertExpired(HeisenbergExtension config, long timeoutMilis, long pollDelayMillis) {
    PollingProber prober = new PollingProber(timeoutMilis, pollDelayMillis);
    prober.check(new JUnitLambdaProbe(() -> {
      assertThat(config.getStop(), is(1));
      assertThat(config.getDispose(), is(1));
      return true;
    }, "config was not stopped or disposed"));
  }

  private HeisenbergExtension invokeDynamicConfig(String flowName, String configName, String payload) throws Exception {
    FlowRunner runner = flowRunner(flowName).withPayload(payload);

    final CoreEvent event = runner.buildEvent();
    String returnedName = getPayloadAsString(runner.run().getMessage());

    HeisenbergExtension config =
        (HeisenbergExtension) muleContext.getExtensionManager().getConfiguration(configName, event).getValue();

    // validate we actually hit the correct dynamic config
    assertThat(returnedName, is(payload));
    assertThat(config.getPersonalInfo().getName(), is(payload));

    return config;
  }
}
