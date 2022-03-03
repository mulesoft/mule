/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.logging;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.internal.processor.LoggerMessageProcessor;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.internal.AbstractApplicationDeploymentTestCase;

import java.util.List;

import io.qameta.allure.Description;
import org.junit.Test;
import org.junit.runners.Parameterized;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

public class LoggingSeparationTestCase extends AbstractApplicationDeploymentTestCase {

  TestLogger logger = TestLoggerFactory.getTestLogger(LoggerMessageProcessor.class);

  @Parameterized.Parameters(name = "Parallel: {0}")
  public static List<Boolean> params() {
    return asList(false);
  }

  public LoggingSeparationTestCase(boolean parallelDeployment) {
    super(parallelDeployment);
  }

  @Test
  @Description("This test verifies that each application logs on their proper log file")
  public void whenLoggingEachAppShouldLogOnItsOwnThreadSuccessfully() throws Exception {
    final ApplicationFileBuilder loggingAppFileBuilder1 =
        appFileBuilder("logging-app-1").definedBy("logging-separation-app-1.xml");
    final ApplicationFileBuilder loggingAppFileBuilder2 =
        appFileBuilder("logging-app-2").definedBy("logging-separation-app-2.xml");

    addExplodedAppFromBuilder(loggingAppFileBuilder1);
    addExplodedAppFromBuilder(loggingAppFileBuilder2);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, loggingAppFileBuilder1.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, loggingAppFileBuilder2.getId());

    executeApplicationFlow("logging");

    assertThat(logger.getAllLoggingEvents().size(), is(1));
    assertThat("", logger.getAllLoggingEvents().get(0).getThreadName(), stringContainsInOrder("logging-app-1"));

    executeApplicationFlow("logging2", null, 1);

    assertThat(logger.getAllLoggingEvents().size(), is(2));
    assertThat("", logger.getAllLoggingEvents().get(1).getThreadName(), stringContainsInOrder("logging-app-2"));
  }
}
