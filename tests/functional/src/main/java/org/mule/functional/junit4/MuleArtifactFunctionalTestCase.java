/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.core.api.event.EventContextFactory.create;

import static org.mockito.junit.MockitoJUnit.rule;

import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.event.EventContextService;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;
import org.mule.test.runner.RunnerConfigSystemProperty;

import jakarta.inject.Inject;

import org.junit.Rule;

import org.mockito.junit.MockitoRule;

/**
 * Base class for mule functional test cases that run tests using class loading isolation. This class will set the default values
 * for testing mule components.
 *
 * @since 4.0
 */
@ArtifactClassLoaderRunnerConfig(
    providedExclusions = {
        "org.mule.tests:*:*:*:*"
    },
    testExclusions = {
        "org.mule.runtime:*:*:*:*",
        "org.mule.runtime.boot:*:*:*:*",
        "org.mule.modules*:*:*:*:*",
        "org.mule.transports:*:*:*:*",
        "org.mule.extensions:*:*:*:*",
        "org.mule.connectors:*:*:*:*",
        "org.mule.tests.plugin:*:*:*:*",
        "com.mulesoft.mule.runtime*:*:*:*:*",
        "com.mulesoft.licm:*:*:*:*",
        // Force logging libs to be used from the container
        "org.slf4j:*:*:*:*",
        "org.apache.logging.log4j:*:*:*:*",
        "com.lmax.disruptor:*:*:*:*"
    },
    testInclusions = {
        "*:*:jar:tests:*",
        "*:*:test-jar:*:*"
    },
    testRunnerExportedRuntimeLibs = {"org.mule.tests:mule-tests-functional"},
    systemProperties = {
        @RunnerConfigSystemProperty(
            key = MuleArtifactFunctionalTestCase.EXTENSION_JVM_ENFORCEMENT_PROPERTY,
            value = MuleArtifactFunctionalTestCase.JVM_ENFORCEMENT_LOOSE)
    })
public abstract class MuleArtifactFunctionalTestCase extends ArtifactFunctionalTestCase {

  /**
   * System property to set the enforcement policy. Defined here as a decision was made not to expose it as an API yet. For now,
   * it will be for internal use only.
   *
   * @since 4.5.0
   */
  static final String EXTENSION_JVM_ENFORCEMENT_PROPERTY = SYSTEM_PROPERTY_PREFIX + "jvm.version.extension.enforcement";
  static final String JVM_ENFORCEMENT_LOOSE = "LOOSE";

  // This is needed apart from the setting in {@code @ArtifactClassLoaderRunnerConfig} because tha validation also takes place
  // during extension registering, not only during its discovery.
  @Rule
  public SystemProperty jvmVersionExtensionEnforcementLoose =
      new SystemProperty(EXTENSION_JVM_ENFORCEMENT_PROPERTY, JVM_ENFORCEMENT_LOOSE);

  @Rule
  public MockitoRule rule = rule();

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
  }
}
