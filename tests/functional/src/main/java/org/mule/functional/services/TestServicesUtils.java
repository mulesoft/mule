/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.services;

import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.exception.MuleRuntimeException;
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
    final File targetFile = new File(tempFolder, resource);
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
        .compiling(getResourceFile("/org/mule/service/scheduler/MockScheduler.java", tempFolder),
                   getResourceFile("/org/mule/service/scheduler/MockSchedulerService.java", tempFolder),
                   getResourceFile("/org/mule/service/scheduler/MockSchedulerServiceProvider.java", tempFolder))
        .compile("mule-module-service-mock-scheduler-1.0-SNAPSHOT.jar");

    return new ServiceFileBuilder("schedulerService")
        .withServiceProviderClass("org.mule.service.scheduler.MockSchedulerServiceProvider")
        .usingLibrary(defaulServiceSchedulerJarFile.getAbsolutePath()).getArtifactFile();
  }

  /**
   * Provides a packaged mock {@link DefaultExpressionLanguageFactoryService} implementation.
   *
   * @param tempFolder where to generate temporary files needed for compilation of the service classes.
   * @return the zip service file
   */
  public static File buildExpressionLanguageServiceFile(File tempFolder) {
    final File defaulServiceSchedulerJarFile = new CompilerUtils.JarCompiler()
        .compiling(getResourceFile("/org/mule/service/el/MockExpressionLanguage.java", tempFolder),
                   getResourceFile("/org/mule/service/el/MockExpressionLanguageFactoryService.java", tempFolder),
                   getResourceFile("/org/mule/service/el/MockExpressionLanguageFactoryServiceProvider.java", tempFolder))
        .compile("mule-module-service-mock-expression-language-1.0-SNAPSHOT.jar");

    return new ServiceFileBuilder("expressionLanguageService")
        .withServiceProviderClass("org.mule.service.el.MockExpressionLanguageFactoryServiceProvider")
        .usingLibrary(defaulServiceSchedulerJarFile.getAbsolutePath()).getArtifactFile();
  }

  /**
   * Provides a packaged mock {@link HttpService} implementation.
   *
   * @param tempFolder where to generate temporary files needed for compilation of the service classes.
   * @return the zip service file
   */
  public static File buildHttpServiceFile(File tempFolder) {
    final File defaulServiceSchedulerJarFile = new CompilerUtils.JarCompiler()
        .compiling(getResourceFile("/org/mule/service/http/MockHttpService.java", tempFolder),
                   getResourceFile("/org/mule/service/http/MockHttpServiceProvider.java", tempFolder))
        .compile("mule-module-service-mock-http-1.0-SNAPSHOT.jar");

    return new ServiceFileBuilder("http-service")
        .withServiceProviderClass("org.mule.service.http.MockHttpServiceProvider")
        .usingLibrary(defaulServiceSchedulerJarFile.getAbsolutePath()).getArtifactFile();
  }
}
