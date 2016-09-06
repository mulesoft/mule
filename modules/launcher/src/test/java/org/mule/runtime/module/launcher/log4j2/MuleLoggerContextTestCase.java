/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.log4j2;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

import org.mule.functional.logging.TestAppender;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.size.SmallTest;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.async.AsyncLoggerConfig;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.message.MessageFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class MuleLoggerContextTestCase extends AbstractMuleTestCase {

  private static final String DEFAULT_CONTEXT_NAME = "Default";
  private static final String MESSAGE = "Do you wanna build a snowman?";
  private static final String CATEGORY = MuleLoggerContextTestCase.class.getName();
  private static final String TEST_APPENDER = "testAppender";
  private static final Level LEVEL = Level.ERROR;

  @Mock
  private ContextSelector contextSelector;

  @Mock
  private MessageFactory messageFactory;

  private MuleLoggerContext context;
  private TestAppender testAppender;

  @Before
  public void before() {
    context = getDefaultContext();
    testAppender = new TestAppender(TEST_APPENDER, null, null);
    context.getConfiguration().addAppender(testAppender);

    LoggerConfig loggerConfig =
        AsyncLoggerConfig.createLogger("false", LEVEL.name(), CATEGORY, "true",
                                       new AppenderRef[] {AppenderRef.createAppenderRef(TEST_APPENDER, null, null)}, null,
                                       context.getConfiguration(), null);

    loggerConfig.addAppender(testAppender, null, null);
    context.getConfiguration().addLogger(CATEGORY, loggerConfig);
    context.getConfiguration().start();
    context.updateLoggers();
  }

  @Test
  public void dispatchingLogger() {
    assertThat(context.newInstance(context, "", messageFactory), instanceOf(DispatchingLogger.class));
  }

  @Test
  public void reconfigureAsyncLoggers() {
    Logger logger = context.getLogger(CATEGORY);
    logger.error(MESSAGE);

    assertLogged();
    testAppender.clear();

    context.updateLoggers(context.getConfiguration());
    logger.error(MESSAGE);
    assertLogged();
  }

  private void assertLogged() {
    PollingProber pollingProber = new PollingProber(5000, 500);
    pollingProber.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        testAppender.ensure(new TestAppender.Expectation(LEVEL.name(), CATEGORY, MESSAGE));
        return true;
      }

      @Override
      public String describeFailure() {
        return "message was not logged";
      }
    });

  }

  private MuleLoggerContext getDefaultContext() {
    return new MuleLoggerContext(DEFAULT_CONTEXT_NAME, null, Thread.currentThread().getContextClassLoader(), contextSelector,
                                 true);
  }
}
