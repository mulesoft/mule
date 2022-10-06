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
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class AbstractFakeMuleServerTestCase extends AbstractMuleTestCase {

  @Rule
  public TemporaryFolder muleHome = new TemporaryFolder();

  @Rule
  public TemporaryFolder compilerWorkFolder = new TemporaryFolder();

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
    muleServer.addZippedService(getExpressionLanguageMetadataService());
  }

  @After
  public void tearDown() throws Exception {
    if (muleServer != null) {
      muleServer.stop();
      muleServer = null;
    }
    shutdown();
  }

  protected File getExpressionLanguageService() throws IOException {
    return buildExpressionLanguageServiceFile(compilerWorkFolder.newFolder("expressionLanguageService"));
  }

  protected File getExpressionLanguageMetadataService() throws IOException {
    return buildExpressionLanguageMetadataServiceFile(compilerWorkFolder.newFolder("expressionLanguageMetadataService"));
  }

  protected File getSchedulerService() throws IOException {
    return buildSchedulerServiceFile(compilerWorkFolder.newFolder("schedulerService"));
  }

  protected File getHttpService() throws IOException {
    return buildHttpServiceFile(compilerWorkFolder.newFolder("httpService"));
  }
}
