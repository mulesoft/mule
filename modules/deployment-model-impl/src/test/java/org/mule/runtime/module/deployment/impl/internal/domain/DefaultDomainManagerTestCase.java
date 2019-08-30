/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.domain;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.internal.config.RuntimeComponentBuildingDefinitionsUtil.getRuntimeComponentBuildingDefinitionProvider;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.internal.application.MuleApplicationClassLoader;
import org.mule.runtime.deployment.model.internal.domain.AbstractDomainTestCase;
import org.mule.runtime.deployment.model.internal.domain.DomainClassLoaderBuilder;
import org.mule.runtime.deployment.model.internal.plugin.PluginDependenciesResolver;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderManager;
import org.mule.runtime.module.license.api.LicenseValidator;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DefaultDomainManagerTestCase extends AbstractDomainTestCase {

  public DefaultDomainManagerTestCase() throws IOException {
    super();
  }

  private final ServiceRepository serviceRepository = mock(ServiceRepository.class);
  private final DomainDescriptorFactory domainDescriptorFactory = mock(DomainDescriptorFactory.class);
  private final PluginDependenciesResolver pluginDependenciesResolver = mock(PluginDependenciesResolver.class);
  private final DomainClassLoaderBuilderFactory domainClassLoaderBuilderFactory = mock(DomainClassLoaderBuilderFactory.class);
  private final ExtensionModelLoaderManager extensionModelLoaderManager = mock(ExtensionModelLoaderManager.class);
  private final LicenseValidator licenseValidator = mock(LicenseValidator.class);
  private final DefaultDomainFactory domainFactory = new DefaultDomainFactory(domainDescriptorFactory,
                                                                              new DefaultDomainManager(),
                                                                              null,
                                                                              serviceRepository,
                                                                              pluginDependenciesResolver,
                                                                              domainClassLoaderBuilderFactory,
                                                                              extensionModelLoaderManager,
                                                                              licenseValidator,
                                                                              getRuntimeComponentBuildingDefinitionProvider());
  private DefaultDomainManager domainManager;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() {
    when(pluginDependenciesResolver.resolve(argThat(is(emptySet())), anyList(), anyBoolean())).thenReturn(emptyList());
    domainManager = new DefaultDomainManager();
  }

  private BundleDescriptor createBundleDescriptor(String domainName, String version) {
    BundleDescriptor.Builder builder = new BundleDescriptor.Builder().setGroupId("com.mulesoft").setArtifactId(domainName)
        .setClassifier("mule-domain").setType("jar").setVersion(version);
    return builder.build();
  }


  private Domain createDomain(String artifactId, String version) throws IOException {
    return createDomain(artifactId, version, artifactId + "-" + version + "-mule-domain");
  }

  private Domain createDomain(String artifactId, String version, String artifactName) throws IOException {
    final DomainDescriptor descriptor = new DomainDescriptor(artifactName);
    descriptor.setBundleDescriptor(createBundleDescriptor(artifactId, version));
    when(domainDescriptorFactory.create(any(), any())).thenReturn(descriptor);

    final MuleApplicationClassLoader domainArtifactClassLoader = mock(MuleApplicationClassLoader.class);
    when(domainArtifactClassLoader.getArtifactId()).thenReturn(artifactId);

    DomainClassLoaderBuilder domainClassLoaderBuilderMock = mock(DomainClassLoaderBuilder.class);
    when(domainClassLoaderBuilderMock.setArtifactDescriptor(any()))
        .thenReturn(domainClassLoaderBuilderMock);
    when(domainClassLoaderBuilderMock.setArtifactId(any())).thenReturn(domainClassLoaderBuilderMock);
    when(domainClassLoaderBuilderMock
        .addArtifactPluginDescriptors(descriptor.getPlugins().toArray(new ArtifactPluginDescriptor[0])))
            .thenReturn(domainClassLoaderBuilderMock);
    when(domainClassLoaderBuilderMock.build()).thenReturn(domainArtifactClassLoader);
    when(domainClassLoaderBuilderFactory.createArtifactClassLoaderBuilder()).thenReturn(domainClassLoaderBuilderMock);

    Domain domain = domainFactory.createArtifact(new File(artifactName), empty());
    assertThat(domain.getArtifactName(), is(artifactName));
    assertThat(domain.getDescriptor(), is(descriptor));
    assertThat(domain.getArtifactClassLoader(), is(domainArtifactClassLoader));
    return domain;
  }

  @Test
  public void simpleAdditionsAndDeletions() throws IOException, DomainNotFoundException {
    String domainName1 = "custom-domain-one";
    String domainName2 = "custom-domain-two";
    String version = "1.1.0";
    Domain domain1 = createDomain(domainName1, version);
    Domain domain2 = createDomain(domainName2, version);

    BundleDescriptor descriptor1 = domain1.getDescriptor().getBundleDescriptor();
    BundleDescriptor descriptor2 = domain2.getDescriptor().getBundleDescriptor();
    String domainCompleteName1 = descriptor1.getArtifactFileName();
    String domainCompleteName2 = descriptor2.getArtifactFileName();
    assertThat(domainCompleteName1, is(domainName1 + "-" + version + "-mule-domain"));
    assertThat(domainCompleteName2, is(domainName2 + "-" + version + "-mule-domain"));

    domainManager.addDomain(domain1);
    assertThat(domainManager.contains(domainCompleteName1), is(true));
    assertThat(domainManager.getDomain(domainCompleteName1), is(domain1));
    assertThat(domainManager.contains(domainCompleteName2), is(false));

    domainManager.addDomain(domain2);
    assertThat(domainManager.getDomain(domainCompleteName1), is(domain1));
    assertThat(domainManager.getDomain(domainCompleteName2), is(domain2));

    domainManager.removeDomain(domain1);
    assertThat(domainManager.contains(domainCompleteName1), is(false));
    assertThat(domainManager.getDomain(domainCompleteName2), is(domain2));

    domainManager.removeDomain(domain2);
    assertThat(domainManager.contains(domainCompleteName1), is(false));
    assertThat(domainManager.contains(domainCompleteName2), is(false));
  }

  @Test
  public void domainNotFound() throws IOException, DomainNotFoundException {
    Domain domainAdded = createDomain("domain-added", "1.0.0");
    String domainAddedName = domainAdded.getDescriptor().getName();

    Domain domainNotAdded = createDomain("domain-not-added", "1.0.0");
    String domainNotAddedName = domainNotAdded.getDescriptor().getName();

    domainManager.addDomain(domainAdded);
    assertThat(domainManager.contains(domainAddedName), is(true));
    assertThat(domainManager.getDomain(domainAddedName), is(domainAdded));

    assertThat(domainManager.contains(domainNotAddedName), is(false));
    expectedException.expect(DomainNotFoundException.class);
    domainManager.getDomain(domainNotAddedName);
  }

  @Test
  public void addTwoIdenticalDomainsWithDifferentNames() throws IOException {
    String domainId = "custom-domain";
    String version = "1.1.0";
    String domainName1 = domainId + "-one-" + version + "-mule-domain";
    String domainName2 = domainId + "-two-" + version + "-mule-domain";
    Domain domain1 = createDomain(domainId, version, domainName1);
    Domain domain2 = createDomain(domainId, version, domainName2);

    domainManager.addDomain(domain1);
    domainManager.addDomain(domain2);
  }

  @Test
  public void getIdenticalDomainsByName() throws IOException, DomainNotFoundException {
    String domainId = "custom-domain";
    String version = "1.1.0";
    String domainName1 = domainId + "-one-" + version + "-mule-domain";
    String domainName2 = domainId + "-two-" + version + "-mule-domain";
    Domain domain1 = createDomain(domainId, version, domainName1);
    Domain domain2 = createDomain(domainId, version, domainName2);

    domainManager.addDomain(domain1);
    domainManager.addDomain(domain2);

    // It is supported to get them by name
    assertThat(domainManager.getDomain(domainName1), is(domain1));
    assertThat(domainManager.getDomain(domainName2), is(domain2));
  }

  @Test
  public void getIdenticalDomainsByBundleDescriptorThrowsException()
      throws IOException, DomainNotFoundException, AmbiguousDomainReferenceException {
    String domainId = "custom-domain";
    String version = "1.1.0";
    String domainName1 = domainId + "-one-" + version + "-mule-domain";
    String domainName2 = domainId + "-two-" + version + "-mule-domain";
    Domain domain1 = createDomain(domainId, version, domainName1);
    Domain domain2 = createDomain(domainId, version, domainName2);

    domainManager.addDomain(domain1);
    domainManager.addDomain(domain2);

    expectedException.expect(AmbiguousDomainReferenceException.class);
    domainManager.getCompatibleDomain(domain1.getDescriptor().getBundleDescriptor());
    domainManager.getCompatibleDomain(domain2.getDescriptor().getBundleDescriptor());
  }

  @Test
  public void cannotAddTwoDomainsWithTheSameArtifactName() throws IOException {
    String domainName = "custom-domain";
    String version = "1.1.0";

    // First addition is ok
    domainManager.addDomain(createDomain("one-id", version, domainName));

    // Second is not
    expectedException.expect(IllegalArgumentException.class);
    domainManager.addDomain(createDomain("another-id", version, domainName));
  }

  @Test
  public void invalidDeletionDoesNothing() throws IOException {
    Domain domain = createDomain("custom-domain", "1.1.0");
    domainManager.removeDomain(domain);

    domainManager.addDomain(domain);
    domainManager.removeDomain(domain);
    domainManager.removeDomain(domain);
  }

  @Test
  public void applicationWorksIfTheDomainHasAHigherMinor()
      throws IOException, DomainNotFoundException, AmbiguousDomainReferenceException {
    // The app references to the old domain
    BundleDescriptor oldBundleDescriptor = createBundleDescriptor("custom-domain", "1.1.0");

    // Upgrade the domain minor
    Domain upgradedDomain = createDomain("custom-domain", "1.2.0");
    domainManager.addDomain(upgradedDomain);

    // Retrieve the domain using the descriptor that we have
    assertThat(domainManager.getCompatibleDomain(oldBundleDescriptor), is(upgradedDomain));
  }

  @Test
  public void applicationWorksIfTheDomainHasAHigherPatch()
      throws IOException, DomainNotFoundException, AmbiguousDomainReferenceException {
    // The app references to the old domain
    BundleDescriptor oldBundleDescriptor = createBundleDescriptor("custom-domain", "1.1.0");

    // Upgrade the domain minor
    Domain upgradedDomain = createDomain("custom-domain", "1.1.1");
    domainManager.addDomain(upgradedDomain);

    // Retrieve the domain using the descriptor that we have
    assertThat(domainManager.getCompatibleDomain(oldBundleDescriptor), is(upgradedDomain));
  }

  @Test
  public void applicationDoesNotWorkIfTheDomainHasAHigherMajor()
      throws IOException, DomainNotFoundException, AmbiguousDomainReferenceException {
    // The app references to the old domain
    BundleDescriptor oldBundleDescriptor = createBundleDescriptor("custom-domain", "1.1.0");

    // Upgrade the domain major
    Domain upgradedDomain = createDomain("custom-domain", "2.1.0");
    domainManager.addDomain(upgradedDomain);

    // Cannot retrieve the domain using the descriptor that we have
    expectedException.expect(DomainNotFoundException.class);
    domainManager.getCompatibleDomain(oldBundleDescriptor);
  }

  @Test
  public void applicationDoesNotWorkIfTheDomainHasALowerMajor()
      throws IOException, DomainNotFoundException, AmbiguousDomainReferenceException {
    // The app references to a domain
    BundleDescriptor bundleDescriptor = createBundleDescriptor("custom-domain", "2.1.0");

    // Downgrade the domain major
    Domain downgradedDomain = createDomain("custom-domain", "1.1.0");
    domainManager.addDomain(downgradedDomain);

    // Cannot retrieve the domain using the descriptor that we have
    expectedException.expect(DomainNotFoundException.class);
    domainManager.getCompatibleDomain(bundleDescriptor);
  }

  @Test
  public void applicationDoesNotWorkIfTheDomainHasALowerMinor()
      throws IOException, DomainNotFoundException, AmbiguousDomainReferenceException {
    // The app references to a domain
    BundleDescriptor bundleDescriptor = createBundleDescriptor("custom-domain", "1.2.0");

    // Downgrade the domain major
    Domain downgradedDomain = createDomain("custom-domain", "1.1.0");
    domainManager.addDomain(downgradedDomain);

    // Cannot retrieve the domain using the descriptor that we have
    expectedException.expect(DomainNotFoundException.class);
    domainManager.getCompatibleDomain(bundleDescriptor);
  }

  @Test
  public void applicationDoesNotWorkIfTheDomainHasALowerPatch()
      throws IOException, DomainNotFoundException, AmbiguousDomainReferenceException {
    // The app references to a domain
    BundleDescriptor bundleDescriptor = createBundleDescriptor("custom-domain", "1.1.1");

    // Downgrade the domain major
    Domain downgradedDomain = createDomain("custom-domain", "1.1.0");
    domainManager.addDomain(downgradedDomain);

    // Cannot retrieve the domain using the descriptor that we have
    expectedException.expect(DomainNotFoundException.class);
    domainManager.getCompatibleDomain(bundleDescriptor);
  }                                                                                                                 

}
