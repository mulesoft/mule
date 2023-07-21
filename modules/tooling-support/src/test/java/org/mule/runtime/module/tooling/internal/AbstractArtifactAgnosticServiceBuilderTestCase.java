/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.tooling.internal;

import static org.mule.runtime.core.internal.config.RuntimeLockFactoryUtil.getRuntimeLockFactory;
import static org.mule.runtime.module.license.api.LicenseValidatorProvider.discoverLicenseValidator;
import static org.mule.test.allure.AllureConstants.ToolingSupport.TOOLING_SUPPORT;
import static org.mule.test.allure.AllureConstants.ToolingSupport.ServiceBuilderStory.SERVICE_BUILDER;

import static java.util.Collections.singletonMap;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.builder.ApplicationClassLoaderBuilderFactory;
import org.mule.runtime.deployment.model.api.plugin.resolver.PluginDependenciesResolver;
import org.mule.runtime.module.artifact.activation.api.descriptor.DeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelLoaderRepository;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainRepository;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateClassLoaderBuilderFactory;
import org.mule.runtime.module.tooling.api.ArtifactAgnosticServiceBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.concurrent.atomic.AtomicReference;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(TOOLING_SUPPORT)
@Story(SERVICE_BUILDER)
@Issue("W-13057814")
public abstract class AbstractArtifactAgnosticServiceBuilderTestCase extends AbstractMuleTestCase {

  private final DefaultApplicationFactory applicationFactory =
      spy(new DefaultApplicationFactory(mock(ApplicationClassLoaderBuilderFactory.class),
                                        DeployableArtifactDescriptorFactory.defaultArtifactDescriptorFactory(),
                                        mock(DomainRepository.class), mock(ServiceRepository.class),
                                        mock(ExtensionModelLoaderRepository.class),
                                        mock(ClassLoaderRepository.class), mock(PolicyTemplateClassLoaderBuilderFactory.class),
                                        mock(PluginDependenciesResolver.class),
                                        discoverLicenseValidator(getClass().getClassLoader()),
                                        getRuntimeLockFactory(),
                                        mock(MemoryManagementService.class),
                                        mock(ArtifactConfigurationProcessor.class)));

  @Test
  public void applicationDescriptorCorrectlyCreated() throws Exception {
    AtomicReference<ApplicationDescriptor> applicationDescriptor = new AtomicReference<>();
    doAnswer(i -> {
      applicationDescriptor.set(i.getArgument(0));
      return mock(Application.class);
    }).when(applicationFactory).createArtifact(any());
    ArtifactAgnosticServiceBuilder builder = getArtifactAgnosticServiceBuilder(applicationFactory);
    ArtifactDeclaration artifactDeclaration = new ArtifactDeclaration();
    builder.setArtifactDeclaration(artifactDeclaration);
    final String testKey = "test key";
    final String testValue = "test value";
    builder.setArtifactProperties(singletonMap(testKey, testValue));

    try {
      ((AbstractArtifactAgnosticService) builder.build()).getStartedApplication();
    } catch (Throwable t) {
      // Not interested in this succeeding, just interested in the descriptor being created
    }

    assertThat(applicationDescriptor.get().getArtifactDeclaration(), is(artifactDeclaration));
    assertThat(applicationDescriptor.get().getAppProperties(), aMapWithSize(1));
    assertThat(applicationDescriptor.get().getAppProperties(), hasEntry(is(testKey), is(testValue)));
  }

  protected abstract ArtifactAgnosticServiceBuilder getArtifactAgnosticServiceBuilder(DefaultApplicationFactory applicationFactory);

}
