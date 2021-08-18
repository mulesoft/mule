/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.reset;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.DEFAULT_DOMAIN_NAME;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.DOMAIN_DEPLOYMENT;

import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.DomainBundleFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.DomainFileBuilder;

import org.junit.Test;

import io.qameta.allure.Feature;

/**
 * Contains test for domain bundle deployment
 */
@Feature(DOMAIN_DEPLOYMENT)
public class DomainBundleDeploymentTestCase extends AbstractDeploymentTestCase {

  public DomainBundleDeploymentTestCase(boolean parallelDeployment) {
    super(parallelDeployment);
  }

  @Test
  public void deploysDomainBundle() throws Exception {
    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder(dummyAppDescriptorFileBuilder)
        .dependingOn(callbackExtensionPlugin).dependingOn(dummyDomainFileBuilder);
    DomainBundleFileBuilder domainBundleFileBuilder =
        new DomainBundleFileBuilder(dummyDomainFileBuilder).containing(applicationFileBuilder);

    addDomainBundleFromBuilder(domainBundleFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainBundleDeploymentListener, domainBundleFileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());

    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, dummyDomainFileBuilder.getId()}, true);
    assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);
  }

  @Test
  public void failsToDeployDomainBundleWithCorruptedDomain() throws Exception {
    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder(dummyAppDescriptorFileBuilder)
        .dependingOn(dummyDomainFileBuilder);
    DomainBundleFileBuilder domainBundleFileBuilder =
        new DomainBundleFileBuilder(new DomainFileBuilder("dummy-domain")).containing(applicationFileBuilder);

    addDomainBundleFromBuilder(domainBundleFileBuilder);

    startDeployment();

    assertDeploymentFailure(domainBundleDeploymentListener, domainBundleFileBuilder.getId());
    assertDeploymentFailure(domainDeploymentListener, dummyDomainFileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
  }

  @Test
  public void deploysDomainBundleWithCorruptedApp() throws Exception {
    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder(dummyAppDescriptorFileBuilder).corrupted();
    DomainBundleFileBuilder domainBundleFileBuilder =
        new DomainBundleFileBuilder(dummyDomainFileBuilder).containing(applicationFileBuilder);

    addDomainBundleFromBuilder(domainBundleFileBuilder);

    startDeployment();

    assertDeploymentFailure(domainBundleDeploymentListener, domainBundleFileBuilder.getId());
    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, dummyDomainFileBuilder.getId()}, true);

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
  }

  @Test
  public void redeploysDomainBundle() throws Exception {
    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder(dummyAppDescriptorFileBuilder)
        .dependingOn(dummyDomainFileBuilder).dependingOn(callbackExtensionPlugin);
    DomainBundleFileBuilder domainBundleFileBuilder =
        new DomainBundleFileBuilder(dummyDomainFileBuilder).containing(applicationFileBuilder);

    addDomainBundleFromBuilder(domainBundleFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainBundleDeploymentListener, domainBundleFileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());

    reset(domainDeploymentListener);
    reset(domainBundleDeploymentListener);
    reset(applicationDeploymentListener);

    addDomainBundleFromBuilder(domainBundleFileBuilder);

    assertDeploymentSuccess(domainBundleDeploymentListener, domainBundleFileBuilder.getId());
    assertDomainRedeploymentSuccess(dummyDomainFileBuilder.getId());
    assertApplicationRedeploymentSuccess(dummyAppDescriptorFileBuilder.getId());
  }

  @Test
  public void redeploysDomainBundleCausesUndeployOfRemovedApps() throws Exception {
    ApplicationFileBuilder applicationFileBuilder1 = new ApplicationFileBuilder(dummyAppDescriptorFileBuilder)
        .dependingOn(callbackExtensionPlugin).dependingOn(dummyDomainFileBuilder);
    ApplicationFileBuilder applicationFileBuilder2 = new ApplicationFileBuilder(emptyAppFileBuilder)
        .dependingOn(callbackExtensionPlugin).dependingOn(dummyDomainFileBuilder);

    DomainBundleFileBuilder domainBundleFileBuilder = new DomainBundleFileBuilder(dummyDomainFileBuilder)
        .containing(applicationFileBuilder1).containing(applicationFileBuilder2);

    addDomainBundleFromBuilder(domainBundleFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainBundleDeploymentListener, domainBundleFileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder1.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder2.getId());

    reset(domainDeploymentListener);
    reset(domainBundleDeploymentListener);
    reset(applicationDeploymentListener);

    domainBundleFileBuilder = new DomainBundleFileBuilder(dummyDomainFileBuilder).containing(applicationFileBuilder1);
    addDomainBundleFromBuilder(domainBundleFileBuilder);

    assertDeploymentSuccess(domainBundleDeploymentListener, domainBundleFileBuilder.getId());
    assertDomainRedeploymentSuccess(dummyDomainFileBuilder.getId());
    assertApplicationRedeploymentSuccess(applicationFileBuilder1.getId());
    assertApplicationMissingOnBundleRedeployment(applicationFileBuilder2.getId());
  }

  @Test
  public void redeploysDomainBundleWithBrokenDomain() throws Exception {
    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder(dummyAppDescriptorFileBuilder)
        .dependingOn(callbackExtensionPlugin).dependingOn(dummyDomainFileBuilder);
    DomainBundleFileBuilder domainBundleFileBuilder =
        new DomainBundleFileBuilder(dummyDomainFileBuilder).containing(applicationFileBuilder);

    addDomainBundleFromBuilder(domainBundleFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainBundleDeploymentListener, domainBundleFileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());

    reset(domainDeploymentListener);
    reset(domainBundleDeploymentListener);
    reset(applicationDeploymentListener);

    dummyDomainFileBuilder = new DomainFileBuilder(dummyDomainFileBuilder).corrupted();
    domainBundleFileBuilder = new DomainBundleFileBuilder(dummyDomainFileBuilder).containing(applicationFileBuilder);
    addDomainBundleFromBuilder(domainBundleFileBuilder);

    assertDeploymentFailure(domainBundleDeploymentListener, domainBundleFileBuilder.getId());
    assertDomainRedeploymentFailure(dummyDomainFileBuilder.getId());
    assertRedeploymentFailure(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertThat(deploymentService.findApplication(dummyAppDescriptorFileBuilder.getId()), is(nullValue()));
  }

  private void addDomainBundleFromBuilder(DomainBundleFileBuilder domainBundleFileBuilder) throws Exception {
    addPackedDomainFromBuilder(domainBundleFileBuilder, null);
  }
}
