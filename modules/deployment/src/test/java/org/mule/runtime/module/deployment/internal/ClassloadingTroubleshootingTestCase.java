/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.container.api.MuleFoldersUtil.getMuleBaseFolder;
import static org.mule.runtime.core.api.util.ClassUtils.MULE_DESIGN_MODE;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsUrl;
import static org.mule.tck.probe.PollingProber.DEFAULT_POLLING_INTERVAL;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;

import org.mule.runtime.module.deployment.impl.internal.application.DefaultMuleApplication;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.DeployableFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.DomainFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultMuleDomain;
import org.mule.runtime.module.deployment.logging.MemoryAppenderResource;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.util.CompilerUtils.JarCompiler;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.StringJoiner;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import io.qameta.allure.Feature;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

@Feature(CLASSLOADING_ISOLATION)
public class ClassloadingTroubleshootingTestCase extends AbstractDeploymentTestCase {

  private static final int EXPECTED_CONTENT_IN_LOG_SECS = 10 * 1000;
  private static final String CLASSLOADER_MODEL_VERSION = "1.2.0";

  @Rule
  public SystemProperty muleDesignModeSystemProperty = new SystemProperty(MULE_DESIGN_MODE, "true");

  private File mavenRepoFolder;
  private JarFileBuilder overriderLibrary;
  private JarFileBuilder overrider2Library;
  private JarFileBuilder overriderTestLibrary;

  TestLogger loggerDefaultMuleDomain = TestLoggerFactory.getTestLogger(DefaultMuleDomain.class);
  TestLogger loggerDefaultArchiveDeployer = TestLoggerFactory.getTestLogger(DefaultArchiveDeployer.class);
  TestLogger loggerDefaultMuleApplication = TestLoggerFactory.getTestLogger(DefaultMuleApplication.class);

  public ClassloadingTroubleshootingTestCase(boolean parallelDeployment) {
    super(parallelDeployment);
  }

  @Parameters(name = "Parallel: {0}")
  public static List<Boolean> params() {
    return asList(false);
  }

  @Before
  public void setup() throws URISyntaxException {
    mavenRepoFolder = Paths.get(getMuleBaseFolder().getAbsolutePath(), "repository").toFile();
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

  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    TestLoggerFactory.clearAll();
  }

  @Test
  public void heavyDomainResourceNotFoundInConfigProperties() throws Exception {
    domainResourceNotFoundInConfigProperties(false);
  }

  @Test
  public void lightDomainResourceNotFoundInConfigProperties() throws Exception {
    domainResourceNotFoundInConfigProperties(true);
  }

  private void domainResourceNotFoundInConfigProperties(boolean useLightWeightPackage) throws Exception {
    DomainFileBuilder domainFileBuilder = createDomainFileBuilder(useLightWeightPackage);
    addOverrideLibrary(domainFileBuilder, useLightWeightPackage);
    addPackedDomainFromBuilder(domainFileBuilder);

    startDeployment();

    assertDeploymentFailure(domainDeploymentListener, domainFileBuilder.getId());
    assertThat(loggerDefaultArchiveDeployer.getAllLoggingEvents().stream().anyMatch(e -> e.getMessage()
        .equals("Failed to deploy artifact [domain-classloading-troubleshooting-1.0.0-mule-domain]")), is(true));
    assertExpectedContentInDomainLog("classloading-troubleshooting/errors/domain-config-yaml-not-found");
  }

  @Test
  public void heavyDomainClassNotFoundInObject() throws Exception {
    domainClassNotFoundInObject(false);
  }

  @Test
  public void lightDomainClassNotFoundInObject() throws Exception {
    domainClassNotFoundInObject(true);
  }

  private void domainClassNotFoundInObject(boolean useLightWeightPackage) throws Exception {
    DomainFileBuilder domainFileBuilder = createDomainFileBuilder(useLightWeightPackage);
    addDomainConfigYamlFile(domainFileBuilder);
    addPackedDomainFromBuilder(domainFileBuilder);

    startDeployment();

    assertDeploymentFailure(domainDeploymentListener, domainFileBuilder.getId());
    assertThat(loggerDefaultArchiveDeployer.getAllLoggingEvents().stream().anyMatch(e -> e.getMessage()
        .equals("Failed to deploy artifact [domain-classloading-troubleshooting-1.0.0-mule-domain]")), is(true));
    assertExpectedContentInDomainLog("classloading-troubleshooting/errors/domain-overrideme-class-not-found");
  }

  @Test
  public void heavyApplicationClassNotFound() throws Exception {
    applicationClassNotFound(false);
  }

  @Test
  public void lightApplicationClassNotFound() throws Exception {
    applicationClassNotFound(true);
  }

