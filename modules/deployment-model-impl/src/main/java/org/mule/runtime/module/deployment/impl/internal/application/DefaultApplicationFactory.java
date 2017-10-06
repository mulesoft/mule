/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.DEFAULT_DOMAIN_NAME;
import static org.mule.runtime.deployment.model.internal.DefaultRegionPluginClassLoadersFactory.PLUGIN_CLASSLOADER_IDENTIFIER;
import static org.mule.runtime.deployment.model.internal.DefaultRegionPluginClassLoadersFactory.getArtifactPluginId;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorUtils.isCompatibleVersion;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.internal.application.ApplicationClassLoaderBuilder;
import org.mule.runtime.deployment.model.internal.plugin.PluginDependenciesResolver;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.deployment.impl.internal.artifact.AbstractDeployableArtifactFactory;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainRepository;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;
import org.mule.runtime.module.deployment.impl.internal.plugin.DefaultArtifactPlugin;
import org.mule.runtime.module.deployment.impl.internal.policy.DefaultPolicyInstanceProviderFactory;
import org.mule.runtime.module.deployment.impl.internal.policy.DefaultPolicyTemplateFactory;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateClassLoaderBuilderFactory;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderRepository;
import org.mule.runtime.module.license.api.LicenseValidator;
import org.mule.runtime.module.reboot.api.MuleContainerBootstrapUtils;
import org.mule.runtime.module.service.api.manager.ServiceRepository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Creates default mule applications
 */
