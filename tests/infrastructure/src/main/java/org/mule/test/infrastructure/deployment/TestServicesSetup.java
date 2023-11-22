/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.deployment;

import static org.mule.test.infrastructure.deployment.TestArtifactsCatalog.expressionLanguageMetadataServiceJarFile;
import static org.mule.test.infrastructure.deployment.TestArtifactsCatalog.expressionLanguageServiceJarFile;
import static org.mule.test.infrastructure.deployment.TestArtifactsCatalog.httpServiceJarFile;
import static org.mule.test.infrastructure.deployment.TestArtifactsCatalog.schedulerServiceJarFile;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;

/**
 * TestRule used to build the mule services before executing the tests. It's intended to be used as a {@link org.junit.ClassRule}
 * instead of using it as a {@link org.junit.Rule}, because the service building process is time expensive.
 * <p>
 * After building the services, you can use the methods {@link #overrideSchedulerService(Function)},
 * {@link #overrideExpressionLanguageMetadataService(Function)}, and {@link #overrideExpressionLanguageService(Function)} to
 * override the default test implementations. You have to pass a {@link Function} that receives the folder where the service
 * should be built, and builds the service.
 * <p>
 * Once that you created this rule, and optionally overwrote some services, you have to call {@link #copyServicesToFolder(File)}
 * in order to copy the implementations to the corresponding folder. This allows you to use only one muleHome folder per suite or
 * one per test, according to your needs.
 * <p>
 * Note: After some profiling in the deployment test cases, we noticed that the expensive part of setting the services up is to
 * compile and build them, and not copying them to the muleHome folder.
 */
public class TestServicesSetup extends ExternalResource {

  public static final String EXPRESSION_LANGUAGE_SERVICE_NAME = "expressionLanguageService";
  public static final String EXPRESSION_LANGUAGE_METADATA_SERVICE_NAME = "expressionLanguageMetadataService";
  public static final String SCHEDULER_SERVICE_NAME = "schedulerService";

  private final TemporaryFolder compilerWorkFolder;

  private File schedulerService;
  private File httpService;
  private File expressionLanguageService;
  private File expressionLanguageMetadataService;
  private boolean expressionLanguageMetadataServiceDisabled;

  public TestServicesSetup(TemporaryFolder compilerWorkFolder) {
    this.compilerWorkFolder = compilerWorkFolder;
  }

  /**
   * Allows to override the scheduler service implementation to be used in the test suite.
   * 
   * @param supplier a function that receives the folder where the service should be built and returns the artifact file.
   * @throws IOException if the temp folder for the service couldn't be created.
   */
  public void overrideSchedulerService(Function<File, File> supplier) throws IOException {
    this.schedulerService = supplier.apply(compilerWorkFolder.newFolder(SCHEDULER_SERVICE_NAME + "_override"));
  }

  /**
   * Allows to override the expression language service implementation to be used in the test suite.
   * 
   * @param supplier a function that receives the folder where the service should be built and returns the artifact file.
   * @throws IOException if the temp folder for the service couldn't be created.
   */
  public void overrideExpressionLanguageService(Function<File, File> supplier) throws IOException {
    this.expressionLanguageService = supplier.apply(compilerWorkFolder.newFolder(EXPRESSION_LANGUAGE_SERVICE_NAME + "_override"));
  }

  /**
   * Allows to override the expression language metadata service implementation to be used in the test suite.
   * 
   * @param supplier a function that receives the folder where the service should be built and returns the artifact file.
   * @throws IOException if the temp folder for the service couldn't be created.
   */
  public void overrideExpressionLanguageMetadataService(Function<File, File> supplier) throws IOException {
    this.expressionLanguageMetadataService =
        supplier.apply(compilerWorkFolder.newFolder(EXPRESSION_LANGUAGE_METADATA_SERVICE_NAME + "_override"));
  }

  /**
   * Allows to disable the expression language metadata service implementation to be used in the test suite. This is useful if the
   * ExpressionLanguageService already provides the metadata service, as DataWeave does.
   */
  public void disableExpressionLanguageMetadataService() throws IOException {
    this.expressionLanguageMetadataServiceDisabled = true;
  }

  public File getSchedulerService() {
    return schedulerService;
  }

  public File getHttpService() {
    return httpService;
  }

  public File getExpressionLanguageService() {
    return expressionLanguageService;
  }

  public File getExpressionLanguageMetadataService() {
    return expressionLanguageMetadataService;
  }

  public void initNotOverriddenServices() throws IOException {
    if (schedulerService == null) {
      schedulerService = schedulerServiceJarFile;
    }
    if (httpService == null) {
      httpService = httpServiceJarFile;
    }
    if (expressionLanguageService == null) {
      expressionLanguageService = expressionLanguageServiceJarFile;
    }
    if (!expressionLanguageMetadataServiceDisabled && expressionLanguageMetadataService == null) {
      expressionLanguageMetadataService = expressionLanguageMetadataServiceJarFile;
    }
  }

  @Override
  protected void after() {
    if (schedulerService != null && !schedulerService.equals(schedulerServiceJarFile)) {
      schedulerService.delete();
      schedulerService = null;
    }
    if (httpService != null && !httpService.equals(httpServiceJarFile)) {
      httpService.delete();
      httpService = null;
    }
    if (expressionLanguageService != null && !expressionLanguageService.equals(expressionLanguageServiceJarFile)) {
      expressionLanguageService.delete();
      expressionLanguageService = null;
    }
    if (!expressionLanguageMetadataServiceDisabled && expressionLanguageMetadataService != null
        && !expressionLanguageMetadataService.equals(expressionLanguageMetadataServiceJarFile)) {
      expressionLanguageMetadataService.delete();
      expressionLanguageMetadataService = null;
    }
  }

  public void reset() {
    schedulerService = null;
    httpService = null;
    expressionLanguageService = null;
    expressionLanguageMetadataService = null;
    expressionLanguageMetadataServiceDisabled = false;
  }
}
