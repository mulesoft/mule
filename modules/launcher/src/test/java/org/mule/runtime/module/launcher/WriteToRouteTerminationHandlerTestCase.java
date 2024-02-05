/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_TERMINATION_LOG_ROUTE_PROPERTY;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.APP_DEPLOYMENT;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.SingleAppDeploymentStory.SINGLE_APP_DEPLOYMENT;

import static java.io.File.createTempFile;
import static java.nio.file.Files.readAllBytes;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.function.Consumer;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;

@Feature(APP_DEPLOYMENT)
@Story(SINGLE_APP_DEPLOYMENT)
public class WriteToRouteTerminationHandlerTestCase {

  public static final String TEST_TERMINATION_FILE_LOG_MESSAGE = "Test termination file log message";
  @Rule
  public SystemProperty terminationFileProperty =
      new SystemProperty(MULE_TERMINATION_LOG_ROUTE_PROPERTY, getTerminationFile().getPath());

  @NotNull
  private static File getTerminationFile() throws RuntimeException {
    try {
      return createTempFile("test-termination-handler", "log");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void whenTerminationExceptionHandledTheTerminationFileIsWritten() throws IOException {
    Consumer<Throwable> shutdownConsumer = mock(Consumer.class);
    WriteToRouteTerminationHandler writeToRouteTerminationHandlerTestCase = new WriteToRouteTerminationHandler(shutdownConsumer);
    RuntimeException terminationThrowable = new RuntimeException(TEST_TERMINATION_FILE_LOG_MESSAGE);
    writeToRouteTerminationHandlerTestCase.accept(terminationThrowable);

    String terminationFileContent = new String(readAllBytes(Paths.get(terminationFileProperty.getValue())));
    assertThat(terminationFileContent, equalTo(TEST_TERMINATION_FILE_LOG_MESSAGE));
    verify(shutdownConsumer).accept(terminationThrowable);
  }
}
