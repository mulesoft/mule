/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.DEFAULT_DOMAIN_NAME;
import static org.mule.test.allure.AllureConstants.DeployableCreationFeature.APP_CREATION;

import static java.util.Optional.empty;

import static org.hamcrest.Matchers.instanceOf;

import static org.junit.Assert.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.builder.ApplicationClassLoaderBuilder;
import org.mule.runtime.deployment.model.api.builder.ApplicationClassLoaderBuilderFactory;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.deployment.model.api.plugin.resolver.PluginDependenciesResolver;
import org.mule.runtime.module.artifact.activation.api.descriptor.DeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelLoaderRepository;
import org.mule.runtime.module.artifact.activation.internal.classloader.MuleApplicationClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainManager;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainManager;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateClassLoaderBuilderFactory;
import org.mule.runtime.module.license.api.LicenseValidator;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.net.URISyntaxException;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;

@Feature(APP_CREATION)
@Issue("W-11911617")
public class ApplicationsCreatedWithDeprecatedDescriptorClassTestCase extends AbstractMuleTestCase {

  private final ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory =
      mock(ApplicationClassLoaderBuilderFactory.class);
  private final DomainManager domainManager = new DefaultDomainManager();
  private final DefaultApplicationFactory applicationFactory =
      new DefaultApplicationFactory(applicationClassLoaderBuilderFactory,
                                    DeployableArtifactDescriptorFactory.defaultArtifactDescriptorFactory(),
                                    domainManager,
                                    mock(ServiceRepository.class),
                                    mock(ExtensionModelLoaderRepository.class),
                                    mock(ClassLoaderRepository.class),
                                    mock(PolicyTemplateClassLoaderBuilderFactory.class),
                                    mock(PluginDependenciesResolver.class),
                                    mock(LicenseValidator.class),
                                    mock(MemoryManagementService.class),
                                    mock(ArtifactConfigurationProcessor.class));

  public ApplicationsCreatedWithDeprecatedDescriptorClassTestCase() {
    ApplicationClassLoaderBuilder applicationClassLoaderBuilderMock = mock(ApplicationClassLoaderBuilder.class);
    when(applicationClassLoaderBuilderMock.setDomainParentClassLoader(any())).thenReturn(applicationClassLoaderBuilderMock);
    when(applicationClassLoaderBuilderMock.setArtifactDescriptor(any()))
        .thenReturn(applicationClassLoaderBuilderMock);
    when(applicationClassLoaderBuilderMock.build()).thenReturn(mock(MuleApplicationClassLoader.class));
    when(applicationClassLoaderBuilderFactory.createArtifactClassLoaderBuilder())
        .thenReturn(applicationClassLoaderBuilderMock);

    createDefaultDomain();
  }

  private void createDefaultDomain() {
    final Domain defaultDomain = mock(Domain.class);

    final ArtifactClassLoader domainArtifactClassLoader = mock(ArtifactClassLoader.class);
    when(domainArtifactClassLoader.getClassLoader()).thenReturn(mock(ClassLoader.class));
    when(defaultDomain.getArtifactClassLoader()).thenReturn(domainArtifactClassLoader);
    when(defaultDomain.getDescriptor()).thenReturn(new DomainDescriptor(DEFAULT_DOMAIN_NAME));

    domainManager.addDomain(defaultDomain);
  }

  @Test
  public void heavyweightApplicationCreatedWithDeprecatedDescriptorClass() throws Exception {
    Application application =
        applicationFactory.createArtifact(getApplicationFolder("apps/no-dependencies-heavyweight"), empty());

    assertThat(application.getDescriptor(), instanceOf(ApplicationDescriptor.class));
  }

  @Test
  public void lightweightApplicationCreatedWithDeprecatedDescriptorClass() throws Exception {
    Application application = applicationFactory.createArtifact(getApplicationFolder("apps/no-dependencies"), empty());

    assertThat(application.getDescriptor(), instanceOf(ApplicationDescriptor.class));
  }

  protected File getApplicationFolder(String path) throws URISyntaxException {
    return new File(getClass().getClassLoader().getResource(path).toURI());
  }

}
