/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import static org.mule.runtime.api.util.MuleSystemProperties.GRACEFUL_SHUTDOWN_DEFAULT_TIMEOUT;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.GracefulShutdownStory.GRACEFUL_SHUTDOWN_STORY;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import static org.junit.Assert.assertThrows;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(GRACEFUL_SHUTDOWN_STORY)
public class MuleConfigurationConfiguratorTestCase extends AbstractMuleTestCase {

  @Rule
  public SystemProperty gracefulShutdownDefaultTimeout = new SystemProperty(GRACEFUL_SHUTDOWN_DEFAULT_TIMEOUT, null);

  private MuleConfigurationConfigurator muleConfigurationConfigurator;

  @Before
  public void setUp() {
    muleConfigurationConfigurator = new MuleConfigurationConfigurator();
    final var muleContext = mock(MuleContext.class);
    when(muleContext.getConfiguration()).thenReturn(new DefaultMuleConfiguration());
    muleConfigurationConfigurator.setMuleContext(muleContext);
  }

  @Test
  public void shutdownTimeoutConfigured() throws Exception {
    muleConfigurationConfigurator.setShutdownTimeout(100L);

    final var configuration = muleConfigurationConfigurator.getObject();
    assertThat(configuration.getShutdownTimeout(), is(100L));
  }

  @Test
  public void shutdownTimeoutDefaultByEnvironment() throws Exception {
    setProperty(GRACEFUL_SHUTDOWN_DEFAULT_TIMEOUT, "200");

    try {
      final var configuration = muleConfigurationConfigurator.getObject();
      assertThat(configuration.getShutdownTimeout(), is(200L));
    } finally {
      clearProperty(GRACEFUL_SHUTDOWN_DEFAULT_TIMEOUT);
    }
  }

  @Test
  public void shutdownTimeoutDefaultByEnvironmentInvalid() throws Exception {
    setProperty(GRACEFUL_SHUTDOWN_DEFAULT_TIMEOUT, "aaa");

    try {
      assertThrows(NumberFormatException.class, () -> muleConfigurationConfigurator.getObject());
    } finally {
      clearProperty(GRACEFUL_SHUTDOWN_DEFAULT_TIMEOUT);
    }
  }

  @Test
  public void shutdownTimeoutDefault() throws Exception {
    final var configuration = muleConfigurationConfigurator.getObject();
    assertThat(configuration.getShutdownTimeout(), is(5000L));
  }
}
