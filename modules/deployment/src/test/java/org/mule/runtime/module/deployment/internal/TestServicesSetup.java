/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static org.mule.functional.services.TestServicesUtils.buildExpressionLanguageMetadataServiceFile;
import static org.mule.functional.services.TestServicesUtils.buildExpressionLanguageServiceFile;
import static org.mule.functional.services.TestServicesUtils.buildSchedulerServiceFile;

import static org.apache.commons.io.FileUtils.copyDirectory;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;

public class TestServicesSetup extends ExternalResource {

  private static final String EXPRESSION_LANGUAGE_SERVICE_NAME = "expressionLanguageService";
  private static final String EXPRESSION_LANGUAGE_METADATA_SERVICE_NAME = "expressionLanguageMetadataService";
  private static final String SCHEDULER_SERVICE_NAME = "schedulerService";

  private final TemporaryFolder compilerWorkFolder;

  private File schedulerService;
  private File expressionLanguageService;
  private File expressionLanguageMetadataService;

  public TestServicesSetup(TemporaryFolder compilerWorkFolder) {
    this.compilerWorkFolder = compilerWorkFolder;
  }

  public void overrideSchedulerService(Function<File, File> supplier) throws IOException {
    this.schedulerService = supplier.apply(compilerWorkFolder.newFolder(SCHEDULER_SERVICE_NAME));
  }

  public void overrideExpressionLanguageService(Function<File, File> supplier) throws IOException {
    this.expressionLanguageService = supplier.apply(compilerWorkFolder.newFolder(EXPRESSION_LANGUAGE_SERVICE_NAME));
  }

  public void overrideExpressionLanguageMetadataService(Function<File, File> supplier) throws IOException {
    this.expressionLanguageMetadataService =
        supplier.apply(compilerWorkFolder.newFolder(EXPRESSION_LANGUAGE_METADATA_SERVICE_NAME));
  }

  public void copyServicesToFolder(File servicesFolder) throws IOException {
    initNotOverriddenServices();

    copyDirectory(schedulerService,
                  new File(servicesFolder, SCHEDULER_SERVICE_NAME));
    copyDirectory(expressionLanguageService,
                  new File(servicesFolder, EXPRESSION_LANGUAGE_SERVICE_NAME));
    copyDirectory(expressionLanguageMetadataService,
                  new File(servicesFolder, EXPRESSION_LANGUAGE_METADATA_SERVICE_NAME));
  }

  private void initNotOverriddenServices() throws IOException {
    if (schedulerService == null) {
      schedulerService = buildSchedulerServiceFile(compilerWorkFolder.newFolder(SCHEDULER_SERVICE_NAME));
    }
    if (expressionLanguageService == null) {
      expressionLanguageService =
          buildExpressionLanguageServiceFile(compilerWorkFolder.newFolder(EXPRESSION_LANGUAGE_SERVICE_NAME));
    }
    if (expressionLanguageMetadataService == null) {
      expressionLanguageMetadataService =
          buildExpressionLanguageMetadataServiceFile(compilerWorkFolder.newFolder(EXPRESSION_LANGUAGE_METADATA_SERVICE_NAME));
    }
  }

  @Override
  protected void after() {
    if (schedulerService == null) {
      schedulerService.delete();
      schedulerService = null;
    }
    if (expressionLanguageService == null) {
      expressionLanguageService.delete();
      expressionLanguageService = null;
    }
    if (expressionLanguageMetadataService == null) {
      expressionLanguageMetadataService.delete();
      expressionLanguageMetadataService = null;
    }
  }
}
