/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.reset;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.DEFAULT_DOMAIN_NAME;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.DomainFileBuilder;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Contains test for deployment of artifacts with patches
 */
public class PatchDeploymentTestCase extends AbstractDeploymentTestCase {

  public PatchDeploymentTestCase(boolean parallelDeployment) {
    super(parallelDeployment);
  }

  @Test
  public void sameApplicationMinorVersionWithGreaterBugFixVersionCausesRedeploy() throws Exception {
    testApplicationWithSameMinorVersionCausesRedeploy("1.0.0", "1.0.1");
  }

  @Test
  public void sameApplicationMinorVersionWithLowerBugFixVersionCausesRedeploy() throws Exception {
    testApplicationWithSameMinorVersionCausesRedeploy("1.0.1", "1.0.0");
  }

  @Test
  public void sameAppMajorVersionButDifferentMinorVersion() throws Exception {
    ApplicationFileBuilder deployedApp = new ApplicationFileBuilder("app")
        .containingClass(echoTestClassFile, "org/foo/EchoTest.class")
        .definedBy("dummy-app-config.xml").withVersion("1.0.0");

    addPackedAppFromBuilder(deployedApp);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, deployedApp.getId());

    reset(applicationDeploymentListener);

    ApplicationFileBuilder newMinorVersionApp = new ApplicationFileBuilder("app")
        .containingClass(echoTestClassFile, "org/foo/EchoTest.class")
        .definedBy("dummy-app-config.xml").withVersion("1.1.0");

    addPackedAppFromBuilder(newMinorVersionApp);
    assertApplicationDeploymentSuccess(applicationDeploymentListener, newMinorVersionApp.getId());
    assertThat(deploymentService.getApplications(), hasSize(2));
    assertAppsDir(NONE, new String[] {deployedApp.getId(), newMinorVersionApp.getId()}, true);
  }

  private void testApplicationWithSameMinorVersionCausesRedeploy(String deployedVersion, String patchedVersion) throws Exception {
    ApplicationFileBuilder deployedApp = new ApplicationFileBuilder("app")
        .containingClass(echoTestClassFile, "org/foo/EchoTest.class")
        .definedBy("dummy-app-config.xml").withVersion(deployedVersion);

    addPackedAppFromBuilder(deployedApp);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, deployedApp.getId());

    reset(applicationDeploymentListener);

    ApplicationFileBuilder patchedApp = new ApplicationFileBuilder("app")
        .containingClass(echoTestClassFile, "org/foo/EchoTest.class")
        .definedBy("dummy-app-config.xml").withVersion(patchedVersion);

    addPackedAppFromBuilder(patchedApp);
    assertUndeploymentSuccess(applicationDeploymentListener, deployedApp.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, patchedApp.getId());
    assertThat(deploymentService.getApplications(), hasSize(1));
    assertAppsDir(NONE, new String[] {patchedApp.getId()}, true);
  }

  @Test
  public void sameDomainMinorVersionWithGreaterBugFixVersionCausesRedeploy() throws Exception {
    testDomainWithSameMinorVersionCausesRedeploy("2.4.2", "2.4.7");
  }

  @Test
  public void sameDomainMinorVersionWithLowerBugFixVersionCausesRedeploy() throws Exception {
    testDomainWithSameMinorVersionCausesRedeploy("2.4.5", "2.4.2");
  }

  @Test
  public void sameDomainMajorVersionButDifferentMinorVersion() throws Exception {
    DomainFileBuilder deployedDomain = new DomainFileBuilder("domain")
        .definedBy("empty-domain-config.xml").withVersion("2.0.0");

    addPackedDomainFromBuilder(deployedDomain);

    startDeployment();
    assertDeploymentSuccess(domainDeploymentListener, deployedDomain.getId());

    reset(domainDeploymentListener);

    DomainFileBuilder newMinorVersionDomain = new DomainFileBuilder("domain")
        .definedBy("empty-domain-config.xml").withVersion("2.4.4");

    addPackedDomainFromBuilder(newMinorVersionDomain);
    assertDeploymentSuccess(domainDeploymentListener, newMinorVersionDomain.getId());
    assertThat(deploymentService.getDomains(), hasSize(3));
    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, deployedDomain.getId(), newMinorVersionDomain.getId()}, true);
  }

  private void testDomainWithSameMinorVersionCausesRedeploy(String deployedVersion, String patchedVersion) throws Exception {
    DomainFileBuilder deployedDomain = new DomainFileBuilder("domain")
        .definedBy("empty-domain-config.xml").withVersion(deployedVersion);

    addPackedDomainFromBuilder(deployedDomain);

    startDeployment();
    assertDeploymentSuccess(domainDeploymentListener, deployedDomain.getId());

    reset(domainDeploymentListener);

    DomainFileBuilder patchedDomain = new DomainFileBuilder("domain")
        .definedBy("empty-domain-config.xml").withVersion(patchedVersion);

    addPackedDomainFromBuilder(patchedDomain);
    assertUndeploymentSuccess(domainDeploymentListener, deployedDomain.getId());
    assertDeploymentSuccess(domainDeploymentListener, patchedDomain.getId());
    assertThat(deploymentService.getDomains(), hasSize(2));
    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, patchedDomain.getId()}, true);
  }
}
