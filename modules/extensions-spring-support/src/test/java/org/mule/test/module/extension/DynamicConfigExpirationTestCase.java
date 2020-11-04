/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.extension.api.error.MuleErrors.CONNECTIVITY;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.tck.probe.PollingProber.checkNot;
import static org.mule.test.heisenberg.extension.MoneyLaunderingOperation.closePagingProviderCalls;
import static org.mule.test.heisenberg.extension.MoneyLaunderingOperation.getPageCalls;

import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import java.util.LinkedList;
import java.util.List;

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

  private static List<Integer> capturedStats;
  private static List<Integer> capturedConfigStates;
  private static HeisenbergExtension streamingConfig;

  public static class CaptureStatisticsProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      synchronized (capturedStats) {
        TypedValue configVariable = event.getVariables().get("heisenbergConfig");
        if (configVariable != null) {
          HeisenbergExtension config = (HeisenbergExtension) configVariable.getValue();
          capturedStats.add(muleContext.getExtensionManager().getConfiguration(config.getConfigName(), event).getStatistics()
              .getActiveComponents());
        } else {
          capturedStats
              .add(muleContext.getExtensionManager().getConfiguration("heisenbergWithShortExpiration", event).getStatistics()
                  .getActiveComponents());
        }
      }
      return event;
    }
  }

  public static class CaptureConfigStateProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      synchronized (capturedConfigStates) {
        TypedValue configVariable = event.getVariables().get("heisenbergConfig");
        if (configVariable != null) {
          HeisenbergExtension config = (HeisenbergExtension) configVariable.getValue();
          capturedConfigStates.add(config.getDispose());
        } else {
          HeisenbergExtension config =
              (HeisenbergExtension) muleContext.getExtensionManager().getConfiguration("heisenbergWithShortExpiration", event)
                  .getValue();
          capturedConfigStates.add(config.getDispose());
        }
      }
      return event;
    }
  }

  public static class CaptureStreamingConfigStateProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      synchronized (capturedConfigStates) {
        if (streamingConfig == null) {
          streamingConfig = (HeisenbergExtension) muleContext.getExtensionManager()
              .getConfiguration("heisenbergWithShortExpiration", event).getValue();
        }
        capturedConfigStates.add(streamingConfig.getDispose());
      }
      return event;
    }
  }

  @Override
  protected void doSetUp() throws Exception {
    resetCounters();
    capturedStats = new LinkedList<>();
    capturedConfigStates = new LinkedList<>();
  }

  @Override
  protected void doTearDown() throws Exception {
    capturedStats = null;
    capturedConfigStates = null;
  }

  @Test
  public void expireDynamicConfig() throws Exception {
    HeisenbergExtension config = invokeDynamicConfig("dynamic", "heisenberg", "Walt");

    assertExpired(config, 5000, 1000);

    assertInitialised(config);
  }

  @Test
  public void expireDynamicConfigWithCustomExpiration() throws Exception {
    HeisenbergExtension config =
        invokeDynamicConfig("dynamicWithCustomExpiration", "heisenbergWithCustomExpiration", "Walter Jr.");

    try {
      assertExpired(config, 1500, 100);
      throw new IllegalStateException("Config should not have been expired");
    } catch (AssertionError e) {
      //all good
    }

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

  @Test
  public void doNotExpireConfigUsedByPagedOperation() throws Exception {
    flowRunner("dynamicWithShortExpirationForPagedOperation").withVariable("heisenbergName", "Waltercito White")
        .withVariable("failOn", -1).run();
    checkNot(30000, 3000, () -> capturedConfigStates.size() > 4);
    assertThat(capturedConfigStates, contains(0, 0, 0, 1));
  }

  @Test
  public void doNotExpireConfigUsedByPagedOperationWithReconnectionOnFirstPage() throws Exception {
    flowRunner("dynamicWithShortExpirationForPagedOperation").withVariable("heisenbergName", "Waltercito White")
        .withVariable("failOn", 1).run();
    checkNot(30000, 3000, () -> capturedConfigStates.size() > 3);
    assertThat(capturedConfigStates, contains(0, 0, 1));
  }

  @Test
  public void doNotExpireConfigUsedByPagedOperationWithReconnectionOnSecondPage() throws Exception {
    flowRunner("dynamicWithShortExpirationForPagedOperation").withVariable("heisenbergName", "Waltercito White")
        .withVariable("failOn", 2).run();
    checkNot(30000, 3000, () -> capturedConfigStates.size() > 3);
    assertThat(capturedConfigStates, contains(0, 0, 1));
  }

  @Test
  public void doNotExpireConfigUsedByStreamingOperation() throws Exception {
    flowRunner("dynamicWithShortExpirationForStreamingOperation").withVariable("heisenbergName", "Waltercito White")
        .run();
    checkNot(30000, 3000, () -> capturedConfigStates.size() > 3);
    assertThat(capturedConfigStates, contains(0, 0, 1));
  }

  // Remove this test once MULE-18774 is fixed
  @Test
  public void doNotExpireConfigUsedByPagedOperationCheckingCounter() throws Exception {
    flowRunner("dynamicWithShortExpirationForPagedOperationCapturingEvents").withVariable("heisenbergName", "Waltercito White")
        .withVariable("failOn", -1).run();
    checkNot(10000, 3000, () -> capturedStats.size() > 4);
    //assertThat(capturedStats, contains(2, 2, 2, 1));
  }

  // Remove this test once MULE-18774 is fixed
  @Test
  public void doNotExpireConfigUsedByPagedOperationWithReconnectionOnFirstPageCheckingCounter() throws Exception {
    flowRunner("dynamicWithShortExpirationForPagedOperationCapturingEvents").withVariable("heisenbergName", "Waltercito White")
        .withVariable("failOn", 1).run();
    checkNot(10000, 3000, () -> capturedStats.size() > 3);
    //assertThat(capturedStats, contains(2, 2, 1));
  }

  // Remove this test once MULE-18774 is fixed
  @Test
  public void doNotExpireConfigUsedByPagedOperationWithReconnectionOnSecondPageCheckingCounter() throws Exception {
    flowRunner("dynamicWithShortExpirationForPagedOperationCapturingEvents").withVariable("heisenbergName", "Waltercito White")
        .withVariable("failOn", 2).run();
    checkNot(10000, 3000, () -> capturedStats.size() > 3);
    //assertThat(capturedStats, contains(2, 2, 1));
  }

  // Remove this test once MULE-18774 is fixed
  @Test
  public void doNotExpireConfigUsedByStreamingOperationCheckingCounter() throws Exception {
    flowRunner("dynamicWithShortExpirationForStreamingOperationCapturingEvents")
        .withVariable("heisenbergName", "Waltercito White")
        .run();
    checkNot(10000, 3000, () -> capturedStats.size() > 2);
    //assertThat(capturedStats, contains(2, 1));
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

  public static void resetCounters() {
    closePagingProviderCalls = 0;
    getPageCalls = 0;
  }
}
