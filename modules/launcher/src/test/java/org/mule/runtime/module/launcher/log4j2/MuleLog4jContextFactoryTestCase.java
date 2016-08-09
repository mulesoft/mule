/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.log4j2;

import static java.lang.System.getProperty;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SmallTest
public class MuleLog4jContextFactoryTestCase extends AbstractMuleTestCase {

  private static final String LOG_CONFIGURATION_FACTORY_PROPERTY = "log4j.configurationFactory";
  private static final String ASYNC_LOGGER_EXCEPTION_HANDLER_PROPERTY = "AsyncLoggerConfig.ExceptionHandler";

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
        System.setProperty(entry.getKey(), entry.getValue());
      } else {
        System.clearProperty(entry.getKey());
      }
    }
  }

  @Test
  public void systemProperties() {
    new MuleLog4jContextFactory();
    assertThat(XmlConfigurationFactory.class.getName(), equalTo(getProperty(LOG_CONFIGURATION_FACTORY_PROPERTY)));
    assertThat(AsyncLoggerExceptionHandler.class.getName(), equalTo(getProperty(ASYNC_LOGGER_EXCEPTION_HANDLER_PROPERTY)));
  }

  @Test
  public void customExceptionHandler() {
    final String customHandler = "custom";
    System.setProperty(ASYNC_LOGGER_EXCEPTION_HANDLER_PROPERTY, customHandler);
    new MuleLog4jContextFactory();
    assertThat(customHandler, equalTo(getProperty(ASYNC_LOGGER_EXCEPTION_HANDLER_PROPERTY)));
  }

  @Test
  public void dispose() {
    ArtifactAwareContextSelector contextSelector = mock(ArtifactAwareContextSelector.class);
    MuleLog4jContextFactory factory = new MuleLog4jContextFactory(contextSelector);
    factory.dispose();
    verify(contextSelector).dispose();
  }
}
