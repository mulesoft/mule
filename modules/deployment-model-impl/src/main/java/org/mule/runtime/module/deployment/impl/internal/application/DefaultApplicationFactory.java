/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.deployment.model.internal.DefaultRegionPluginClassLoadersFactory.PLUGIN_CLASSLOADER_IDENTIFIER;
import static org.mule.runtime.deployment.model.internal.DefaultRegionPluginClassLoadersFactory.getArtifactPluginId;
import static org.mule.runtime.module.deployment.impl.internal.application.DefaultMuleApplication.getApplicationDomain;
import static org.mule.runtime.module.deployment.impl.internal.artifact.MuleDeployableProjectModelBuilder.isHeavyPackage;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

import static com.google.common.collect.Maps.fromProperties;

import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.builder.ApplicationClassLoaderBuilder;
import org.mule.runtime.deployment.model.api.builder.ApplicationClassLoaderBuilderFactory;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.plugin.resolver.PluginDependenciesResolver;
import org.mule.runtime.internal.memory.management.ArtifactMemoryManagementService;
import org.mule.runtime.module.artifact.activation.api.descriptor.DeployableArtifactDescriptorCreator;
import org.mule.runtime.module.artifact.activation.api.descriptor.DeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.activation.api.descriptor.DomainDescriptorResolutionException;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelLoaderRepository;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.deployment.impl.internal.artifact.AbstractDeployableArtifactFactory;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactory;
import org.mule.runtime.module.deployment.impl.internal.artifact.MuleDeployableProjectModelBuilder;
import org.mule.runtime.module.deployment.impl.internal.domain.AmbiguousDomainReferenceException;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainNotFoundException;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainRepository;
import org.mule.runtime.module.deployment.impl.internal.domain.IncompatibleDomainException;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;
import org.mule.runtime.module.deployment.impl.internal.plugin.DefaultArtifactPlugin;
import org.mule.runtime.module.deployment.impl.internal.policy.DefaultPolicyInstanceProviderFactory;
import org.mule.runtime.module.deployment.impl.internal.policy.DefaultPolicyTemplateFactory;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateClassLoaderBuilderFactory;
import org.mule.runtime.module.license.api.LicenseValidator;
import org.mule.runtime.module.reboot.api.MuleContainerBootstrapUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Creates default mule applications
 */
