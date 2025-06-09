/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.domain;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.internal.util.MuleContainerUtils.getMuleDomainsDir;
import static org.mule.runtime.deployment.model.internal.DefaultRegionPluginClassLoadersFactory.PLUGIN_CLASSLOADER_IDENTIFIER;
import static org.mule.runtime.deployment.model.internal.DefaultRegionPluginClassLoadersFactory.getArtifactPluginId;
import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.DEFAULT_DOMAIN_NAME;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

import static com.google.common.collect.Maps.fromProperties;

import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.builder.DomainClassLoaderBuilder;
import org.mule.runtime.deployment.model.api.builder.DomainClassLoaderBuilderFactory;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.internal.memory.management.ArtifactMemoryManagementService;
import org.mule.runtime.module.artifact.activation.api.descriptor.DeployableArtifactDescriptorCreator;
import org.mule.runtime.module.artifact.activation.api.descriptor.DeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelLoaderRepository;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.runtime.module.deployment.impl.internal.artifact.AbstractDeployableArtifactFactory;
import org.mule.runtime.module.deployment.impl.internal.plugin.DefaultArtifactPlugin;
import org.mule.runtime.module.license.api.LicenseValidator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class DefaultDomainFactory extends AbstractDeployableArtifactFactory<DomainDescriptor, Domain> {

  private final DomainManager domainManager;
  private final DeployableArtifactDescriptorFactory deployableArtifactDescriptorFactory;
  private final ClassLoaderRepository classLoaderRepository;
  private final ServiceRepository serviceRepository;
  private final DomainClassLoaderBuilderFactory domainClassLoaderBuilderFactory;
  private final ExtensionModelLoaderRepository extensionModelLoaderRepository;

  /**
   * Creates a new domain factory
   *
   * @param domainDescriptorFactory             creates descriptors for the new domains in case of a lightweight deployment. Non
   *                                            null.
   * @param deployableArtifactDescriptorFactory creates descriptors for the new domains. Non null.
   * @param domainManager                       tracks the domains deployed on the container. Non null.
   * @param classLoaderRepository               contains all the class loaders in the container. Non null.
   * @param serviceRepository                   repository of available services. Non null.
   * @param domainClassLoaderBuilderFactory     creates builders to build the classloaders for each domain. Non null.
   * @param extensionModelLoaderRepository      manager capable of resolve {@link ExtensionModel extension models}. Non null.
   * @param artifactConfigurationProcessor      the processor to use for building the application model. Non null.
   */
  public DefaultDomainFactory(DomainDescriptorFactory domainDescriptorFactory,
                              DeployableArtifactDescriptorFactory deployableArtifactDescriptorFactory,
                              DomainManager domainManager,
                              ClassLoaderRepository classLoaderRepository,
                              ServiceRepository serviceRepository,
                              DomainClassLoaderBuilderFactory domainClassLoaderBuilderFactory,
                              ExtensionModelLoaderRepository extensionModelLoaderRepository,
                              LicenseValidator licenseValidator,
                              MemoryManagementService memoryManagementService,
                              ArtifactConfigurationProcessor artifactConfigurationProcessor) {

    super(licenseValidator, memoryManagementService, artifactConfigurationProcessor);

    checkArgument(domainDescriptorFactory != null, "domainDescriptorFactory cannot be null");
    checkArgument(deployableArtifactDescriptorFactory != null, "Deployable artifact descriptor factory cannot be null");
    checkArgument(domainManager != null, "Domain manager cannot be null");
    checkArgument(serviceRepository != null, "Service repository cannot be null");
    checkArgument(domainClassLoaderBuilderFactory != null, "domainClassLoaderBuilderFactory cannot be null");
    checkArgument(extensionModelLoaderRepository != null, "extensionModelLoaderRepository cannot be null");
    checkArgument(artifactConfigurationProcessor != null, "artifactConfigurationProcessor cannot be null");

    this.classLoaderRepository = classLoaderRepository;
    this.deployableArtifactDescriptorFactory = deployableArtifactDescriptorFactory;
    this.domainManager = domainManager;
    this.serviceRepository = serviceRepository;
    this.domainClassLoaderBuilderFactory = domainClassLoaderBuilderFactory;
    this.extensionModelLoaderRepository = extensionModelLoaderRepository;
  }

  private DomainDescriptor findDomain(String domainName, File domainLocation, Optional<Properties> deploymentProperties) {
    if (DEFAULT_DOMAIN_NAME.equals(domainName)) {
      return new EmptyDomainDescriptor(new File(getMuleDomainsDir(), DEFAULT_DOMAIN_NAME));
    }

    return deployableArtifactDescriptorFactory
        .createDomainDescriptor(createDeployableProjectModel(domainLocation, true),
                                deploymentProperties.map(dp -> (Map<String, String>) fromProperties(dp))
                                    .orElse(emptyMap()),
                                getDescriptorCreator());
  }

  private DeployableArtifactDescriptorCreator<DomainDescriptor> getDescriptorCreator() {
    return new DeployableArtifactDescriptorCreator<DomainDescriptor>() {

      @Override
      public org.mule.runtime.deployment.model.api.domain.DomainDescriptor create(String name) {
        return new org.mule.runtime.deployment.model.api.domain.DomainDescriptor(name);
      }

      @Override
      public org.mule.runtime.deployment.model.api.domain.DomainDescriptor create(String name,
                                                                                  Optional deploymentProperties) {
        return new org.mule.runtime.deployment.model.api.domain.DomainDescriptor(name,
                                                                                 deploymentProperties);
      }
    };
  }

  private List<ArtifactPlugin> createArtifactPluginList(MuleDeployableArtifactClassLoader domainClassLoader,
                                                        List<ArtifactPluginDescriptor> plugins) {
    return plugins.stream()
        .map(artifactPluginDescriptor -> new DefaultArtifactPlugin(getArtifactPluginId(domainClassLoader.getArtifactId(),
                                                                                       artifactPluginDescriptor.getName()),
                                                                   artifactPluginDescriptor, domainClassLoader
                                                                       .getArtifactPluginClassLoaders().stream()
                                                                       .filter(artifactClassLoader -> {
                                                                         final String artifactPluginDescriptorName =
                                                                             PLUGIN_CLASSLOADER_IDENTIFIER
                                                                                 + artifactPluginDescriptor.getName();
                                                                         return artifactClassLoader
                                                                             .getArtifactId()
                                                                             .endsWith(artifactPluginDescriptorName);
                                                                       })
                                                                       .findFirst().get()))
        .collect(toList());
  }

  @Override
  public File getArtifactDir() {
    return getMuleDomainsDir();
  }

  public void dispose(DomainWrapper domain) {
    domainManager.removeDomain(domain);
  }

  public void start(DomainWrapper domainWrapper) {
    domainManager.addDomain(domainWrapper);
  }

  @Override
  protected Domain doCreateArtifact(File domainLocation, Optional<Properties> deploymentProperties) throws IOException {
    String domainName = domainLocation.getName();
    if (domainManager.contains(domainName)) {
      throw new IllegalArgumentException(format("Domain '%s'  already exists", domainName));
    }
    if (domainName.contains(" ")) {
      throw new IllegalArgumentException("Mule domain name may not contain spaces: " + domainName);
    }

    DomainDescriptor domainDescriptor = findDomain(domainName, domainLocation, deploymentProperties);

    List<ArtifactPluginDescriptor> resolvedArtifactPluginDescriptors = new ArrayList<>(domainDescriptor.getPlugins());

    DomainClassLoaderBuilder artifactClassLoaderBuilder =
        domainClassLoaderBuilderFactory.createArtifactClassLoaderBuilder();
    MuleDeployableArtifactClassLoader domainClassLoader =
        artifactClassLoaderBuilder
            .setArtifactDescriptor(domainDescriptor).build();

    List<ArtifactPlugin> artifactPlugins =
        createArtifactPluginList(domainClassLoader, resolvedArtifactPluginDescriptors);

    DefaultMuleDomain defaultMuleDomain =
        new DefaultMuleDomain(domainDescriptor, domainClassLoader, classLoaderRepository, serviceRepository, artifactPlugins,
                              extensionModelLoaderRepository,
                              new ArtifactMemoryManagementService(getMemoryManagementService()),
                              getArtifactConfigurationProcessor());

    DomainWrapper domainWrapper = new DomainWrapper(defaultMuleDomain, this);
    domainManager.addDomain(domainWrapper);
    return domainWrapper;
  }

  @Override
  public DeployableArtifactDescriptor createArtifactDescriptor(File artifactLocation, Optional<Properties> deploymentProperties) {
    return deployableArtifactDescriptorFactory
        .createDomainDescriptor(createDeployableProjectModel(artifactLocation, true),
                                deploymentProperties.map(dp -> (Map<String, String>) fromProperties(dp))
                                    .orElse(emptyMap()));
  }
}
