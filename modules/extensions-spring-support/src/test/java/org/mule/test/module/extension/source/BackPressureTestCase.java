/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.extension.api.runtime.source.BackPressureAction.DROP;
import static org.mule.runtime.extension.api.runtime.source.BackPressureAction.FAIL;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.test.allure.AllureConstants.ExecutionEngineFeature.ExecutionEngineStory.BACKPRESSURE;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getConfigurationFromRegistry;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.extension.api.runtime.source.BackPressureContext;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SOURCES)
@Story(BACKPRESSURE)
public class BackPressureTestCase extends AbstractExtensionFunctionalTestCase {

  private static List<CoreEvent> EVENTS;

  public static class Collector implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      synchronized (EVENTS) {
        EVENTS.add(event);
        return event;
      }
    }
  }

  private HeisenbergExtension heisenberg;
  private List<BackPressureContext> backPressureContexts;
  private List<org.mule.sdk.api.runtime.source.BackPressureContext> sdkBackPressureContexts;

  @Override
  protected String getConfigFile() {
    return "source/heisenberg-backpressure-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    heisenberg = getConfigurationFromRegistry("heisenberg", testEvent(), muleContext);
    assertThat(heisenberg, is(notNullValue()));
    backPressureContexts = new LinkedList<>();
    sdkBackPressureContexts = new LinkedList<>();
    EVENTS = new LinkedList<>();
  }

  @Override
  protected void doTearDown() throws Exception {
    backPressureContexts = null;
    sdkBackPressureContexts = null;
    EVENTS = null;
  }

  @Test
  public void backPressureWithFailStrategy() throws Exception {
    startFlow("defaultToFail");
    check(15000, 100, () -> {
      sdkBackPressureContexts.addAll(heisenberg.getSdkBackPressureContexts());
      return !sdkBackPressureContexts.isEmpty();
    });

    org.mule.sdk.api.runtime.source.BackPressureContext sample = sdkBackPressureContexts.get(0);
    assertThat(sample.getAction(), is(org.mule.sdk.api.runtime.source.BackPressureAction.FAIL));
    assertThat(sample.getEvent().getMessage().getPayload().getValue().toString(), containsString("If found by DEA contact"));
    assertThat(sample.getSourceCallbackContext(), is(notNullValue()));
  }

  @Test
  public void backPressureWithDropStrategy() throws Exception {
    startFlow("configuredToDrop");
    check(15000, 100, () -> {
      sdkBackPressureContexts.addAll(heisenberg.getSdkBackPressureContexts());
      return !sdkBackPressureContexts.isEmpty();
    });

    org.mule.sdk.api.runtime.source.BackPressureContext sample = sdkBackPressureContexts.get(0);
    assertThat(sample.getAction(), is(org.mule.sdk.api.runtime.source.BackPressureAction.DROP));
    assertThat(sample.getEvent().getMessage().getPayload().getValue().toString(), containsString("If found by DEA contact"));
    assertThat(sample.getSourceCallbackContext(), is(notNullValue()));
  }

  @Test
  public void defaultToWait() throws Exception {
    startFlow("defaultCase");
    check(15000, 100, () -> EVENTS.size() >= 3);

    assertThat(backPressureContexts, hasSize(0));
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) registry.lookupByName(flowName).get()).start();
  }

}
