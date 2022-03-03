/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.logging;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.org.lidalia.slf4jtest.LoggingEvent.error;

import org.mule.runtime.core.internal.processor.LoggerMessageProcessor;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.internal.AbstractApplicationDeploymentTestCase;

import java.util.HashMap;
import java.util.List;

import io.qameta.allure.Description;
import static org.hamcrest.Matchers.is;
import org.junit.Test;
import org.junit.runners.Parameterized;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

public class LoggingSupportabilityContextTestCase extends AbstractApplicationDeploymentTestCase {

  TestLogger logger = TestLoggerFactory.getTestLogger(LoggerMessageProcessor.class);

  @Parameterized.Parameters(name = "Parallel: {0}")
  public static List<Boolean> params() {
    return asList(false);
  }

  public LoggingSupportabilityContextTestCase(boolean parallelDeployment) {
    super(parallelDeployment);
  }

  @Test
  @Description("This test verifies that supportability information in logging entries is present")
  public void whenLoggingSupportabilityInformationShouldBePresent() throws Exception {
    String correlationId = "12353425";
    HashMap mdcMap = new HashMap();
    mdcMap.put("correlationId", correlationId);
    mdcMap.put("processorPath", "logging/processors/0");

    final ApplicationFileBuilder loggingAppFileBuilder =
        appFileBuilder("logging-app").definedBy("logging-supportability-app.xml");

    addExplodedAppFromBuilder(loggingAppFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, loggingAppFileBuilder.getId());

    executeApplicationFlow("logging", correlationId);

    assertThat(logger.getAllLoggingEvents(), is(asList(error(mdcMap, "I'm a logger"))));
  }
}
