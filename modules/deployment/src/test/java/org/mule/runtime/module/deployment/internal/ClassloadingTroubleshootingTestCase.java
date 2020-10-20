/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static java.lang.System.getProperty;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.core.api.util.ClassUtils.MULE_DESIGN_MODE;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsUrl;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.tck.probe.PollingProber.DEFAULT_POLLING_INTERVAL;
import static org.mule.tck.util.CompilerUtils.JarCompiler;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static uk.org.lidalia.slf4jtest.TestLoggerFactory.getTestLogger;

import io.qameta.allure.Feature;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.runtime.core.internal.exception.OnErrorPropagateHandler;
import org.mule.runtime.core.internal.logging.LogUtil;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.DeployableFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.DomainFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactoryResetRule;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Contains test for application classloading isolation scenarios
 */
@Feature(CLASSLOADING_ISOLATION)
public class ClassloadingTroubleshootingTestCase extends AbstractDeploymentTestCase {

  private static final int EXPECTED_CONTENT_IN_LOG_SECS = 60 * 1000;

  private final ApplicationFileBuilder APP_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER =
      new ApplicationFileBuilder("app-classloading-troubleshooting")
          .definedBy("classloading-troubleshooting/app-classloading-troubleshooting-config.xml");

  private final DomainFileBuilder DOMAIN_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER =
      new DomainFileBuilder("domain-classloading-troubleshooting")
          .definedBy("classloading-troubleshooting/domain-classloading-troubleshooting-config.xml");

  private JarFileBuilder overriderLibrary;
  private JarFileBuilder overrider2Library;
  private JarFileBuilder overriderTestLibrary;
  private JarFileBuilder muleJavaModulePlugin;

  @Rule
  public SystemProperty muleDesignModeSystemProperty = new SystemProperty(MULE_DESIGN_MODE, "true");

  @Rule
  public TestLoggerFactoryResetRule testLoggerFactoryResetRule = new TestLoggerFactoryResetRule();

  private TestLogger logUtilLogger = getTestLogger(LogUtil.class);

  private TestLogger onErrorPropagateHandlerLogger = getTestLogger(OnErrorPropagateHandler.class);

  public ClassloadingTroubleshootingTestCase(boolean parallelDeployment) {
    super(parallelDeployment);
  }

  @Parameters(name = "Parallel: {0}")
  public static List<Boolean> params() {
    // Only run without parallel deployment since this configuration does not affect re-deployment at all
    return asList(false);
  }

  @Before
  public void setup() throws URISyntaxException {
    overriderLibrary =
        new JarFileBuilder("overrider-library",
                           new JarCompiler().compiling(getResourceFile("/classloading-troubleshooting/src/OverrideMe.java"))
                               .compile("overrider-library.jar"));
    overrider2Library =
        new JarFileBuilder("overrider2-library",
                           new JarCompiler().compiling(getResourceFile("/classloading-troubleshooting/src/OverrideMe2.java"))
                               .compile("overrider2-library.jar"));

    overriderTestLibrary =
        new JarFileBuilder("overrider-test-library",
                           new JarCompiler().compiling(getResourceFile("/classloading-troubleshooting/src/test/OverrideMe.java"))
                               .compile("overrider-test-library.jar"));

    muleJavaModulePlugin =
        new JarFileBuilder("mule-java-module", new File(getProperty("muleJavaModule"))).withGroupId("org.mule.module")
            .withClassifier(MULE_PLUGIN_CLASSIFIER).withVersion("1.3.0-SNAPSHOT");
  }

  @Test
  public void domainResourceNotFoundInConfigProperties() throws Exception {
    addOverrideLibrary(DOMAIN_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER);

    addPackedDomainFromBuilder(DOMAIN_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER);

    startDeployment();

    assertDeploymentFailure(domainDeploymentListener, DOMAIN_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER.getId());

    assertExpectedContentInAppLog(logUtilLogger,
                                  "classloading-troubleshooting/errors/domain-config-yaml-not-found");
  }

  @Test
  public void domainClassNotFoundInObject() throws Exception {
    addDomainConfigYamlFile(DOMAIN_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER);

    addPackedDomainFromBuilder(DOMAIN_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER);

    startDeployment();

    assertDeploymentFailure(domainDeploymentListener, DOMAIN_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER.getId());

    assertExpectedContentInAppLog(logUtilLogger,
                                  "classloading-troubleshooting/errors/domain-overrideme-class-not-found");
  }

