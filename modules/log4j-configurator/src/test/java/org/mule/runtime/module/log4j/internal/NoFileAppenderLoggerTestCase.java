/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.log4j.internal;

import static org.mule.runtime.module.log4j.internal.NoFileAppenderLogger.FILE_APPENDER_NAME;
import static org.mule.test.allure.AllureConstants.RuntimeEnvironment.RUNTIME_ENVIRONMENT;
import static org.mule.test.allure.AllureConstants.RuntimeEnvironment.RuntimeEnvironmentStory.SINGLE_APP_ENVIRONMENT;

import static java.util.Collections.singletonMap;

import static org.apache.logging.log4j.Level.ALL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.message.MessageFactory;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

import java.util.Collections;

@Feature(RUNTIME_ENVIRONMENT)
@Story(SINGLE_APP_ENVIRONMENT)
public class NoFileAppenderLoggerTestCase {

  @Test
  public void whenFileAppenderIsCreatedTheFileAppenderIsRemovedAndTheDefaultContainerAppenderIsSet() {
    LoggerContext loggerContext = mock(LoggerContext.class);
    MessageFactory messageFactory = mock(MessageFactory.class);
    Appender appender = mock(Appender.class);
    Configuration configuration = mock(Configuration.class);
    LoggerConfig loggerConfig = mock(LoggerConfig.class);
    when(loggerContext.getConfiguration()).thenReturn(configuration);
    when(configuration.getLoggerConfig(any())).thenReturn(loggerConfig);
    when(loggerConfig.getLevel()).thenReturn(ALL);
    Appender fileAppender = mock(Appender.class);
    when(loggerConfig.getAppenders()).thenReturn(singletonMap(FILE_APPENDER_NAME, fileAppender));
    when(fileAppender.getName()).thenReturn(FILE_APPENDER_NAME);
    Logger logger = new NoFileAppenderLogger(loggerContext, "test", messageFactory, appender);
    verify(configuration).addLoggerAppender(logger, appender);
    verify(loggerConfig).removeAppender(FILE_APPENDER_NAME);
  }
}
