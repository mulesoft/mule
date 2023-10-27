/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import org.apache.commons.lang.NotImplementedException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.builder.ApplicationClassLoaderBuilder;
import org.mule.runtime.deployment.model.api.builder.ApplicationClassLoaderBuilderFactory;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.plugin.resolver.PluginDependenciesResolver;
import org.mule.runtime.internal.memory.management.ArtifactMemoryManagementService;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.api.descriptor.DeployableArtifactDescriptorCreator;
import org.mule.runtime.module.artifact.activation.api.descriptor.DeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.activation.api.descriptor.DomainDescriptorResolutionException;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelLoaderRepository;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.deployment.impl.internal.artifact.AbstractDeployableArtifactFactory;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.AmbiguousDomainReferenceException;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainNotFoundException;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainRepository;
import org.mule.runtime.module.deployment.impl.internal.plugin.DefaultArtifactPlugin;
import org.mule.runtime.module.deployment.impl.internal.policy.DefaultPolicyInstanceProviderFactory;
import org.mule.runtime.module.deployment.impl.internal.policy.DefaultPolicyTemplateFactory;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateClassLoaderBuilderFactory;
import org.mule.runtime.module.license.api.LicenseValidator;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.google.common.collect.Maps.fromProperties;
import static java.lang.String.format;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.internal.util.MuleContainerUtils.getMuleAppsDir;
import static org.mule.runtime.deployment.model.internal.DefaultRegionPluginClassLoadersFactory.PLUGIN_CLASSLOADER_IDENTIFIER;
import static org.mule.runtime.deployment.model.internal.DefaultRegionPluginClassLoadersFactory.getArtifactPluginId;

/**
 * Creates default mule applications
 */
public class DefaultVoltronFactory extends AbstractDeployableArtifactFactory<ApplicationDescriptor, Application>
    implements ArtifactFactory<ApplicationDescriptor, Application> {

  private final DomainRepository domainRepository;
  private final ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory;
  private final ServiceRepository serviceRepository;
  private final ExtensionModelLoaderRepository extensionModelLoaderRepository;
  private final ClassLoaderRepository classLoaderRepository;
  private final LicenseValidator licenseValidator;

  public DefaultVoltronFactory(ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory,
                               DomainRepository domainRepository,
                               ServiceRepository serviceRepository,
                               ExtensionModelLoaderRepository extensionModelLoaderRepository,
                               ClassLoaderRepository classLoaderRepository,
                               LicenseValidator licenseValidator,
                               LockFactory runtimeLockFactory,
                               MemoryManagementService memoryManagementService,
                               ArtifactConfigurationProcessor artifactConfigurationProcessor) {
    super(licenseValidator, runtimeLockFactory, memoryManagementService, artifactConfigurationProcessor);
    checkArgument(applicationClassLoaderBuilderFactory != null, "Application classloader builder factory cannot be null");
    checkArgument(domainRepository != null, "Domain repository cannot be null");
    checkArgument(serviceRepository != null, "Service repository cannot be null");
    checkArgument(extensionModelLoaderRepository != null, "extensionModelLoaderRepository cannot be null");
    checkArgument(classLoaderRepository != null, "classLoaderRepository cannot be null");
    checkArgument(memoryManagementService != null, "memoryManagementService cannot be null");
    checkArgument(artifactConfigurationProcessor != null, "artifactConfigurationProcessor cannot be null");

    this.classLoaderRepository = classLoaderRepository;
    this.applicationClassLoaderBuilderFactory = applicationClassLoaderBuilderFactory;
    this.domainRepository = domainRepository;
    this.serviceRepository = serviceRepository;
    this.extensionModelLoaderRepository = extensionModelLoaderRepository;
    this.licenseValidator = licenseValidator;
  }

  protected Application doCreateArtifact(File artifactDir, ArtifactAst artifactAst, Optional<Properties> deploymentProperties)
      throws IOException {
    String appName = artifactDir.getName();
    if (appName.contains(" ")) {
      throw new IllegalArgumentException("Mule application name may not contain spaces: " + appName);
    }

    final ApplicationDescriptor descriptor = createArtifactDescriptor(artifactDir, deploymentProperties);
    return createArtifact(descriptor, artifactAst);
  }

  @Override
  public File getArtifactDir() {
    return getMuleAppsDir();
  }

  @Override
  protected Application doCreateArtifact(File artifactDir, Optional<Properties> properties) throws IOException {
    // TODO fix this
    throw new NotImplementedException("method call not expected");
  }

  @Override
  public ApplicationDescriptor createArtifactDescriptor(File artifactLocation, Optional<Properties> deploymentProperties) {
    // TODO do not call createDeployableProjectModel, hardcode values
    return createArtifactDescriptor(artifactLocation, createDeployableProjectModel(artifactLocation, false),
                                    deploymentProperties);
  }


  @Override
  protected DeployableProjectModel createDeployableProjectModel(File artifactLocation, boolean isDomain) {
    // TODO remove this fake bundle descriptor
    BundleDescriptor fakeBundleDescriptor = new BundleDescriptor.Builder()
        .setGroupId("fake")
        .setArtifactId("fake")
        .setVersion("1.0.0")
        .build();

    return new DeployableProjectModel(emptyList(), emptyList(), emptyList(), fakeBundleDescriptor, () -> null, artifactLocation,
                                      emptyList(), emptySet(), emptyMap());
  }

  public ApplicationDescriptor createArtifactDescriptor(File artifactLocation, DeployableProjectModel model,
                                                        Optional<Properties> deploymentProperties) {
    ApplicationDescriptor applicationDescriptor = new ApplicationDescriptor(artifactLocation.getName(), deploymentProperties);
    applicationDescriptor.setArtifactLocation(artifactLocation);
    return applicationDescriptor;
  }

  public Application createArtifact(ApplicationDescriptor descriptor, ArtifactAst artifactAst) throws IOException {
    Domain domain = getDomain();

    ApplicationClassLoaderBuilder artifactClassLoaderBuilder =
        applicationClassLoaderBuilderFactory.createArtifactClassLoaderBuilder();
    MuleDeployableArtifactClassLoader applicationClassLoader =
        artifactClassLoaderBuilder
            .setDomainParentClassLoader((ArtifactClassLoader) domain.getArtifactClassLoader().getClassLoader().getParent())
            .setArtifactDescriptor(descriptor).build();

    List<ArtifactPlugin> artifactPlugins = emptyList();

    DefaultVoltronIntegration delegate =
        new DefaultVoltronIntegration(descriptor, applicationClassLoader, artifactPlugins, domainRepository,
                                      serviceRepository, extensionModelLoaderRepository, descriptor.getArtifactLocation(),
                                      classLoaderRepository, null, getRuntimeLockFactory(),
                                      new ArtifactMemoryManagementService(getMemoryManagementService()),
                                      getArtifactConfigurationProcessor(),
                                      artifactAst);

    return new ApplicationWrapper(delegate);
  }

  public Application createArtifact(File artifactDir, ArtifactAst artifactAst, Optional<Properties> deploymentProperties)
      throws IOException {
    return doCreateArtifact(artifactDir, artifactAst, deploymentProperties);
  }

  private Domain getDomain() {
    Domain domain;
    try {
      domain = domainRepository.getDomain("io-domain");
    } catch (DomainNotFoundException e) {
      throw new RuntimeException(e);
    }
    return domain;
  }

}
