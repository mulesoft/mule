/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_TERMINATION_LOG_PATH_PROPERTY;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.APP_DEPLOYMENT;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.SingleAppDeploymentStory.SINGLE_APP_DEPLOYMENT;

import static java.lang.System.clearProperty;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.io.File.createTempFile;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(APP_DEPLOYMENT)
@Story(SINGLE_APP_DEPLOYMENT)
public class WriteToRouteTerminationHandlerTestCase {

  public static final String TEST_TERMINATION_FILE_LOG_MESSAGE = "Test termination file was written";

  private static File getTempFileForTerminationLog() throws RuntimeException {
    try {
      return createTempFile("test-termination-handler", "log");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void terminationLogPathIsTakenFromEnvVariable() throws IOException {
    Consumer<Throwable> shutdownConsumer = mock(Consumer.class);
    WriteToRouteTerminationHandler writeToRouteTerminationHandlerTestCase = new WriteToRouteTerminationHandler(shutdownConsumer);
    RuntimeException terminationThrowable = new RuntimeException(TEST_TERMINATION_FILE_LOG_MESSAGE);
    writeToRouteTerminationHandlerTestCase.accept(terminationThrowable);

    String terminationFileContent = new String(readAllBytes(get(getProperty("java.io.tmpdir") + getProperty("file.separator")
        + "termination_log_set_by_env_property")));
    assertThat(terminationFileContent, equalTo(TEST_TERMINATION_FILE_LOG_MESSAGE));
    verify(shutdownConsumer).accept(terminationThrowable);
  }

  @Test
  public void terminationLogPathIsTakenFromSysPropOverridingEnvVariable() throws IOException {
    File terminationFile = getTempFileForTerminationLog();
    try {
      setProperty(MULE_TERMINATION_LOG_PATH_PROPERTY, terminationFile.getPath());
      Consumer<Throwable> shutdownConsumer = mock(Consumer.class);
      WriteToRouteTerminationHandler writeToRouteTerminationHandlerTestCase =
          new WriteToRouteTerminationHandler(shutdownConsumer);
      RuntimeException terminationThrowable = new RuntimeException(TEST_TERMINATION_FILE_LOG_MESSAGE);
      writeToRouteTerminationHandlerTestCase.accept(terminationThrowable);
      String terminationFileContent = new String(readAllBytes(get(terminationFile.getPath())));
      assertThat(terminationFileContent, equalTo(TEST_TERMINATION_FILE_LOG_MESSAGE));
      verify(shutdownConsumer).accept(terminationThrowable);
    } finally {
      clearProperty(MULE_TERMINATION_LOG_PATH_PROPERTY);
    }
  }
}
