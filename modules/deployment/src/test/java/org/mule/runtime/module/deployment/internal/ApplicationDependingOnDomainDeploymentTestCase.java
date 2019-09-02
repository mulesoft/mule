/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static org.mockito.Mockito.reset;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.DomainFileBuilder;

import org.junit.Test;

public class ApplicationDependingOnDomainDeploymentTestCase extends AbstractDeploymentTestCase {

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
  private final ApplicationFileBuilder appReferencingDomain100FileBuilder = new ApplicationFileBuilder("dummy-domain100-app-ref")
      .definedBy("empty-config.xml").dependingOn(emptyDomain100FileBuilder)
      .deployedWith("domain", "empty-domain-1.0.0-mule-domain");

  private final ApplicationFileBuilder incompatibleDomainNameAppFileBuilder = new ApplicationFileBuilder("bad-domain-app-ref")
      .definedBy("empty-config.xml").dependingOn(emptyDomain101FileBuilder)
      .deployedWith("domain", "empty-domain-1.0.0-mule-domain");

  private final ApplicationFileBuilder appWithDomainNameButMissingBundleDescriptor =
      new ApplicationFileBuilder("dummy-domain101-app-ref")
          .definedBy("empty-config.xml").deployedWith("domain", "empty-domain-1.0.1-mule-domain");

  public ApplicationDependingOnDomainDeploymentTestCase(boolean parallelDeployment) {
    super(parallelDeployment);
  }

  @Test
  public void domainNotFound() throws Exception {
    startDeployment();

    // By GAV
    addExplodedAppFromBuilder(appDependingOnDomain100FileBuilder, appDependingOnDomain100FileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, appDependingOnDomain100FileBuilder.getId());

    // By name
    addExplodedAppFromBuilder(appReferencingDomain101FileBuilder, appReferencingDomain101FileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, appReferencingDomain101FileBuilder.getId());
  }

  @Test
  public void referenceToDomainByGAV() throws Exception {
    startDeployment();

    // Add domain 1.0.0
    addExplodedDomainFromBuilder(emptyDomain100FileBuilder, emptyDomain100FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain100FileBuilder.getId());

    // Add app pointing to 1.0.0
    addExplodedAppFromBuilder(appDependingOnDomain100FileBuilder, appDependingOnDomain100FileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, appDependingOnDomain100FileBuilder.getId());
  }

  @Test
  public void referenceToCompatibleDomainByGAV() throws Exception {
    startDeployment();

    // Add domain with version upgraded (1.0.1)
    addExplodedDomainFromBuilder(emptyDomain101FileBuilder, emptyDomain101FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain101FileBuilder.getId());

    // Add app pointing to 1.0.0 or compatible
    addExplodedAppFromBuilder(appDependingOnDomain100FileBuilder, appDependingOnDomain100FileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, appDependingOnDomain100FileBuilder.getId());
  }

  @Test
  public void referenceDomainByName() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(emptyDomain101FileBuilder, emptyDomain101FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain101FileBuilder.getId());

    reset(applicationDeploymentListener);

    addExplodedAppFromBuilder(appReferencingDomain101FileBuilder, appReferencingDomain101FileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, appReferencingDomain101FileBuilder.getId());
  }

  @Test
  public void failsWhenSpecifiedNameIsNotFoundEvenWhenCompatibleIsPresent() throws Exception {
    startDeployment();

    // Add domain with version upgraded (1.0.1)
    addExplodedDomainFromBuilder(emptyDomain101FileBuilder, emptyDomain101FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain101FileBuilder.getId());

    // Add app pointing to 1.0.0 by name
    addExplodedAppFromBuilder(appReferencingDomain100FileBuilder, appReferencingDomain100FileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, appReferencingDomain100FileBuilder.getId());
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
  public void appPointingToIncompatibleDomain() throws Exception {
    startDeployment();

    // Deploy both versions to ensure that there is one compatible domain and a name-matching domain
    addExplodedDomainFromBuilder(emptyDomain100FileBuilder, emptyDomain100FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain100FileBuilder.getId());
    addExplodedDomainFromBuilder(emptyDomain101FileBuilder, emptyDomain101FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain101FileBuilder.getId());

    // The app depends on 1.0.1 but references the domain 1.0.0 by name, so it must fail
    addExplodedAppFromBuilder(incompatibleDomainNameAppFileBuilder, incompatibleDomainNameAppFileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, incompatibleDomainNameAppFileBuilder.getId());
  }

  @Test
  public void failToDeployAppWithDomainNameButMissingBundleDescriptor() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(emptyDomain101FileBuilder, emptyDomain101FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain101FileBuilder.getId());

    addExplodedAppFromBuilder(appWithDomainNameButMissingBundleDescriptor, appWithDomainNameButMissingBundleDescriptor.getId());
    assertDeploymentFailure(applicationDeploymentListener, appWithDomainNameButMissingBundleDescriptor.getId());
  }
}
