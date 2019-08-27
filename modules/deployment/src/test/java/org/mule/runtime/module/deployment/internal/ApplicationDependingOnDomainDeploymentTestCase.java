/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.reset;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.DomainFileBuilder;
import org.mule.tck.util.CompilerUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ApplicationDependingOnDomainDeploymentTestCase extends AbstractDeploymentTestCase {

  private static File pluginForbiddenJavaEchoTestClassFile;
  private static File pluginForbiddenMuleContainerEchoTestClassFile;
  private static File pluginForbiddenMuleThirdPartyEchoTestClassFile;


  private final DomainFileBuilder emptyDomain100FileBuilder =
      new DomainFileBuilder("empty-domain").definedBy("empty-domain-config.xml").withVersion("1.0.0");
  private final DomainFileBuilder emptyDomain101FileBuilder =
      new DomainFileBuilder("empty-domain").definedBy("empty-domain-config.xml").withVersion("1.0.1");


  private final ApplicationFileBuilder appDependingOnDomain100FileBuilder = new ApplicationFileBuilder("dummy-domain100-app")
      .definedBy("empty-config.xml").dependingOn(emptyDomain100FileBuilder);
  private final ApplicationFileBuilder appDependingOnDomain101FileBuilder = new ApplicationFileBuilder("dummy-domain101-app")
      .definedBy("empty-config.xml").dependingOn(emptyDomain101FileBuilder);

  private final ApplicationFileBuilder appReferencingDomain101FileBuilder = new ApplicationFileBuilder("dummy-domain101-app-ref")
      .definedBy("empty-config.xml").dependingOn(emptyDomain100FileBuilder)
      .deployedWith("domain", "empty-domain-1.0.1-mule-domain");

  @BeforeClass
  public static void compileTestClasses() throws Exception {
    pluginForbiddenJavaEchoTestClassFile =
        new CompilerUtils.SingleClassCompiler().dependingOn(barUtilsForbiddenJavaJarFile)
            .compile(getResourceFile("/org/foo/echo/PluginForbiddenJavaEcho.java"));
    pluginForbiddenMuleContainerEchoTestClassFile =
        new CompilerUtils.SingleClassCompiler().dependingOn(barUtilsForbiddenMuleContainerJarFile)
            .compile(getResourceFile("/org/foo/echo/PluginForbiddenMuleContainerEcho.java"));
    pluginForbiddenMuleThirdPartyEchoTestClassFile =
        new CompilerUtils.SingleClassCompiler().dependingOn(barUtilsForbiddenMuleThirdPartyJarFile)
            .compile(getResourceFile("/org/foo/echo/PluginForbiddenMuleThirdPartyEcho.java"));
  }

  @Before
  public void resetListeners() {
    reset(applicationDeploymentListener);
    reset(domainDeploymentListener);
  }

  public ApplicationDependingOnDomainDeploymentTestCase(boolean parallelDeployment) {
    super(parallelDeployment);
  }

  @Test
  public void appDeploymentPointingToDomain() throws Exception {
    startDeployment();

    // Fails because domain is not found
    addExplodedAppFromBuilder(appDependingOnDomain100FileBuilder, appDependingOnDomain100FileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, appDependingOnDomain100FileBuilder.getId());

    // Add domain 1.0.0
    addExplodedDomainFromBuilder(emptyDomain100FileBuilder, emptyDomain100FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain100FileBuilder.getId());

    // Add app pointing to 1.0.0
    addExplodedAppFromBuilder(appDependingOnDomain100FileBuilder, appDependingOnDomain100FileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, appDependingOnDomain100FileBuilder.getId());
  }

  @Test
  public void appDeploymentPointingToCompatibleDomain() throws Exception {
    startDeployment();

    // Fails because domain is not found
    addExplodedAppFromBuilder(appDependingOnDomain100FileBuilder, appDependingOnDomain100FileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, appDependingOnDomain100FileBuilder.getId());
    reset(applicationDeploymentListener);

    // Add domain with version upgraded (1.0.1)
    addExplodedDomainFromBuilder(emptyDomain101FileBuilder, emptyDomain101FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain101FileBuilder.getId());
    Domain domainFound = deploymentService.findDomain(emptyDomain101FileBuilder.getId());
    assertThat(domainFound, is(notNullValue()));

    // Add app pointing to 1.0.0 or compatible
    addExplodedAppFromBuilder(appDependingOnDomain100FileBuilder, appDependingOnDomain100FileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, appDependingOnDomain100FileBuilder.getId());
  }

  @Test
  public void appDeploymentFailsIfMultipleCompatibleDomainsAreDeployed() throws Exception {
    startDeployment();

    // Deploy two compatible domains (1.0.0 and 1.0.1)
    addExplodedDomainFromBuilder(emptyDomain100FileBuilder, emptyDomain100FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain100FileBuilder.getId());
    addExplodedDomainFromBuilder(emptyDomain101FileBuilder, emptyDomain101FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain101FileBuilder.getId());

    // Following application depends on domain 1.0.0, and both domains 1.0.0 and 1.0.1 are deployed, so
    // the deployment fails because the domain reference is ambiguous
    addExplodedAppFromBuilder(appDependingOnDomain100FileBuilder, appDependingOnDomain100FileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, appDependingOnDomain100FileBuilder.getId());

    reset(applicationDeploymentListener);

    // This application depends on domain 1.0.1, which is not considered compatible with 1.0.1, so this deployment is ok
    addExplodedAppFromBuilder(appDependingOnDomain101FileBuilder, appDependingOnDomain101FileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, appDependingOnDomain101FileBuilder.getId());
  }

  @Test
  public void referenceDomainByName() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(appReferencingDomain101FileBuilder, appReferencingDomain101FileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, appReferencingDomain101FileBuilder.getId());

    addExplodedDomainFromBuilder(emptyDomain101FileBuilder, emptyDomain101FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain101FileBuilder.getId());

    reset(applicationDeploymentListener);

    addExplodedAppFromBuilder(appReferencingDomain101FileBuilder, appReferencingDomain101FileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, appReferencingDomain101FileBuilder.getId());
  }

  @Override
  protected void deployURI(URI uri, Properties deploymentProperties) throws IOException {
    deploymentService.deployDomain(uri, deploymentProperties);
  }

  @Override
  protected void redeployId(String id, Properties deploymentProperties) {
    if (deploymentProperties == null) {
      deploymentService.redeployDomain(id);
    } else {
      deploymentService.redeployDomain(id, deploymentProperties);
    }
  }
}
