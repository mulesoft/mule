/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test.internal;

import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.functional.junit4.matchers.ThrowableRootCauseMatcher.hasRootCause;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_DYNAMIC_CONFIG_REF_PROPERTY;
import static org.mule.runtime.core.api.util.FileUtils.unzip;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.classloaderConfigConnectExtensionPlugin;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.classloaderConnectExtensionPlugin;
import static org.mule.tck.junit4.matcher.EventMatcher.hasMessage;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.DOMAIN_DEPLOYMENT;

import static java.lang.System.getProperty;
import static java.nio.file.Files.createTempDirectory;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import org.mule.functional.junit4.matchers.ThrowableMessageMatcher;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.DomainFileBuilder;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;

/**
 * Contains tests for domain deployment that require a functioning expression language service
 */
@Feature(DOMAIN_DEPLOYMENT)
public class DomainDeploymentWithELServiceTestCase extends AbstractDeploymentTestCase {

  @ClassRule
  public static SystemProperty enableDynamicConfigRef = new SystemProperty(ENABLE_DYNAMIC_CONFIG_REF_PROPERTY, "true");

  @BeforeClass
  public static void setUpELService() throws IOException {
    testServicesSetup
        .overrideExpressionLanguageService(DomainDeploymentWithELServiceTestCase::getRealExpressionLanguageServiceFile);
    testServicesSetup.disableExpressionLanguageMetadataService();
  }

  @AfterClass
  public static void tearDownELService() {
    testServicesSetup.reset();
  }

  private final DomainFileBuilder domainWithConfigsFileBuilder =
      new DomainFileBuilder("domain-with-configs")
          .definedBy("domain-with-configs-config.xml")
          .dependingOn(classloaderConfigConnectExtensionPlugin)
          .dependingOn(classloaderConnectExtensionPlugin);

  private final ApplicationFileBuilder appWithConfigRefToDomain = new ApplicationFileBuilder("app-with-config-ref-to-domain")
      .definedBy("app-with-config-ref-to-domain.xml")
      .dependingOn(domainWithConfigsFileBuilder)
      .dependingOn(classloaderConfigConnectExtensionPlugin);
  private final ApplicationFileBuilder appWithConfigRefExpressionToDomain =
      new ApplicationFileBuilder("app-with-config-ref-expr-to-domain")
          .definedBy("app-with-config-ref-expr-to-domain.xml")
          .dependingOn(domainWithConfigsFileBuilder)
          .dependingOn(classloaderConfigConnectExtensionPlugin);
  private final ApplicationFileBuilder appWithConfigRefToNonExistant =
      new ApplicationFileBuilder("app-with-config-ref-to-non-existent")
          .definedBy("app-with-config-ref-to-non-existent.xml")
          .dependingOn(domainWithConfigsFileBuilder)
          .dependingOn(classloaderConfigConnectExtensionPlugin);
  private final ApplicationFileBuilder appWithConfigRefToInvalid = new ApplicationFileBuilder("app-with-config-ref-to-invalid")
      .definedBy("app-with-config-ref-to-invalid.xml")
      .dependingOn(domainWithConfigsFileBuilder)
      .dependingOn(classloaderConfigConnectExtensionPlugin);

  public DomainDeploymentWithELServiceTestCase(boolean parallelDeployment) {
    super(parallelDeployment);
  }

  @After
  public void disposeStaleDomains() {
    TestDomainFactory.after();
  }

  private static File getRealExpressionLanguageServiceFile(File tempFolder) {
    try {
      // final URL weaveJarUrl = WeaveServiceProvider.class.getProtectionDomain().getCodeSource().getLocation();
      // String weaveSeriveFileName = Paths.get(weaveJarUrl.toURI()).getFileName().toString();
      // String weaveServiceArtifactId = weaveSeriveFileName.substring(0, weaveSeriveFileName.length() - 4);

      // Unpack the service because java doesn't allow to create a classloader with jars within a zip out of the box.
      File serviceExplodedDir;
      serviceExplodedDir = createTempDirectory("mule-service-weave").toFile();

      // URL serviceBundleUrl = weaveJarUrl;

      unzip(new File(getProperty("dataWeaveService")), serviceExplodedDir);
      return serviceExplodedDir;

    } catch (IOException e) {
      throw new IllegalStateException("Couldn't prepare RealExpressionLanguageService.", e);
    }
  }