public class DefaultApplicationFactory extends AbstractDeployableArtifactFactory<ApplicationDescriptor, Application>
    implements ArtifactFactory<ApplicationDescriptor, Application> {

  // TODO - W-11086334: remove old application descriptor factory with the migration
  private final ApplicationDescriptorFactory applicationDescriptorFactory;
  private final DeployableArtifactDescriptorFactory deployableArtifactDescriptorFactory;
  private final DomainRepository domainRepository;
  private final ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory;
  private final ServiceRepository serviceRepository;
  private final ExtensionModelLoaderRepository extensionModelLoaderRepository;
  private final ClassLoaderRepository classLoaderRepository;
  private final PolicyTemplateClassLoaderBuilderFactory policyTemplateClassLoaderBuilderFactory;
  private final PluginDependenciesResolver pluginDependenciesResolver;
  private final ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader;
  private final LicenseValidator licenseValidator;

  public DefaultApplicationFactory(ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory,
                                   ApplicationDescriptorFactory applicationDescriptorFactory,
                                   DeployableArtifactDescriptorFactory deployableArtifactDescriptorFactory,
                                   DomainRepository domainRepository,
                                   ServiceRepository serviceRepository,
                                   ExtensionModelLoaderRepository extensionModelLoaderRepository,
                                   ClassLoaderRepository classLoaderRepository,
                                   PolicyTemplateClassLoaderBuilderFactory policyTemplateClassLoaderBuilderFactory,
                                   PluginDependenciesResolver pluginDependenciesResolver,
                                   ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader,
                                   LicenseValidator licenseValidator,
                                   LockFactory runtimeLockFactory,
                                   MemoryManagementService memoryManagementService,
                                   ArtifactConfigurationProcessor artifactConfigurationProcessor) {
    super(licenseValidator, runtimeLockFactory, memoryManagementService, artifactConfigurationProcessor);
    checkArgument(applicationClassLoaderBuilderFactory != null, "Application classloader builder factory cannot be null");
    checkArgument(applicationDescriptorFactory != null, "Application descriptor factory cannot be null");
    checkArgument(deployableArtifactDescriptorFactory != null, "Deployable artifact descriptor factory cannot be null");
    checkArgument(domainRepository != null, "Domain repository cannot be null");
    checkArgument(serviceRepository != null, "Service repository cannot be null");
    checkArgument(extensionModelLoaderRepository != null, "extensionModelLoaderRepository cannot be null");
    checkArgument(classLoaderRepository != null, "classLoaderRepository cannot be null");
    checkArgument(policyTemplateClassLoaderBuilderFactory != null, "policyClassLoaderBuilderFactory cannot be null");
    checkArgument(pluginDependenciesResolver != null, "pluginDependenciesResolver cannot be null");
    checkArgument(artifactPluginDescriptorLoader != null, "artifactPluginDescriptorLoader cannot be null");
    checkArgument(memoryManagementService != null, "memoryManagementService cannot be null");
    checkArgument(artifactConfigurationProcessor != null, "artifactConfigurationProcessor cannot be null");

    this.classLoaderRepository = classLoaderRepository;
    this.applicationClassLoaderBuilderFactory = applicationClassLoaderBuilderFactory;
    this.applicationDescriptorFactory = applicationDescriptorFactory;
    this.deployableArtifactDescriptorFactory = deployableArtifactDescriptorFactory;
    this.domainRepository = domainRepository;
    this.serviceRepository = serviceRepository;
    this.extensionModelLoaderRepository = extensionModelLoaderRepository;
    this.policyTemplateClassLoaderBuilderFactory = policyTemplateClassLoaderBuilderFactory;
    this.pluginDependenciesResolver = pluginDependenciesResolver;
    this.artifactPluginDescriptorLoader = artifactPluginDescriptorLoader;
    this.licenseValidator = licenseValidator;
  }

  @Override
  protected Application doCreateArtifact(File artifactDir, Optional<Properties> properties) throws IOException {
    String appName = artifactDir.getName();
    if (appName.contains(" ")) {
      throw new IllegalArgumentException("Mule application name may not contain spaces: " + appName);
    }

    final ApplicationDescriptor descriptor = createArtifactDescriptor(artifactDir, properties);
    return createArtifact(descriptor);
  }

  @Override
  public File getArtifactDir() {
    return MuleContainerBootstrapUtils.getMuleAppsDir();
  }

  @Override
  public ApplicationDescriptor createArtifactDescriptor(File artifactLocation, Optional<Properties> deploymentProperties) {
    // TODO - W-11086334: remove this conditional during lightweight deployment migration
    if (isHeavyPackage(artifactLocation)) {
      return deployableArtifactDescriptorFactory
          .createApplicationDescriptor(createDeployableProjectModel(artifactLocation),
                                       deploymentProperties.map(dp -> (Map<String, String>) fromProperties(dp))
                                           .orElse(emptyMap()),
                                       (domainName,
                                        bundleDescriptor) -> getDomainForDescriptor(domainName, bundleDescriptor,
                                                                                    artifactLocation)
                                                                                        .getDescriptor(),
                                       getDescriptorCreator());
    } else {
      return applicationDescriptorFactory.create(artifactLocation, deploymentProperties);
    }
  }

  private DeployableArtifactDescriptorCreator<ApplicationDescriptor> getDescriptorCreator() {
    return new DeployableArtifactDescriptorCreator<ApplicationDescriptor>() {

      @Override
      public ApplicationDescriptor create(String name) {
        return new org.mule.runtime.deployment.model.api.application.ApplicationDescriptor(name);
      }

      @Override
      public ApplicationDescriptor create(String name, Optional<Properties> deploymentProperties) {
        return new org.mule.runtime.deployment.model.api.application.ApplicationDescriptor(name, deploymentProperties);
      }
    };
  }

  public Application createArtifact(ApplicationDescriptor descriptor) throws IOException {
    Domain domain = getDomainForDescriptor(descriptor);

    // TODO - W-11086334: remove this conditional during lightweight deployment migration
    if (!isHeavyPackage(descriptor.getArtifactLocation())) {
      List<ArtifactPluginDescriptor> resolvedArtifactPluginDescriptors =
          pluginDependenciesResolver.resolve(domain.getDescriptor().getPlugins(),
                                             new ArrayList<>(getArtifactPluginDescriptors(descriptor)), true);

      // Refreshes the list of plugins on the descriptor with the resolved from domain and transitive plugin dependencies
      Set<ArtifactPluginDescriptor> resolvedArtifactPlugins = new LinkedHashSet<>(resolvedArtifactPluginDescriptors);
      descriptor.setPlugins(resolvedArtifactPlugins);
    }

    ApplicationClassLoaderBuilder artifactClassLoaderBuilder =
        applicationClassLoaderBuilderFactory.createArtifactClassLoaderBuilder();
    MuleDeployableArtifactClassLoader applicationClassLoader =
        artifactClassLoaderBuilder
            .setDomainParentClassLoader((ArtifactClassLoader) domain.getArtifactClassLoader().getClassLoader().getParent())
            .setArtifactDescriptor(descriptor).build();

    List<ArtifactPlugin> artifactPlugins =
        createArtifactPluginList(applicationClassLoader, new ArrayList<>(descriptor.getPlugins()));

    MuleApplicationPolicyProvider applicationPolicyProvider =
        new MuleApplicationPolicyProvider(
                                          new DefaultPolicyTemplateFactory(policyTemplateClassLoaderBuilderFactory,
                                                                           pluginDependenciesResolver,
                                                                           licenseValidator),
                                          new DefaultPolicyInstanceProviderFactory(serviceRepository,
                                                                                   classLoaderRepository,
                                                                                   extensionModelLoaderRepository,
                                                                                   getArtifactConfigurationProcessor()));
    DefaultMuleApplication delegate =
        new DefaultMuleApplication(descriptor, applicationClassLoader, artifactPlugins, domainRepository,
                                   serviceRepository, extensionModelLoaderRepository, descriptor.getArtifactLocation(),
                                   classLoaderRepository, applicationPolicyProvider, getRuntimeLockFactory(),
                                   new ArtifactMemoryManagementService(getMemoryManagementService()),
                                   getArtifactConfigurationProcessor());

    applicationPolicyProvider.setApplication(delegate);
    return new ApplicationWrapper(delegate);
  }

  private Domain getDomainForDescriptor(ApplicationDescriptor descriptor) {
    // TODO - W-11086334: remove this conditional during lightweight deployment migration
    if (isHeavyPackage(descriptor.getArtifactLocation())) {
      return getDomainForDescriptor(descriptor.getDomainName(), descriptor.getDomainDescriptor().orElse(null),
                                    descriptor.getArtifactLocation());
    } else {
      try {
        return getApplicationDomain(domainRepository, descriptor);
      } catch (DomainNotFoundException e) {
        throw new DeploymentException(createStaticMessage(format("Domain '%s' has to be deployed in order to deploy Application '%s'",
                                                                 e.getDomainName(), descriptor.getName())),
                                      e);
      } catch (IncompatibleDomainException e) {
        throw new DeploymentException(createStaticMessage("Domain was found, but the bundle descriptor is incompatible"), e);
      } catch (AmbiguousDomainReferenceException e) {
        throw new DeploymentException(createStaticMessage("Multiple domains were found"), e);
      }
    }
  }

  private Domain getDomainForDescriptor(String domainName, BundleDescriptor domainBundleDescriptor, File artifactLocation) {
    try {
      return domainName != null ? domainRepository.getDomain(domainName)
          : domainRepository.getCompatibleDomain(domainBundleDescriptor);
    } catch (DomainNotFoundException e) {
      throw new DeploymentException(createStaticMessage(format("Domain '%s' has to be deployed in order to deploy Application in '%s'",
                                                               e.getDomainName(), artifactLocation.toString())),
                                    e);
    } catch (DomainDescriptorResolutionException e) {
      throw new DeploymentException(createStaticMessage(format("Problems found while retrieving domain '%s'", domainName)), e);
    } catch (AmbiguousDomainReferenceException e) {
      throw new DeploymentException(createStaticMessage("Multiple domains were found"), e);
    }
  }

  private Set<ArtifactPluginDescriptor> getArtifactPluginDescriptors(ApplicationDescriptor descriptor) {
    if (descriptor.getPlugins().isEmpty()) {
      Set<ArtifactPluginDescriptor> pluginDescriptors = new HashSet<>();

      for (BundleDependency bundleDependency : descriptor.getClassLoaderModel().getDependencies()) {
        if (bundleDependency.getDescriptor().isPlugin()) {
          File pluginZip = new File(bundleDependency.getBundleUri());
          try {
            pluginDescriptors.add(artifactPluginDescriptorLoader.load(pluginZip, bundleDependency.getDescriptor(), descriptor));
          } catch (IOException e) {
            throw new IllegalStateException("Cannot create plugin descriptor: " + pluginZip.getAbsolutePath(), e);
          }
        }
      }
      return pluginDescriptors;
    } else {
      return descriptor.getPlugins();
    }
  }

  private List<ArtifactPlugin> createArtifactPluginList(MuleDeployableArtifactClassLoader applicationClassLoader,
                                                        List<ArtifactPluginDescriptor> plugins) {
    return plugins.stream()
        .map(artifactPluginDescriptor -> new DefaultArtifactPlugin(getArtifactPluginId(applicationClassLoader.getArtifactId(),
                                                                                       artifactPluginDescriptor.getName()),
                                                                   artifactPluginDescriptor, applicationClassLoader
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

}
