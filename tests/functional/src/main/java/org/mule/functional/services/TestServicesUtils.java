/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.services;

import static java.lang.System.getProperty;

import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.module.service.builder.ServiceFileBuilder;
import org.mule.tck.util.CompilerUtils;

import java.io.File;
import java.io.IOException;

public class TestServicesUtils {

  private TestServicesUtils() {
    // Nothing to do
  }

  private static File getResourceFile(String resource, File tempFolder) {
    return getResourceFile(resource, tempFolder, resource);
  }

  private static File getResourceFile(String resource, File tempFolder, String targetFilePath) {
    final File targetFile = new File(tempFolder, targetFilePath);
    try {
      copyInputStreamToFile(TestServicesUtils.class.getResourceAsStream(resource), targetFile);
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }

    return targetFile;
  }

  /**
   * Provides a packaged mock {@link SchedulerService} implementation.
   *
   * @param tempFolder where to generate temporary files needed for compilation of the service classes.
   * @return the zip service file
   */
  public static File buildSchedulerServiceFile(File tempFolder) {
    final File defaulServiceSchedulerJarFile = new CompilerUtils.JarCompiler()
        .targetJavaVersion(17)
        .compiling(getResourceFile("/org/mule/service/scheduler/MockScheduler.java", tempFolder),
                   getResourceFile("/org/mule/service/scheduler/MockSchedulerService.java", tempFolder),
                   getResourceFile("/org/mule/service/scheduler/MockSchedulerServiceProvider.java", tempFolder),
                   getResourceFile("/org/mule/service/scheduler/MockSchedulerView.java", tempFolder),
                   getResourceFile("/org/mule/service/scheduler/module-info.java", tempFolder, "module-info.java"))
        .including(getResourceFile("/org/mule/service/scheduler/MANIFEST.MF", tempFolder),
                   "META-INF/MANIFEST.MF")
        .dependingOn(new File(getProperty("apiAnnotationsLib")))
        .compile("mule-module-service-mock-scheduler-1.0-SNAPSHOT.jar");

    return new ServiceFileBuilder("schedulerService")
        .withServiceProviderClass("org.mule.service.scheduler.MockSchedulerServiceProvider")
        .forContract("org.mule.runtime.api.scheduler.SchedulerService")
        .usingLibrary(defaulServiceSchedulerJarFile.getAbsolutePath())
        .unpack(true)
        .getArtifactFile();
  }

  /**
   * Provides a packaged mock {@link DefaultExpressionLanguageFactoryService} implementation.
   *
   * @param tempFolder where to generate temporary files needed for compilation of the service classes.
   * @return the zip service file
   */
  public static File buildExpressionLanguageServiceFile(File tempFolder) {
    final File defaulServiceSchedulerJarFile = new CompilerUtils.JarCompiler()
        .targetJavaVersion(17)
        .compiling(getResourceFile("/org/mule/service/el/MockExpressionLanguage.java", tempFolder),
                   getResourceFile("/org/mule/service/el/MockExpressionLanguageFactoryService.java", tempFolder),
                   getResourceFile("/org/mule/service/el/MockExpressionLanguageFactoryServiceProvider.java", tempFolder),
                   getResourceFile("/org/mule/service/el/module-info.java", tempFolder, "module-info.java"))
        .including(getResourceFile("/org/mule/service/el/MANIFEST.MF", tempFolder),
                   "META-INF/MANIFEST.MF")
        .dependingOn(new File(getProperty("apiAnnotationsLib")))
        .compile("mule-module-service-mock-expression-language-1.0-SNAPSHOT.jar");

    return new ServiceFileBuilder("expressionLanguageService")
        .withServiceProviderClass("org.mule.service.el.MockExpressionLanguageFactoryServiceProvider")
        .forContract("org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService")
        .usingLibrary(defaulServiceSchedulerJarFile.getAbsolutePath())
        .unpack(true)
        .getArtifactFile();
  }


  /**
   * Provides a packaged mock {@link ExpressionLanguageMetadataService} implementation.
   *
   * @param tempFolder where to generate temporary files needed for compilation of the service classes.
   * @return the zip service file
   */
  public static File buildExpressionLanguageMetadataServiceFile(File tempFolder) {
    final File defaultServiceJarFile = new CompilerUtils.JarCompiler()
        .targetJavaVersion(17)
        .compiling(getResourceFile("/org/mule/service/el/metadata/MockExpressionLanguageMetadataService.java", tempFolder),
                   getResourceFile("/org/mule/service/el/metadata/MockExpressionLanguageMetadataServiceProvider.java",
                                   tempFolder),
                   getResourceFile("/org/mule/service/el/metadata/module-info.java", tempFolder, "module-info.java"))
        .including(getResourceFile("/org/mule/service/el/metadata/MANIFEST.MF", tempFolder),
                   "META-INF/MANIFEST.MF")
        .dependingOn(new File(getProperty("apiAnnotationsLib")))
        .compile("mule-module-service-mock-expression-language-metadata-1.0-SNAPSHOT.jar");

    return new ServiceFileBuilder("expressionLanguageMetadataService")
        .withServiceProviderClass("org.mule.service.el.metadata.MockExpressionLanguageMetadataServiceProvider")
        .forContract("org.mule.runtime.api.metadata.ExpressionLanguageMetadataService")
        .usingLibrary(defaultServiceJarFile.getAbsolutePath())
        .unpack(true)
        .getArtifactFile();
  }

  /**
   * Provides a packaged mock {@link HttpService} implementation.
   *
   * @param tempFolder where to generate temporary files needed for compilation of the service classes.
   * @return the zip service file
   */
  public static File buildHttpServiceFile(File tempFolder) {
    final File defaulServiceSchedulerJarFile = new CompilerUtils.JarCompiler()
        .targetJavaVersion(17)
        .compiling(getResourceFile("/org/mule/service/http/mock/MockHttpService.java", tempFolder),
                   getResourceFile("/org/mule/service/http/mock/MockHttpServiceProvider.java", tempFolder),
                   getResourceFile("/org/mule/service/http/mock/module-info.java", tempFolder, "module-info.java"))
        .including(getResourceFile("/org/mule/service/http/mock/MANIFEST.MF", tempFolder),
                   "META-INF/MANIFEST.MF")
        .dependingOn(new File(getProperty("apiAnnotationsLib")))
        .compile("mule-module-service-mock-http-1.0-SNAPSHOT.jar");

    return new ServiceFileBuilder("http-service")
        .withServiceProviderClass("org.mule.service.http.mock.MockHttpServiceProvider")
        .forContract("org.mule.runtime.http.api.HttpService")
        .unpack(true)
        .usingLibrary(defaulServiceSchedulerJarFile.getAbsolutePath()).getArtifactFile();
  }
}
