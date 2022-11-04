/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.domain;

import static org.mule.runtime.core.internal.config.RuntimeLockFactoryUtil.getRuntimeLockFactory;
import static org.mule.test.allure.AllureConstants.DeployableCreationFeature.DOMAIN_CREATION;

import static java.util.Optional.empty;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.builder.DomainClassLoaderBuilderFactory;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.internal.artifact.ServiceRegistryDescriptorLoaderRepository;
import org.mule.runtime.deployment.model.internal.artifact.extension.ExtensionModelLoaderManager;
import org.mule.runtime.module.artifact.activation.api.classloader.ArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.api.descriptor.DeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;
import org.mule.runtime.module.license.api.LicenseValidator;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import org.junit.Test;

@Feature(DOMAIN_CREATION)
@Issue("W-11086334")
public class DefaultLightweightDomainFactoryTestCase extends AbstractMuleTestCase {

  private final DefaultDomainFactory domainFactory =
      new DefaultDomainFactory(new DomainDescriptorFactory(mock(ArtifactPluginDescriptorLoader.class),
                                                           new ServiceRegistryDescriptorLoaderRepository(new SpiServiceRegistry()),
                                                           ArtifactDescriptorValidatorBuilder.builder()),
                               DeployableArtifactDescriptorFactory.defaultArtifactDescriptorFactory(),
                               new DefaultDomainManager(),
                               null,
                               mock(ServiceRepository.class),
                               new DomainClassLoaderBuilderFactory(ArtifactClassLoaderResolver.defaultClassLoaderResolver()),
                               mock(ExtensionModelLoaderManager.class),
                               mock(LicenseValidator.class),
                               getRuntimeLockFactory(),
                               mock(MemoryManagementService.class),
                               mock(ArtifactConfigurationProcessor.class));

  @Test
  public void lightweightDomain() throws Exception {
    String domainName = "no-dependencies";
    Domain domain = domainFactory.createArtifact(getDomainFolder("domains/" + domainName), empty());

    assertThat(domain.getDescriptor(), instanceOf(DomainDescriptor.class));
    assertThat(domain.getDescriptor().getName(), is(domainName));
    assertThat(domain.getArtifactPlugins().size(), is(0));
  }

  @Test
  public void lightweightDomainWithDependencies() throws Exception {
    String domainName = "multiple-dependencies";
    Domain domain = domainFactory.createArtifact(getDomainFolder("domains/" + domainName), empty());

    assertThat(domain.getDescriptor(), instanceOf(DomainDescriptor.class));
    assertThat(domain.getDescriptor().getName(), is(domainName));
    List<ArtifactPlugin> plugins = domain.getArtifactPlugins();
    assertThat(plugins.size(), is(2));
    assertThat(plugins, contains(
                                 hasProperty("artifactId", is("domain/" + domainName + "/plugin/Sockets")),
                                 hasProperty("artifactId", is("domain/" + domainName + "/plugin/HTTP"))));
  }

  protected File getDomainFolder(String path) throws URISyntaxException {
    return new File(getClass().getClassLoader().getResource(path).toURI());
  }

}