public class DefaultApplicationFactory extends AbstractDeployableArtifactFactory<Application>
    implements ArtifactFactory<Application> {

  private final ApplicationDescriptorFactory applicationDescriptorFactory;
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
                                   DomainRepository domainRepository,
                                   ServiceRepository serviceRepository,
                                   ExtensionModelLoaderRepository extensionModelLoaderRepository,
                                   ClassLoaderRepository classLoaderRepository,
                                   PolicyTemplateClassLoaderBuilderFactory policyTemplateClassLoaderBuilderFactory,
                                   PluginDependenciesResolver pluginDependenciesResolver,
                                   ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader,
                                   LicenseValidator licenseValidator) {
    super(licenseValidator);
    checkArgument(applicationClassLoaderBuilderFactory != null, "Application classloader builder factory cannot be null");
    checkArgument(applicationDescriptorFactory != null, "Application descriptor factory cannot be null");
    checkArgument(domainRepository != null, "Domain repository cannot be null");
    checkArgument(serviceRepository != null, "Service repository cannot be null");
    checkArgument(extensionModelLoaderRepository != null, "extensionModelLoaderRepository cannot be null");
    checkArgument(classLoaderRepository != null, "classLoaderRepository cannot be null");
    checkArgument(policyTemplateClassLoaderBuilderFactory != null, "policyClassLoaderBuilderFactory cannot be null");
    checkArgument(pluginDependenciesResolver != null, "pluginDependenciesResolver cannot be null");
    checkArgument(artifactPluginDescriptorLoader != null, "artifactPluginDescriptorLoader cannot be null");

    this.classLoaderRepository = classLoaderRepository;
    this.applicationClassLoaderBuilderFactory = applicationClassLoaderBuilderFactory;
    this.applicationDescriptorFactory = applicationDescriptorFactory;
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

    final ApplicationDescriptor descriptor = applicationDescriptorFactory.create(artifactDir, properties);

    return createArtifact(descriptor);
  }

  @Override
  public File getArtifactDir() {
    return MuleContainerBootstrapUtils.getMuleAppsDir();
  }

  public Application createArtifact(ApplicationDescriptor descriptor) throws IOException {
    Domain domain = getApplicationDomain(descriptor);

    List<ArtifactPluginDescriptor> applicationPluginDescriptors =
        getArtifactPluginDescriptors(domain.getDescriptor().getPlugins(), descriptor);
    List<ArtifactPluginDescriptor> resolvedArtifactPluginDescriptors =
        pluginDependenciesResolver.resolve(applicationPluginDescriptors);

    ApplicationClassLoaderBuilder artifactClassLoaderBuilder =
        applicationClassLoaderBuilderFactory.createArtifactClassLoaderBuilder();
    MuleDeployableArtifactClassLoader applicationClassLoader =
        artifactClassLoaderBuilder
            .setDomain(domain)
            .addArtifactPluginDescriptors(resolvedArtifactPluginDescriptors.toArray(new ArtifactPluginDescriptor[0]))
            .setArtifactId(descriptor.getName()).setArtifactDescriptor(descriptor).build();


    List<ArtifactPlugin> artifactPlugins =
        createArtifactPluginList(applicationClassLoader, resolvedArtifactPluginDescriptors);

    MuleApplicationPolicyProvider applicationPolicyProvider =
        new MuleApplicationPolicyProvider(
                                          new DefaultPolicyTemplateFactory(policyTemplateClassLoaderBuilderFactory,
                                                                           pluginDependenciesResolver,
                                                                           licenseValidator),
                                          new DefaultPolicyInstanceProviderFactory(
                                                                                   serviceRepository,
                                                                                   classLoaderRepository,
                                                                                   extensionModelLoaderRepository));
    DefaultMuleApplication delegate =
        new DefaultMuleApplication(descriptor, applicationClassLoader, artifactPlugins, domainRepository,
                                   serviceRepository, extensionModelLoaderRepository, descriptor.getArtifactLocation(),
                                   classLoaderRepository,
                                   applicationPolicyProvider);

    applicationPolicyProvider.setApplication(delegate);
    return new ApplicationWrapper(delegate);
  }

  private Domain getApplicationDomain(ApplicationDescriptor descriptor) {
    Optional<BundleDescriptor> domainDescriptor = descriptor.getDomainDescriptor();

    Domain domain;
    if (domainDescriptor.isPresent()) {
      domain = domainRepository.getDomain(domainDescriptor.get().getArtifactFileName());
    } else {

      domain = domainRepository.getDomain(DEFAULT_DOMAIN_NAME);
    }

    if (domain == null) {
      throw new DeploymentException(createStaticMessage(format("Domain '%s' "
          + "has to be deployed in order to deploy Application '%s'",
                                                               domainDescriptor.isPresent()
                                                                   ? domainDescriptor.get().getArtifactId() : DEFAULT_DOMAIN_NAME,
                                                               descriptor.getName())));
    }

    return domain;
  }

  private List<ArtifactPluginDescriptor> getArtifactPluginDescriptors(Set<ArtifactPluginDescriptor> plugins,
                                                                      ApplicationDescriptor descriptor) {
    List<ArtifactPluginDescriptor> artifactPluginDescriptors = new ArrayList<>();

    for (ArtifactPluginDescriptor appPluginDescriptor : getArtifactPluginDescriptors(descriptor)) {
      Optional<ArtifactPluginDescriptor> pluginDescriptor = findPlugin(plugins, appPluginDescriptor.getBundleDescriptor());

      if (!pluginDescriptor.isPresent()) {
        artifactPluginDescriptors.add(appPluginDescriptor);
      } else if (!isCompatibleVersion(pluginDescriptor.get().getBundleDescriptor().getVersion(),
                                      appPluginDescriptor.getBundleDescriptor().getVersion())) {
        throw new IllegalStateException(
                                        format("Incompatible version of plugin '%s' found. Application requires version'%s' but domain provides version'%s'",
                                               appPluginDescriptor.getName(),
                                               appPluginDescriptor.getBundleDescriptor().getVersion(),
                                               pluginDescriptor.get().getBundleDescriptor().getVersion()));
      }
    }
    return artifactPluginDescriptors;
  }

  private Set<ArtifactPluginDescriptor> getArtifactPluginDescriptors(ApplicationDescriptor descriptor) {
    if (descriptor.getPlugins().isEmpty()) {
      Set<ArtifactPluginDescriptor> pluginDescriptors = new HashSet<>();

      for (BundleDependency bundleDependency : descriptor.getClassLoaderModel().getDependencies()) {
        if (bundleDependency.getDescriptor().isPlugin()) {
          File pluginZip = new File(bundleDependency.getBundleUri());
          try {
            pluginDescriptors.add(artifactPluginDescriptorLoader.load(pluginZip));
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

  private Optional<ArtifactPluginDescriptor> findPlugin(Set<ArtifactPluginDescriptor> appPlugins,
                                                        BundleDescriptor bundleDescriptor) {
    for (ArtifactPluginDescriptor appPlugin : appPlugins) {
      if (appPlugin.getBundleDescriptor().getArtifactId().equals(bundleDescriptor.getArtifactId())
          && appPlugin.getBundleDescriptor().getGroupId().equals(bundleDescriptor.getGroupId())) {
        return of(appPlugin);
      }
    }

    return empty();
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
