/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.tck.MuleTestUtils.getTestFlow;

import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.event.EventContextService;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import javax.inject.Inject;

import org.junit.Rule;

/**
 * Base class for mule functional test cases that run tests using class loading isolation. This class will set the default values
 * for testing mule components.
 *
 * @since 4.0
 */
@ArtifactClassLoaderRunnerConfig(
    providedExclusions = {
        "org.mule.tests:*:*:*:*",
        "com.mulesoft.compatibility.tests:*:*:*:*"
    },
    testExclusions = {
        "org.mule.runtime:*:*:*:*",
        "org.mule.modules*:*:*:*:*",
        "org.mule.transports:*:*:*:*",
        "org.mule.mvel:*:*:*:*",
        "org.mule.extensions:*:*:*:*",
        "org.mule.connectors:*:*:*:*",
        "org.mule.tests.plugin:*:*:*:*",
        "com.mulesoft.mule.runtime*:*:*:*:*",
        "com.mulesoft.licm:*:*:*:*"
    },
    testInclusions = {
        "*:*:jar:tests:*",
        "*:*:test-jar:*:*"
    },
    testRunnerExportedRuntimeLibs = {"org.mule.tests:mule-tests-functional"})
public abstract class MuleArtifactFunctionalTestCase extends ArtifactFunctionalTestCase {

  @Inject
  protected ConfigurationComponentLocator locator;

  @Inject
  protected NotificationListenerRegistry notificationListenerRegistry;

  @Inject
  private EventContextService eventContextService;

  /**
   * Sets to disable initial state manager from MUnit as the plugin will be discovered and register to the ExtensionManager if
   * declared in the pom.xml.
   */
  @Rule
  public SystemProperty mUnitDisableInitialStateManagerProperty =
      new SystemProperty("munit.disable.initial.state.manager", "true");

  private CoreEvent _testEvent;

  /**
   * Creates and caches a test {@link CoreEvent} instance for the scope of the current test method.
   *
   * @return test event.
   * @throws MuleException
   */
  @Override
  protected CoreEvent testEvent() throws MuleException {
    if (_testEvent == null) {
      _testEvent = baseEvent();
    }
    return _testEvent;
  }

  private CoreEvent baseEvent() throws MuleException {
    FlowConstruct flowConstruct = getTestFlow(muleContext);
    return CoreEvent.builder(create(flowConstruct, TEST_CONNECTOR_LOCATION)).message(Message.of(TEST_PAYLOAD)).build();
  }

  @Override
  protected boolean doTestClassInjection() {
    return true;
  }

  @Override
  protected void doTearDown() throws Exception {
    if (_testEvent != null) {
      ((BaseEventContext) _testEvent.getContext()).success();
    }
    super.doTearDown();

    if (eventContextService != null) {
      new PollingProber(1000, 10).check(new JUnitLambdaProbe(() -> {
        assertThat(eventContextService.getCurrentlyActiveFlowStacks().toString(),
                   eventContextService.getCurrentlyActiveFlowStacks(), is(empty()));
        return true;
      }));
    }
  }
}