  private void applicationClassNotFound(boolean useLightWeightPackage) throws Exception {
    DomainFileBuilder domainFileBuilder = createDomainFileBuilder(useLightWeightPackage);
    completeDomain(domainFileBuilder, useLightWeightPackage);

    ApplicationFileBuilder applicationFileBuilder = createApplicationFileBuilder(useLightWeightPackage);
    addJmsPropertiesResourceFile(applicationFileBuilder)
        .dependingOn(domainFileBuilder);

    deployDomainAndApplication(domainFileBuilder, applicationFileBuilder);

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, applicationFileBuilder.getId());

    assertThat(loggerDefaultArchiveDeployer.getAllLoggingEvents().stream().anyMatch(e -> e.getMessage()
        .equals("Failed to deploy artifact [app-classloading-troubleshooting-1.0.0-mule-application]")), is(true));
    assertExpectedContentInAppLog("classloading-troubleshooting/errors/app-overrideme2-class-not-found");
  }

  @Test
  public void heavyApplicationClassNotFoundButInDomain() throws Exception {
    applicationClassNotFoundButInDomain(false);
  }

  @Test
  public void lightApplicationClassNotFoundButInDomain() throws Exception {
    applicationClassNotFoundButInDomain(true);
  }

  public void applicationClassNotFoundButInDomain(boolean useLightWeightPackage) throws Exception {
    DomainFileBuilder domainFileBuilder = createDomainFileBuilder(useLightWeightPackage);
    completeDomain(domainFileBuilder, useLightWeightPackage);

    ApplicationFileBuilder applicationFileBuilder = createApplicationFileBuilder(useLightWeightPackage);
    addJmsPropertiesResourceFile(
                                 addOverride2Library(applicationFileBuilder, useLightWeightPackage))
                                     .dependingOn(domainFileBuilder);

    deployDomainAndApplication(domainFileBuilder, applicationFileBuilder);

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, applicationFileBuilder.getId());

    assertThat(loggerDefaultArchiveDeployer.getAllLoggingEvents().stream().anyMatch(e -> e.getMessage()
        .equals("Failed to deploy artifact [app-classloading-troubleshooting-1.0.0-mule-application]")), is(true));
    assertExpectedContentInAppLog("classloading-troubleshooting/errors/app-test-overrideme-class-not-found");
  }

  @Test
  public void heavyApplicationResourceNotFoundButInDomain() throws Exception {
    applicationResourceNotFoundButInDomain(false);
  }

  @Test
  public void lightApplicationResourceNotFoundButInDomain() throws Exception {
    applicationResourceNotFoundButInDomain(true);
  }

  private void applicationResourceNotFoundButInDomain(boolean useLightWeightPackage) throws Exception {
    DomainFileBuilder domainFileBuilder = createDomainFileBuilder(useLightWeightPackage);
    completeDomain(domainFileBuilder, useLightWeightPackage);

    ApplicationFileBuilder applicationFileBuilder = createApplicationFileBuilder(useLightWeightPackage);
    addOverride2Library(applicationFileBuilder, useLightWeightPackage)
        .dependingOn(domainFileBuilder);

    deployDomainAndApplication(domainFileBuilder, applicationFileBuilder);

    assertDeploymentFailure(applicationDeploymentListener, applicationFileBuilder.getId());
    assertThat(loggerDefaultArchiveDeployer.getAllLoggingEvents().stream().anyMatch(e -> e.getMessage()
        .equals("Failed to deploy artifact [app-classloading-troubleshooting-1.0.0-mule-application]")), is(true));
    assertExpectedContentInAppLog("classloading-troubleshooting/errors/app-jms-properties-resource-not-found");
  }

  private DomainFileBuilder createDomainFileBuilder(boolean useLightWeightPackage) {
    DomainFileBuilder builder = new DomainFileBuilder("domain-classloading-troubleshooting")
        .definedBy("classloading-troubleshooting/domain-classloading-troubleshooting-config.xml")
        .withClassloaderModelVersion(CLASSLOADER_MODEL_VERSION);
    if (useLightWeightPackage) {
      builder.usingLightWeightPackage();
    }
    return builder;
  }


  private ApplicationFileBuilder createApplicationFileBuilder(boolean useLightWeightPackage) {
    ApplicationFileBuilder builder = new ApplicationFileBuilder("app-classloading-troubleshooting")
        .definedBy("classloading-troubleshooting/app-classloading-troubleshooting-config.xml")
        .withClassloaderModelVersion(CLASSLOADER_MODEL_VERSION);
    if (useLightWeightPackage) {
      builder.usingLightWeightPackage();
    }
    return builder;
  }

  private void completeDomain(DomainFileBuilder domainFileBuilder, boolean useLightWeightPackage) throws IOException {
    addOverriderTestLibrary(
                            addOverrideLibrary(
                                               addJmsPropertiesResourceFile(addDomainConfigYamlFile(domainFileBuilder)),
                                               useLightWeightPackage),
                            useLightWeightPackage);
  }

  private <T extends DeployableFileBuilder<T>> DeployableFileBuilder<T> addOverrideLibrary(DeployableFileBuilder<T> deployableFileBuilder,
                                                                                           boolean useLightWeightPackage)
      throws IOException {
    if (useLightWeightPackage) {
      addDependencyToRepository(overriderLibrary);
    }
    return deployableFileBuilder.dependingOn(overriderLibrary);
  }

  private <T extends DeployableFileBuilder<T>> DeployableFileBuilder<T> addOverride2Library(DeployableFileBuilder<T> deployableFileBuilder,
                                                                                            boolean useLightWeightPackage)
      throws IOException {
    if (useLightWeightPackage) {
      addDependencyToRepository(overrider2Library);
    }
    return deployableFileBuilder.dependingOn(overrider2Library);
  }

  private <T extends DeployableFileBuilder<T>> DeployableFileBuilder<T> addOverriderTestLibrary(DeployableFileBuilder<T> deployableFileBuilder,
                                                                                                boolean useLightWeightPackage)
      throws IOException {
    if (useLightWeightPackage) {
      addDependencyToRepository(overriderTestLibrary);
    }
    return deployableFileBuilder.dependingOn(overriderTestLibrary);
  }

  private <T extends DeployableFileBuilder<T>> DeployableFileBuilder<T> addDomainConfigYamlFile(DeployableFileBuilder<T> deployableFileBuilder) {
    return deployableFileBuilder.containingResource("classloading-troubleshooting/domain-config.yaml", "domain-config.yaml");
  }

  private <T extends DeployableFileBuilder<T>> DeployableFileBuilder<T> addJmsPropertiesResourceFile(DeployableFileBuilder<T> deployableFileBuilder) {
    return deployableFileBuilder.containingResource("classloading-troubleshooting/jms.properties", "jms.properties");
  }

  private void addDependencyToRepository(JarFileBuilder jarFileBuilder) throws IOException {
    String[] groupId = jarFileBuilder.getGroupId().split("\\.");
    File moduleGroupIdRepoFolder = Paths.get(mavenRepoFolder.getAbsolutePath(), groupId).toFile();
    String artifactId = jarFileBuilder.getArtifactId();
    String version = jarFileBuilder.getVersion();

    copyFile(jarFileBuilder.getArtifactPomFile(),
             Paths.get(moduleGroupIdRepoFolder.getAbsolutePath(), artifactId, version,
                       format("%s-%s.pom", artifactId, version))
                 .toFile());

    String classifierSuffix = ofNullable(jarFileBuilder.getClassifier()).map(s -> "-" + s).orElse("");
    copyFile(jarFileBuilder.getArtifactFile(),
             Paths.get(moduleGroupIdRepoFolder.getAbsolutePath(), artifactId, version,
                       format("%s-%s%s.jar", artifactId, version, classifierSuffix))
                 .toFile());
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

  private void assertExpectedContentInAppLog(String fileLocation) throws Exception {
    assertExpectedContentInLog(fileLocation, loggerDefaultMuleApplication);
  }

  private void assertExpectedContentInDomainLog(String fileLocation) throws Exception {
    assertExpectedContentInLog(fileLocation, loggerDefaultMuleDomain);
  }

  private void assertExpectedContentInLog(String fileLocation, TestLogger logger) throws Exception {
    File logContent =
        new File(getResourceAsUrl(fileLocation, ClassloadingTroubleshootingTestCase.class)
            .toURI());
    final String expectedErrorLog = readFileToString(logContent);
    new PollingProber(EXPECTED_CONTENT_IN_LOG_SECS, DEFAULT_POLLING_INTERVAL)
        .check(new Probe() {

          @Override
          public boolean isSatisfied() {
            try {
              assertThat(logger.getAllLoggingEvents().stream().anyMatch(e -> e.getMessage().equals("expectedErrorLog")),
                         is(true));
              return true;
            } catch (Exception e) {
              return false;
            }
          }

          @Override
          public String describeFailure() {
            final StringJoiner joiner = new StringJoiner(lineSeparator());
            logger.getAllLoggingEvents().forEach(e -> joiner.add(e.getMessage()));

            return "expected content ('" + expectedErrorLog + "') not found. Full log is:" + lineSeparator() + joiner.toString();
          }
        });
  }

}
