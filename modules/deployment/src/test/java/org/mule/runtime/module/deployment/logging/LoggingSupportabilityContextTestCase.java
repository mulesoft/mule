/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.logging;

import static org.mule.test.allure.AllureConstants.Logging.LOGGING;
import static org.mule.test.allure.AllureConstants.Logging.LoggingStory.LOG_FORMAT;

import static java.util.Arrays.asList;

import static com.github.valfirst.slf4jtest.LoggingEvent.error;
import static com.github.valfirst.slf4jtest.TestLoggerFactory.getTestLogger;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

import org.mule.runtime.core.internal.processor.LoggerMessageProcessor;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.internal.AbstractApplicationDeploymentTestCase;

import java.util.HashMap;
import java.util.List;

import com.github.valfirst.slf4jtest.TestLogger;

import org.junit.Test;
import org.junit.runners.Parameterized;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(LOGGING)
@Story(LOG_FORMAT)
public class LoggingSupportabilityContextTestCase extends AbstractApplicationDeploymentTestCase {

  TestLogger logger = getTestLogger(LoggerMessageProcessor.class);

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

    assertThat(logger.getAllLoggingEvents(), hasItem(error(mdcMap, "I'm a logger")));
  }
}
