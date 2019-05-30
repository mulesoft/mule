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
  public void setUp() throws Exception {
    when(pluginDependenciesResolver.resolve(argThat(is(emptySet())), anyList(), anyBoolean())).thenReturn(emptyList());
    domainManager = new DefaultDomainManager();
  }

  private BundleDescriptor createBundleDescriptor(String domainName, String version) {
    BundleDescriptor.Builder builder = new BundleDescriptor.Builder().setGroupId("com.mulesoft").setArtifactId(domainName)
        .setClassifier("mule-domain").setType("jar").setVersion(version);
    return builder.build();
  }

  private Domain createDomain(String artifactId, String version) throws IOException {
    String artifactName = artifactId + "-" + version + "-mule-domain";
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

  public DefaultDomainManagerTestCase() throws IOException {
    super();
  }

  @Test
  public void additionsAndDeletions() throws IOException, DomainNotFoundException, IncompatibleDomainVersionException {
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
    assertThat(domainManager.contains(descriptor1), is(true));
    assertThat(domainManager.contains(domainName1), is(true));
    assertThat(domainManager.contains(domainCompleteName1), is(true));
    assertThat(domainManager.getDomain(descriptor1), is(domain1));
    assertThat(domainManager.getDomain(domainName1), is(domain1));
    assertThat(domainManager.getDomain(domainCompleteName1), is(domain1));
    assertThat(domainManager.contains(descriptor2), is(false));
    assertThat(domainManager.contains(domainName2), is(false));
    assertThat(domainManager.contains(domainCompleteName2), is(false));

    domainManager.addDomain(domain2);
    assertThat(domainManager.getDomain(descriptor1), is(domain1));
    assertThat(domainManager.getDomain(descriptor2), is(domain2));
    assertThat(domainManager.getDomain(domainCompleteName1), is(domain1));
    assertThat(domainManager.getDomain(domainCompleteName2), is(domain2));

    domainManager.removeDomain(domain1);
    assertThat(domainManager.contains(descriptor1), is(false));
    assertThat(domainManager.contains(domainName1), is(false));
    assertThat(domainManager.contains(domainCompleteName1), is(false));
    assertThat(domainManager.getDomain(descriptor2), is(domain2));
    assertThat(domainManager.getDomain(domainCompleteName2), is(domain2));

    domainManager.removeDomain(domain2);
    assertThat(domainManager.contains(descriptor1), is(false));
    assertThat(domainManager.contains(domainName1), is(false));
    assertThat(domainManager.contains(domainCompleteName1), is(false));
    assertThat(domainManager.contains(descriptor2), is(false));
    assertThat(domainManager.contains(domainName2), is(false));
    assertThat(domainManager.contains(domainCompleteName2), is(false));
  }

  @Test
  public void invalidAdditionThrowsException() throws IOException {
    Domain domain = createDomain("custom-domain", "1.1.0");
    // first addition is ok
    domainManager.addDomain(domain);

    // second is not
    expectedException.expect(IllegalArgumentException.class);
    expectedException
        .expectMessage("Trying to add domain 'custom-domain-1.1.0-mule-domain', but a domain named 'custom-domain-1.1.0-mule-domain' was found");
    domainManager.addDomain(domain);
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
      throws IOException, IncompatibleDomainVersionException, DomainNotFoundException {
    // the app references to the old domain
    BundleDescriptor oldBundleDescriptor = createBundleDescriptor("custom-domain", "1.1.0");

    // we upgrade the domain minor
    Domain upgradedDomain = createDomain("custom-domain", "1.2.0");
    domainManager.addDomain(upgradedDomain);

    // we retrieve the domain using the descriptor that we have
    assertThat(domainManager.getDomain(oldBundleDescriptor), is(upgradedDomain));
  }

  @Test
  public void applicationWorksIfTheDomainHasAHigherPatch()
      throws IOException, IncompatibleDomainVersionException, DomainNotFoundException {
    // the app references to the old domain
    BundleDescriptor oldBundleDescriptor = createBundleDescriptor("custom-domain", "1.1.0");

    // we upgrade the domain minor
    Domain upgradedDomain = createDomain("custom-domain", "1.1.1");
    domainManager.addDomain(upgradedDomain);

    // we retrieve the domain using the descriptor that we have
    assertThat(domainManager.getDomain(oldBundleDescriptor), is(upgradedDomain));
  }

  @Test
  public void applicationDoesNotWorkIfTheDomainHasAHigherMajor()
      throws IOException, IncompatibleDomainVersionException, DomainNotFoundException {
    // the app references to the old domain
    BundleDescriptor oldBundleDescriptor = createBundleDescriptor("custom-domain", "1.1.0");

    // we upgrade the domain major
    Domain upgradedDomain = createDomain("custom-domain", "2.1.0");
    domainManager.addDomain(upgradedDomain);

    // we cannot retrieve the domain using the descriptor that we have
    expectedException.expect(DomainNotFoundException.class);
    expectedException.expectMessage("The domain 'custom-domain-1.1.0-mule-domain' was not found");
    domainManager.getDomain(oldBundleDescriptor);
  }

  @Test
  public void applicationDoesNotWorkIfTheDomainHasALowerMajor()
      throws IOException, IncompatibleDomainVersionException, DomainNotFoundException {
    // the app references to a domain
    BundleDescriptor bundleDescriptor = createBundleDescriptor("custom-domain", "2.1.0");

    // we downgrade the domain major
    Domain downgradedDomain = createDomain("custom-domain", "1.1.0");
    domainManager.addDomain(downgradedDomain);

    // we cannot retrieve the domain using the descriptor that we have
    expectedException.expect(DomainNotFoundException.class);
    expectedException.expectMessage("The domain 'custom-domain-2.1.0-mule-domain' was not found");
    domainManager.getDomain(bundleDescriptor);
  }

  @Test
  public void applicationDoesNotWorkIfTheDomainHasALowerMinor()
      throws IOException, IncompatibleDomainVersionException, DomainNotFoundException {
    // the app references to a domain
    BundleDescriptor bundleDescriptor = createBundleDescriptor("custom-domain", "1.2.0");

    // we downgrade the domain major
    Domain downgradedDomain = createDomain("custom-domain", "1.1.0");
    domainManager.addDomain(downgradedDomain);

    // we cannot retrieve the domain using the descriptor that we have
    expectedException.expect(IncompatibleDomainVersionException.class);
    expectedException
        .expectMessage("Expected domain 'custom-domain-1.2.0-mule-domain' couldn't be retrieved. It is available the '1.1.0' version");
    domainManager.getDomain(bundleDescriptor);
  }

  @Test
  public void applicationDoesNotWorkIfTheDomainHasALowerPatch()
      throws IOException, IncompatibleDomainVersionException, DomainNotFoundException {
    // the app references to a domain
    BundleDescriptor bundleDescriptor = createBundleDescriptor("custom-domain", "1.1.1");

    // we downgrade the domain major
    Domain downgradedDomain = createDomain("custom-domain", "1.1.0");
    domainManager.addDomain(downgradedDomain);

    // we cannot retrieve the domain using the descriptor that we have
    expectedException.expect(IncompatibleDomainVersionException.class);
    expectedException
        .expectMessage("Expected domain 'custom-domain-1.1.1-mule-domain' couldn't be retrieved. It is available the '1.1.0' version");
    domainManager.getDomain(bundleDescriptor);
  }

  @Test
  public void cannotAddDomainWithSameArtifactId() throws IOException {
    domainManager.addDomain(createDomain("custom-domain", "1.1.0"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException
        .expectMessage("Trying to add domain 'custom-domain-1.1.1-mule-domain', but a domain named 'custom-domain-1.1.0-mule-domain' was found");
    domainManager.addDomain(createDomain("custom-domain", "1.1.1"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException
        .expectMessage("Trying to add domain 'custom-domain-1.2.0-mule-domain', but a domain named 'custom-domain-1.1.0-mule-domain' was found");
    domainManager.addDomain(createDomain("custom-domain", "1.2.0"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException
        .expectMessage("Trying to add domain 'custom-domain-2.1.0-mule-domain', but a domain named 'custom-domain-1.1.0-mule-domain' was found");
    domainManager.addDomain(createDomain("custom-domain", "2.1.0"));
  }

  @Test
  public void domainUpgradeShouldBeDoneByRemovingThePreviousOne() throws IOException {
    Domain domain = createDomain("custom-domain", "1.1.0");
    domainManager.addDomain(domain);
    assertThat(domainManager.contains("custom-domain"), is(true));
    assertThat(domainManager.contains("custom-domain-1.1.0-mule-domain"), is(true));
    assertThat(domainManager.contains("custom-domain-1.2.0-mule-domain"), is(false));

    domain.dispose();
    domainManager.removeDomain(domain);
    assertThat(domainManager.contains("custom-domain"), is(false));
    assertThat(domainManager.contains("custom-domain-1.1.0-mule-domain"), is(false));
    assertThat(domainManager.contains("custom-domain-1.2.0-mule-domain"), is(false));

    domainManager.addDomain(createDomain("custom-domain", "1.2.0"));
    assertThat(domainManager.contains("custom-domain"), is(true));
    assertThat(domainManager.contains("custom-domain-1.1.0-mule-domain"), is(false));
    assertThat(domainManager.contains("custom-domain-1.2.0-mule-domain"), is(true));
  }
}
