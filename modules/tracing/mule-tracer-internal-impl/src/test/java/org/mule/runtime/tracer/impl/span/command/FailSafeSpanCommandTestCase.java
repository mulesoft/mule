/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.command;

import static org.mule.runtime.tracer.impl.span.command.FailsafeSpanCommand.getFailsafeSpanCommand;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.runtime.tracer.impl.span.command.FailsafeSpanCommand;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.slf4j.Logger;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class FailSafeSpanCommandTestCase {

  public static final String EXPECTED_WARNING_MESSAGE = "expectedWarningMessage";

  public static final RuntimeException TEST_EXCEPTION = new RuntimeException();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();


  @Test
  public void executePropagate() {
    Logger logger = getTestLogger();
    doExecuteFailsafeCommand(false, logger);
    verify(logger).warn(EXPECTED_WARNING_MESSAGE, TEST_EXCEPTION);
  }

  @Test
  public void executePropagateNoPropagate() {
    expectedException.expect(RuntimeException.class);
    Logger logger = getTestLogger();
    doExecuteFailsafeCommand(true, logger);
    verify(logger).warn(EXPECTED_WARNING_MESSAGE, TEST_EXCEPTION);
  }

  private static Logger getTestLogger() {
    Logger logger = mock(Logger.class);
    when(logger.isWarnEnabled()).thenReturn(true);
    return logger;
  }

  private void doExecuteFailsafeCommand(boolean propagateException, Logger logger) {
    FailsafeSpanCommand failsafeSpanCommand = getFailsafeSpanCommand(logger, EXPECTED_WARNING_MESSAGE, propagateException);
    failsafeSpanCommand.execute(() -> {
      throw TEST_EXCEPTION;
    });
  }
}
