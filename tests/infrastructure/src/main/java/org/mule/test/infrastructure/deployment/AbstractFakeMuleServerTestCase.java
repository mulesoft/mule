/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.deployment;

import static org.apache.logging.log4j.LogManager.shutdown;

import static org.mule.functional.services.TestServicesUtils.buildExpressionLanguageMetadataServiceFile;
import static org.mule.functional.services.TestServicesUtils.buildExpressionLanguageServiceFile;
import static org.mule.functional.services.TestServicesUtils.buildHttpServiceFile;
import static org.mule.functional.services.TestServicesUtils.buildSchedulerServiceFile;

import org.mule.runtime.container.api.MuleCoreExtension;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class AbstractFakeMuleServerTestCase extends AbstractMuleTestCase {

  @Rule
  public TemporaryFolder muleHome = new TemporaryFolder();

  @ClassRule
  public static final TemporaryFolder compilerWorkFolder = new TemporaryFolder();
  private static File cachedSchedulerService = null;
  private static File cachedHttpService = null;
  private static File cachedELService = null;
  private static File cachedELMService = null;

  protected FakeMuleServer muleServer;

  protected List<MuleCoreExtension> getCoreExtensions() {
    return new LinkedList<>();
  }

  @Before
  public void setUp() throws Exception {
    muleServer = new FakeMuleServer(muleHome.getRoot().getAbsolutePath(), getCoreExtensions());
    muleServer.addZippedService(getSchedulerService());
    muleServer.addZippedService(getHttpService());
    muleServer.addZippedService(getExpressionLanguageService());
    if (addExpressionLanguageMetadataService()) {
      muleServer.addZippedService(getExpressionLanguageMetadataService());
    }
  }

  protected boolean addExpressionLanguageMetadataService() {
    return true;
  }

  @After
  public void tearDown() throws Exception {
    if (muleServer != null) {
      muleServer.stop();
      muleServer = null;
    }
    shutdown();
  }

  @AfterClass
  public static void kill() {
    cachedELMService = null;
    cachedELService = null;
    cachedSchedulerService = null;
    cachedHttpService = null;
  }

  protected File getExpressionLanguageService() throws IOException {
    if (cachedELService == null) {
      cachedELService = buildExpressionLanguageServiceFile(compilerWorkFolder.newFolder("expressionLanguageService"));
    }
    return cachedELService;
  }

  protected File getExpressionLanguageMetadataService() throws IOException {
    if (cachedELMService == null) {
      cachedELMService = buildExpressionLanguageMetadataServiceFile(compilerWorkFolder.newFolder("expressionLanguageMetadataService"));
    }
    return cachedELMService;
  }

  protected File getSchedulerService() throws IOException {
    if (cachedSchedulerService == null) {
      cachedSchedulerService = buildSchedulerServiceFile(compilerWorkFolder.newFolder("schedulerService"));
    }
    return cachedSchedulerService;
  }

  protected File getHttpService() throws IOException {
    if (cachedHttpService == null) {
      cachedHttpService = buildHttpServiceFile(compilerWorkFolder.newFolder("httpService"));
    }
    return cachedHttpService;
  }
}