  @Test
  @Description("Control test to verify that the deployment fails if the configuration reference does not exist, even if there is a parent domain")
  public void deployDomainWithConfigAndAppWithConfigRefToNonExistentShouldFail() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(domainWithConfigsFileBuilder);
    addPackedAppFromBuilder(appWithConfigRefToNonExistant);

    assertDeploymentSuccess(domainDeploymentListener, domainWithConfigsFileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, appWithConfigRefToNonExistant.getId());

    final String expectedMessage =
        "[mule-config.xml:10]: Referenced component 'test-config-non-existent' must be one of stereotypes [CLASSLOADER-CONFIG-CONNECT:CONFIG].";
    verify(applicationDeploymentListener)
        .onDeploymentFailure(eq(appWithConfigRefToNonExistant.getId()),
                             argThat(allOf(instanceOf(DeploymentException.class),
                                           hasRootCause(allOf(instanceOf(ConfigurationException.class),
                                                              ThrowableMessageMatcher.hasMessage(expectedMessage))))));
  }

  @Test
  @Ignore("W-11453332: un-ignore once the bug is fixed")
  @Description("Control test to verify that the deployment fails if the configuration reference is invalid, even if there is a parent domain")
  public void deployDomainWithConfigAndAppWithConfigRefToInvalidShouldFail() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(domainWithConfigsFileBuilder);
    addPackedAppFromBuilder(appWithConfigRefToInvalid);

    assertDeploymentSuccess(domainDeploymentListener, domainWithConfigsFileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, appWithConfigRefToInvalid.getId());

    final String expectedMessage =
        "[mule-config.xml:10]: Referenced component 'test-config-in-domain-with-invalid-type' must be one of stereotypes [CLASSLOADER-CONFIG-CONNECT:CONFIG].";
    verify(applicationDeploymentListener)
        .onDeploymentFailure(eq(appWithConfigRefToInvalid.getId()),
                             argThat(allOf(instanceOf(DeploymentException.class),
                                           hasRootCause(allOf(instanceOf(ConfigurationException.class),
                                                              ThrowableMessageMatcher.hasMessage(expectedMessage))))));
  }

  @Test
  @Description("Verifies that the deployment succeeds when the configuration reference is to a config defined in the parent domain")
  public void deployDomainWithConfigAndAppWithConfigRefToShared() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(domainWithConfigsFileBuilder);
    addPackedAppFromBuilder(appWithConfigRefToDomain);

    assertDeploymentSuccess(domainDeploymentListener, domainWithConfigsFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, appWithConfigRefToDomain.getId());

    CoreEvent result = executeApplicationFlow("flowWithReferenceToConfigInDomain", null);
    assertThat(result, hasMessage(hasPayload(is("this operation receives the FooConfig!"))));
  }

  @Test
  @Description("Verifies that the deployment succeeds when the configuration reference uses an expression resolving to a config defined in the parent domain")
  public void deployDomainWithConfigAndAppWithConfigRefExpressionToShared() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(domainWithConfigsFileBuilder);
    addPackedAppFromBuilder(appWithConfigRefExpressionToDomain);

    assertDeploymentSuccess(domainDeploymentListener, domainWithConfigsFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, appWithConfigRefExpressionToDomain.getId());

    CoreEvent result = executeApplicationFlow("flowWithExpressionReferenceToConfigInDomain", null);
    assertThat(result, hasMessage(hasPayload(is("this operation receives the FooConfig!"))));
  }
}