  @Test
  public void applicationClassNotFound() throws Exception {
    completeDomain();

    APP_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER
        .dependingOn(muleJavaModulePlugin)
        .dependingOn(DOMAIN_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER);

    deployDomainAndApplication(DOMAIN_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER, APP_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER);

    assertDeploymentSuccess(domainDeploymentListener, DOMAIN_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER.getId());

    assertDeploymentFailure(applicationDeploymentListener, APP_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER.getId());

    assertExpectedContentInAppLog(logUtilLogger,
                                  "classloading-troubleshooting/errors/app-overrideme2-class-not-found");
  }

  @Test
  public void applicationClassNotFoundButInDomain() throws Exception {
    completeDomain();

    APP_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER
        .dependingOn(muleJavaModulePlugin)
        .dependingOn(overrider2Library)
        .dependingOn(DOMAIN_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER);

    deployDomainAndApplication(DOMAIN_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER, APP_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER);

    assertDeploymentSuccess(domainDeploymentListener, DOMAIN_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER.getId());

    assertDeploymentSuccess(applicationDeploymentListener, APP_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER.getId());

    assertExpectedContentInAppLog(onErrorPropagateHandlerLogger,
                                  "classloading-troubleshooting/errors/app-test-overrideme-class-not-found");
  }

  @Test
  @Ignore
  public void applicationResourceNotFoundButInDomain() throws Exception {
    completeDomain();

    APP_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER
        .dependingOn(muleJavaModulePlugin)
        .dependingOn(overrider2Library)
        .dependingOn(DOMAIN_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER);

    deployDomainAndApplication(DOMAIN_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER, APP_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER);

    assertDeploymentFailure(applicationDeploymentListener, APP_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER.getId());
  }

  private void completeDomain() {
    addOverrideLibrary(addDomainConfigYamlFile(DOMAIN_CLASSLOADING_TROUBLESHOOTING_FILE_BUILDER))
        .dependingOn(overriderTestLibrary);
    //.containingResource("classloading-troubleshooting/jms.properties", "jms.properties");
  }

  private <T extends DeployableFileBuilder<T>> DeployableFileBuilder<T> addOverrideLibrary(DeployableFileBuilder<T> deployableFileBuilder) {
    return deployableFileBuilder.dependingOn(overriderLibrary);
  }

  private <T extends DeployableFileBuilder<T>> DeployableFileBuilder<T> addDomainConfigYamlFile(DeployableFileBuilder<T> deployableFileBuilder) {
    return deployableFileBuilder.containingResource("classloading-troubleshooting/domain-config.yaml", "domain-config.yaml");
  }

  private <T extends DeployableFileBuilder<T>> DeployableFileBuilder<T> addJmsPropertiesResourceFile(DeployableFileBuilder<T> deployableFileBuilder) {
    return deployableFileBuilder.containingResource("classloading-troubleshooting/jms.properties", "jms.properties");
  }

  private void deployDomainAndApplication(DomainFileBuilder domainFileBuilder,
                                          ApplicationFileBuilder applicationFileBuilder)
      throws Exception {
    assertThat("Application should depend on domain",
               applicationFileBuilder.getDependencies().contains(domainFileBuilder), is(true));

    // Add domain
    addExplodedDomainFromBuilder(domainFileBuilder, domainFileBuilder.getId());

    // Deploy an application (exploded)
    addExplodedAppFromBuilder(applicationFileBuilder);

    startDeployment();
  }

  private void assertExpectedContentInAppLog(TestLogger logger, String fileLocation) throws Exception {
    File logContent =
        new File(getResourceAsUrl(fileLocation, ClassloadingTroubleshootingTestCase.class)
            .toURI());

    final String expectedErrorLog = readFileToString(logContent);

    new PollingProber(EXPECTED_CONTENT_IN_LOG_SECS, DEFAULT_POLLING_INTERVAL)
        .check(new Probe() {

          @Override
          public boolean isSatisfied() {
            try {
              assertThat(contains(logger.getAllLoggingEvents(), expectedErrorLog), is(true));
              return true;
            } catch (Exception e) {
              return false;
            }
          }

          @Override
          public String describeFailure() {
            return "expected content not found.";
          }
        });
  }

  private boolean contains(List<LoggingEvent> events, String content) {
    return events.stream().anyMatch(event -> event.getMessage().contains(content));
  }
}
