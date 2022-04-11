/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.container.api.MuleFoldersUtil.getAppDataFolder;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorFactoryProvider.artifactDescriptorFactoryProvider;
import static org.mule.runtime.module.deployment.internal.processor.SerializedAstArtifactConfigurationProcessor.serializedAstWithFallbackArtifactConfigurationProcessor;
import static org.mule.runtime.module.license.api.LicenseValidatorProvider.discoverLicenseValidator;

import static org.mockito.Mockito.mock;

import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.core.internal.config.RuntimeLockFactoryUtil;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.artifact.DescriptorLoaderRepositoryFactory;
import org.mule.runtime.deployment.model.api.artifact.extension.ExtensionModelLoaderRepository;
import org.mule.runtime.deployment.model.api.builder.ApplicationClassLoaderBuilderFactory;
import org.mule.runtime.deployment.model.api.plugin.resolver.PluginDependenciesResolver;
import org.mule.runtime.module.artifact.activation.internal.classloader.DefaultArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.internal.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.api.descriptor.AbstractArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.application.ApplicationDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.application.TestApplicationWrapper;
import org.mule.runtime.module.deployment.impl.internal.artifact.DefaultArtifactDescriptorFactoryProvider;
import org.mule.runtime.module.deployment.impl.internal.artifact.DefaultClassLoaderManager;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainManager;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainRepository;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateClassLoaderBuilderFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Creates a {@link DefaultApplicationFactory} that returns {@link TestApplicationWrapper} instances in order to simulate errors
 * on application deployment phases.
 */
public class TestApplicationFactory extends DefaultApplicationFactory {

  private static List<Runnable> afterTasks = new ArrayList<>();

  private boolean failOnStopApplication;
  private boolean failOnDisposeApplication;

  private TestApplicationFactory(ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory,
                                 ApplicationDescriptorFactory applicationDescriptorFactory,
                                 DomainRepository domainRepository,
                                 ServiceRepository serviceRepository,
                                 ExtensionModelLoaderRepository extensionModelLoaderRepository,
                                 ClassLoaderRepository classLoaderRepository,
                                 PolicyTemplateClassLoaderBuilderFactory policyTemplateClassLoaderBuilderFactory,
                                 PluginDependenciesResolver pluginDependenciesResolver,
                                 ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader) {
    super(applicationClassLoaderBuilderFactory, applicationDescriptorFactory, domainRepository,
          serviceRepository, extensionModelLoaderRepository, classLoaderRepository, policyTemplateClassLoaderBuilderFactory,
          pluginDependenciesResolver, artifactPluginDescriptorLoader,
          discoverLicenseValidator(TestApplicationFactory.class.getClassLoader()),
          RuntimeLockFactoryUtil.getRuntimeLockFactory(),
          mock(MemoryManagementService.class),
          serializedAstWithFallbackArtifactConfigurationProcessor());
  }

  public static TestApplicationFactory createTestApplicationFactory(DomainManager domainManager,
                                                                    ServiceRepository serviceRepository,
                                                                    ExtensionModelLoaderRepository extensionModelLoaderRepository,
                                                                    ModuleRepository moduleRepository,
                                                                    DescriptorLoaderRepository descriptorLoaderRepository) {
    AbstractArtifactDescriptorFactory<MulePluginModel, ArtifactPluginDescriptor> artifactPluginDescriptorFactory =
        artifactDescriptorFactoryProvider()
            .createArtifactPluginDescriptorFactory(new DescriptorLoaderRepositoryFactory().createDescriptorLoaderRepository(),
                                                   ArtifactDescriptorValidatorBuilder.builder());
    ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader =
        new ArtifactPluginDescriptorLoader(artifactPluginDescriptorFactory);
    ApplicationDescriptorFactory applicationDescriptorFactory =
        new ApplicationDescriptorFactory(artifactPluginDescriptorLoader, descriptorLoaderRepository,
                                         ArtifactDescriptorValidatorBuilder.builder());
    final DefaultClassLoaderManager artifactClassLoaderManager = new DefaultClassLoaderManager();
    PluginDependenciesResolver pluginDependenciesResolver =
        new DefaultArtifactDescriptorFactoryProvider().createBundlePluginDependenciesResolver(artifactPluginDescriptorFactory);

    DefaultArtifactClassLoaderResolver artifactClassLoaderResolver = new DefaultArtifactClassLoaderResolver(moduleRepository,
                                                                                                            new DefaultNativeLibraryFinderFactory(name -> getAppDataFolder(name)));
    ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory =
        new ApplicationClassLoaderBuilderFactory(artifactClassLoaderResolver);

    return new TestApplicationFactory(applicationClassLoaderBuilderFactory, applicationDescriptorFactory,
                                      domainManager, serviceRepository,
                                      extensionModelLoaderRepository, artifactClassLoaderManager,
                                      mock(PolicyTemplateClassLoaderBuilderFactory.class), pluginDependenciesResolver,
                                      artifactPluginDescriptorLoader);
  }

  @Override
  public Application createArtifact(File appLocation, Optional<Properties> appProperties) throws IOException {
    Application app = super.createArtifact(appLocation, appProperties);

    TestApplicationWrapper testApplicationWrapper = new TestApplicationWrapper(app);
    testApplicationWrapper.setFailOnDisposeApplication(failOnDisposeApplication);
    testApplicationWrapper.setFailOnStopApplication(failOnStopApplication);

    if (failOnStopApplication) {
      afterTasks.add(() -> app.stop());
    }
    if (failOnDisposeApplication) {
      afterTasks.add(() -> app.dispose());
    }

    return testApplicationWrapper;
  }

  public void setFailOnDisposeApplication(boolean failOnDisposeApplication) {
    this.failOnDisposeApplication = failOnDisposeApplication;
  }

  public void setFailOnStopApplication(boolean failOnStopApplication) {
    this.failOnStopApplication = failOnStopApplication;
  }

  /**
   * Finish the dispose/stop process that was aborted due to an exception.
   */
  public static void after() {
    afterTasks.forEach(Runnable::run);
    afterTasks.clear();
  }
}
