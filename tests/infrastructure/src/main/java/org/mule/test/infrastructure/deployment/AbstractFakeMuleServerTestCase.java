/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.deployment;

import static org.mule.runtime.api.util.MuleSystemProperties.CLASSLOADER_CONTAINER_JPMS_MODULE_LAYER;

import static org.apache.logging.log4j.LogManager.shutdown;

import static org.junit.rules.RuleChain.outerRule;

import org.mule.runtime.config.api.properties.PropertiesResolverUtils;
import org.mule.runtime.container.api.MuleCoreExtension;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

public class AbstractFakeMuleServerTestCase extends AbstractMuleTestCase {

  @ClassRule
  public static SystemProperty duplicateProvidersLax =
      new SystemProperty(PropertiesResolverUtils.class.getName() + ".duplicateProvidersLax", "true");

  @Rule
  public SystemProperty jvmVersionExtensionEnforcementLoose =
      new SystemProperty("mule.jvm.version.extension.enforcement", "LOOSE");

  @Rule
  public SystemProperty classloaderContainerJpmsModuleLayer =
      new SystemProperty(CLASSLOADER_CONTAINER_JPMS_MODULE_LAYER, "" + classloaderContainerJpmsModuleLayer());

  @Rule
  public TemporaryFolder muleHome = new TemporaryFolder();

  public static final TemporaryFolder compilerWorkFolder = new TemporaryFolder();
  protected static TestArtifactsCatalog testArtifactsCatalog = new TestArtifactsCatalog(compilerWorkFolder);
  protected static TestServicesSetup testServicesSetup = new TestServicesSetup(compilerWorkFolder);

  @ClassRule
  public static RuleChain ruleChain = outerRule(compilerWorkFolder).around(testArtifactsCatalog).around(testServicesSetup);

  private static boolean areServicesInitialised = false;
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
    testServicesSetup.initNotOverriddenServices();
    initialiseServicesIfNeeded();
    muleServer.addZippedService(cachedSchedulerService);
    muleServer.addZippedService(cachedHttpService);
    muleServer.addZippedService(cachedELService);
    if (addExpressionLanguageMetadataService()) {
      muleServer.addZippedService(cachedELMService);
    }
  }

  // These tests just create a container form the test classpath. There are no modules involved in these tests, so code relying on
  // modules from the container doesn't work (i.e.: class.getModule().getLayer() returns null).
  protected boolean classloaderContainerJpmsModuleLayer() {
    return false;
  }

  protected boolean addExpressionLanguageMetadataService() {
    return !testServicesSetup.isExpressionLanguageMetadataServiceDisabled();
  }

  @After
  public void tearDown() throws Exception {
    if (muleServer != null) {
      muleServer.stop();
      muleServer = null;
    }
    shutdown();
  }

  private void initialiseServicesIfNeeded() throws IOException {
    if (!areServicesInitialised) {
      areServicesInitialised = true;
      cachedSchedulerService = getSchedulerService();
      cachedHttpService = getHttpService();
      cachedELService = getExpressionLanguageService();
      if (addExpressionLanguageMetadataService()) {
        cachedELMService = getExpressionLanguageMetadataService();
      }
    }
  }

  @AfterClass
  public static void nullifyCachedServices() {
    areServicesInitialised = false;
    cachedSchedulerService = null;
    cachedHttpService = null;
    cachedELService = null;
    cachedELMService = null;
  }

  protected File getExpressionLanguageService() throws IOException {
    return testServicesSetup.getExpressionLanguageService();
  }

  protected File getExpressionLanguageMetadataService() throws IOException {
    return testServicesSetup.getExpressionLanguageMetadataService();
  }

  protected File getSchedulerService() throws IOException {
    return testServicesSetup.getSchedulerService();
  }

  protected File getHttpService() throws IOException {
    return testServicesSetup.getHttpService();
  }

}
