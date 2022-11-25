/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.functional.junit4.matchers.ThrowableRootCauseMatcher.hasRootCause;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_DYNAMIC_CONFIG_REF_PROPERTY;
import static org.mule.tck.junit4.matcher.EventMatcher.hasMessage;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.DOMAIN_DEPLOYMENT;

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
import org.mule.runtime.module.service.builder.ServiceFileBuilder;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.weave.v2.el.WeaveServiceProvider;

import java.io.File;
import java.net.URL;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Contains tests for domain deployment that require a functioning expression language service
 */
@Feature(DOMAIN_DEPLOYMENT)
public class DomainDeploymentWithELServiceTestCase extends AbstractDeploymentTestCase {

  @ClassRule
  public static SystemProperty enableDynamicConfigRef = new SystemProperty(ENABLE_DYNAMIC_CONFIG_REF_PROPERTY, "true");

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

  @Override
  protected File getExpressionLanguageServiceFile(File tempFolder) {
    final URL weaveJarUrl = WeaveServiceProvider.class.getProtectionDomain().getCodeSource().getLocation();
    final File defaulServiceSchedulerJarFile = getResourceFile(weaveJarUrl.getFile(), tempFolder);

    return new ServiceFileBuilder("expressionLanguageService")
        .withServiceProviderClass("org.mule.weave.v2.el.WeaveServiceProvider")
        .forContract("org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService")
        .usingLibrary(defaulServiceSchedulerJarFile.getAbsolutePath())
        .unpack(true)
        .getArtifactFile();
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
