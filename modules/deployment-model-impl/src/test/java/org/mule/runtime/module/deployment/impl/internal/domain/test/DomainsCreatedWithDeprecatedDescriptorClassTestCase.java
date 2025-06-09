/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.domain.test;

import static org.mule.test.allure.AllureConstants.DeployableCreationFeature.DOMAIN_CREATION;

import static java.util.Optional.empty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import static org.mockito.Mockito.mock;

import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.builder.DomainClassLoaderBuilderFactory;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.module.artifact.activation.api.classloader.ArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.api.descriptor.DeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelLoaderRepository;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.internal.util.ServiceRegistryDescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainManager;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;
import org.mule.runtime.module.license.api.LicenseValidator;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.net.URISyntaxException;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;

@Feature(DOMAIN_CREATION)
@Issue("W-11911617")
public class DomainsCreatedWithDeprecatedDescriptorClassTestCase extends AbstractMuleTestCase {

  private final DefaultDomainFactory domainFactory =
      new DefaultDomainFactory(new DomainDescriptorFactory(mock(ArtifactPluginDescriptorLoader.class),
                                                           new ServiceRegistryDescriptorLoaderRepository(),
                                                           ArtifactDescriptorValidatorBuilder.builder()),
                               DeployableArtifactDescriptorFactory.defaultArtifactDescriptorFactory(),
                               new DefaultDomainManager(),
                               null,
                               mock(ServiceRepository.class),
                               new DomainClassLoaderBuilderFactory(ArtifactClassLoaderResolver.defaultClassLoaderResolver()),
                               mock(ExtensionModelLoaderRepository.class),
                               mock(LicenseValidator.class),
                               mock(MemoryManagementService.class),
                               mock(ArtifactConfigurationProcessor.class));

  @Test
  public void heavyweightDomainCreatedWithDeprecatedDescriptorClass() throws Exception {
    Domain domain = domainFactory.createArtifact(getDomainFolder("domains/no-dependencies-heavyweight"), empty());

    assertThat(domain.getDescriptor(), instanceOf(DomainDescriptor.class));
  }

  @Test
  public void lightweightDomainCreatedWithDeprecatedDescriptorClass() throws Exception {
    Domain domain = domainFactory.createArtifact(getDomainFolder("domains/no-dependencies"), empty());

    assertThat(domain.getDescriptor(), instanceOf(DomainDescriptor.class));
  }

  protected File getDomainFolder(String path) throws URISyntaxException {
    return new File(getClass().getClassLoader().getResource(path).toURI());
  }

}
