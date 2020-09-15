/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.log4j2;

import static java.lang.Runtime.getRuntime;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mule.test.allure.AllureConstants.Logging.LOGGING;
import static org.mule.test.allure.AllureConstants.Logging.LoggingStory.CONTEXT_FACTORY;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.apache.logging.log4j.core.util.Cancellable;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.module.launcher.api.log4j2.AsyncLoggerExceptionHandler;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SmallTest
@Feature(LOGGING)
@Story(CONTEXT_FACTORY)
public class MuleLog4jContextFactoryTestCase extends AbstractMuleTestCase {

  private static final String LOG_CONFIGURATION_FACTORY_PROPERTY = "log4j.configurationFactory";
  private static final String ASYNC_LOGGER_EXCEPTION_HANDLER_PROPERTY = "AsyncLoggerConfig.ExceptionHandler";

  private static final Integer SHUTDOWN_HOOKS_NUMBER = getRuntime().availableProcessors();
  private static final Integer LATCH_TIMEOUT_MILLIS = 500;

  private Map<String, String> originalSystemProperties;

  @Before
  public void before() {
    originalSystemProperties = new HashMap<>();
    originalSystemProperties.put(LOG_CONFIGURATION_FACTORY_PROPERTY, getProperty(LOG_CONFIGURATION_FACTORY_PROPERTY));
    originalSystemProperties.put(ASYNC_LOGGER_EXCEPTION_HANDLER_PROPERTY, getProperty(ASYNC_LOGGER_EXCEPTION_HANDLER_PROPERTY));
  }

  @After
  public void after() {
    for (Map.Entry<String, String> entry : originalSystemProperties.entrySet()) {
      if (entry.getValue() != null) {
        setProperty(entry.getKey(), entry.getValue());
      } else {
        System.clearProperty(entry.getKey());
      }
    }
  }

  @Test
  public void systemProperties() {
    MuleLog4jContextFactory factory = new MuleLog4jContextFactory(true);
    assertThat(XmlConfigurationFactory.class.getName(), equalTo(getProperty(LOG_CONFIGURATION_FACTORY_PROPERTY)));
    assertThat(AsyncLoggerExceptionHandler.class.getName(), equalTo(getProperty(ASYNC_LOGGER_EXCEPTION_HANDLER_PROPERTY)));
    factory.dispose();
  }

  @Test
  public void customExceptionHandler() {
    final String customHandler = "custom";
    setProperty(ASYNC_LOGGER_EXCEPTION_HANDLER_PROPERTY, customHandler);
    MuleLog4jContextFactory factory = new MuleLog4jContextFactory(true);
    assertThat(customHandler, equalTo(getProperty(ASYNC_LOGGER_EXCEPTION_HANDLER_PROPERTY)));
    factory.dispose();
  }

  @Test
  public void dispose() {
    ArtifactAwareContextSelector contextSelector = mock(ArtifactAwareContextSelector.class);
    MuleLog4jContextFactory factory = new MuleLog4jContextFactory(contextSelector);
    factory.dispose();
    verify(contextSelector).dispose();
  }

  @Test
  @Issue("MULE-18742")
  @Description("If any shutdown callback is cancelled while the log is disposing everything should work")
  public void cancelWhileDisposing() {
    ArtifactAwareContextSelector contextSelector = mock(ArtifactAwareContextSelector.class);
    MuleLog4jContextFactory factory = new MuleLog4jContextFactory(contextSelector);
    ShutdownCallbackRegistry shutdownCallbackRegistry = factory.getShutdownCallbackRegistry();

    Latch latch = new Latch();
    AtomicInteger executedHooks = new AtomicInteger();

    Cancellable hookToCancel = shutdownCallbackRegistry.addShutdownCallback(() -> {
      try {
        latch.await(LATCH_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
      }
      executedHooks.incrementAndGet();
    });
    shutdownCallbackRegistry.addShutdownCallback(() -> {
      hookToCancel.cancel();
      latch.release();
      executedHooks.incrementAndGet();
    });
    for (int i = 0; i < SHUTDOWN_HOOKS_NUMBER - 2; i++) {
      shutdownCallbackRegistry.addShutdownCallback(executedHooks::incrementAndGet);
    }

    assertThat(executedHooks.get(), is(0));
    factory.dispose();
    assertThat(executedHooks.get(), is(SHUTDOWN_HOOKS_NUMBER));
  }
}
