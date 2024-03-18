/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.deployment;

import static org.mule.functional.services.TestServicesUtils.buildExpressionLanguageMetadataServiceFile;
import static org.mule.functional.services.TestServicesUtils.buildExpressionLanguageServiceFile;
import static org.mule.functional.services.TestServicesUtils.buildHttpServiceFile;
import static org.mule.functional.services.TestServicesUtils.buildSchedulerServiceFile;
import static org.mule.test.infrastructure.deployment.TestServicesSetup.EXPRESSION_LANGUAGE_METADATA_SERVICE_NAME;
import static org.mule.test.infrastructure.deployment.TestServicesSetup.EXPRESSION_LANGUAGE_SERVICE_NAME;
import static org.mule.test.infrastructure.deployment.TestServicesSetup.SCHEDULER_SERVICE_NAME;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;

public final class TestArtifactsCatalog extends ExternalResource {

  public static File schedulerServiceJarFile;
  public static File httpServiceJarFile;
  public static File expressionLanguageServiceJarFile;
  public static File expressionLanguageMetadataServiceJarFile;

  private static TemporaryFolder compilerWorkFolder;

  public TestArtifactsCatalog(TemporaryFolder compilerWorkFolder) {
    TestArtifactsCatalog.compilerWorkFolder = compilerWorkFolder;
  }

  @Override
  protected void before() throws Throwable {
    super.before();

    if (schedulerServiceJarFile != null) {
      // avoid recompiling everything
      return;
    }

    try {
      initFiles();
    } catch (URISyntaxException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void initFiles() throws URISyntaxException, IOException {
    httpServiceJarFile = buildHttpServiceFile(compilerWorkFolder.newFolder("httpService"));
    schedulerServiceJarFile = buildSchedulerServiceFile(compilerWorkFolder.newFolder(SCHEDULER_SERVICE_NAME));
    expressionLanguageServiceJarFile =
        buildExpressionLanguageServiceFile(compilerWorkFolder.newFolder(EXPRESSION_LANGUAGE_SERVICE_NAME));
    expressionLanguageMetadataServiceJarFile =
        buildExpressionLanguageMetadataServiceFile(compilerWorkFolder.newFolder(EXPRESSION_LANGUAGE_METADATA_SERVICE_NAME));
  }

}
