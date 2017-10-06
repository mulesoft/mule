/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.domain;

import static java.util.Optional.empty;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainFolder;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.DEFAULT_DOMAIN_NAME;
import static org.mule.runtime.deployment.model.internal.DefaultRegionPluginClassLoadersFactory.PLUGIN_CLASSLOADER_IDENTIFIER;
import static org.mule.runtime.deployment.model.internal.DefaultRegionPluginClassLoadersFactory.getArtifactPluginId;
import static org.mule.runtime.module.reboot.api.MuleContainerBootstrapUtils.getMuleDomainsDir;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.internal.domain.DomainClassLoaderBuilder;
import org.mule.runtime.deployment.model.internal.plugin.PluginDependenciesResolver;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactory;
import org.mule.runtime.module.deployment.impl.internal.plugin.DefaultArtifactPlugin;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderManager;
import org.mule.runtime.module.service.api.manager.ServiceRepository;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

public class DefaultDomainFactory implements ArtifactFactory<Domain> {

  private final DomainManager domainManager;
  private final DomainDescriptorFactory domainDescriptorFactory;
  private final ClassLoaderRepository classLoaderRepository;
  private final ServiceRepository serviceRepository;
  private final PluginDependenciesResolver pluginDependenciesResolver;
  private final DomainClassLoaderBuilderFactory domainClassLoaderBuilderFactory;

  private ExtensionModelLoaderManager extensionModelLoaderManager;

  /**
   * Creates a new domain factory
   *
   * @param domainDescriptorFactory creates descriptors for the new domains. Non null.
   * @param domainManager tracks the domains deployed on the container. Non null.
   * @param classLoaderRepository contains all the class loaders in the container. Non null.
   * @param serviceRepository repository of available services. Non null.
   * @param pluginDependenciesResolver resolver for the plugins on which the {@code artifactPluginDescriptor} declares it depends.
   *        Non null.
   * @param domainClassLoaderBuilderFactory creates builders to build the classloaders for each domain. Non null.
   * @param extensionModelLoaderManager manager capable of resolve {@link ExtensionModel extension models}. Non null.
   */
  public DefaultDomainFactory(DomainDescriptorFactory domainDescriptorFactory,
                              DomainManager domainManager,
                              ClassLoaderRepository classLoaderRepository, ServiceRepository serviceRepository,
                              PluginDependenciesResolver pluginDependenciesResolver,
                              DomainClassLoaderBuilderFactory domainClassLoaderBuilderFactory,
                              ExtensionModelLoaderManager extensionModelLoaderManager) {
    checkArgument(domainDescriptorFactory != null, "domainDescriptorFactory cannot be null");
    checkArgument(domainManager != null, "Domain manager cannot be null");
    checkArgument(serviceRepository != null, "Service repository cannot be null");
    checkArgument(pluginDependenciesResolver != null, "pluginDependenciesResolver cannot be null");
    checkArgument(domainClassLoaderBuilderFactory != null, "domainClassLoaderBuilderFactory cannot be null");
    checkArgument(extensionModelLoaderManager != null, "extensionModelLoaderManager cannot be null");

    this.classLoaderRepository = classLoaderRepository;
    this.domainDescriptorFactory = domainDescriptorFactory;
    this.domainManager = domainManager;
    this.serviceRepository = serviceRepository;
    this.pluginDependenciesResolver = pluginDependenciesResolver;
    this.domainClassLoaderBuilderFactory = domainClassLoaderBuilderFactory;
    this.extensionModelLoaderManager = extensionModelLoaderManager;
  }

  public Domain createArtitact(DomainDescriptor descriptor) throws IOException {
    List<ArtifactPluginDescriptor> artifactPluginDescriptors = descriptor.getPlugins().stream().collect(Collectors.toList());
    List<ArtifactPluginDescriptor> resolvedArtifactPluginDescriptors =
        pluginDependenciesResolver.resolve(artifactPluginDescriptors);

    DomainClassLoaderBuilder artifactClassLoaderBuilder =
        domainClassLoaderBuilderFactory.createArtifactClassLoaderBuilder();
    MuleDeployableArtifactClassLoader domainClassLoader =
        artifactClassLoaderBuilder
            .addArtifactPluginDescriptors(resolvedArtifactPluginDescriptors.toArray(new ArtifactPluginDescriptor[0]))
            .setArtifactId(descriptor.getName()).setArtifactDescriptor(descriptor).build();

    List<ArtifactPlugin> artifactPlugins =
        createArtifactPluginList(domainClassLoader, resolvedArtifactPluginDescriptors);

    DefaultMuleDomain defaultMuleDomain =
        new DefaultMuleDomain(descriptor, domainClassLoader, classLoaderRepository, serviceRepository, artifactPlugins,
                              extensionModelLoaderManager);

    DomainWrapper domainWrapper = new DomainWrapper(defaultMuleDomain, this);
    domainManager.addDomain(domainWrapper);
    return domainWrapper;
  }

  private DomainDescriptor findDomain(String domainName, Optional<Properties> deploymentProperties) throws IOException {
    if (DEFAULT_DOMAIN_NAME.equals(domainName)) {
      return new EmptyDomainDescriptor(new File(getMuleDomainsDir(), DEFAULT_DOMAIN_NAME));
    }

    File domainFolder = getDomainFolder(domainName);
    DomainDescriptor descriptor = domainDescriptorFactory.create(domainFolder, deploymentProperties);

    return descriptor;
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
    domainManager.removeDomain(domain.getArtifactName());
  }

  public void start(DomainWrapper domainWrapper) {
    domainManager.addDomain(domainWrapper);
  }

  @Override
  public Domain createArtifact(File domainLocation, Optional<Properties> deploymentProperties) throws IOException {
    String domainName = domainLocation.getName();
    Domain domain = domainManager.getDomain(domainName);
    if (domain != null) {
      throw new IllegalArgumentException(format("Domain '%s'  already exists", domainName));
    }
    if (domainName.contains(" ")) {
      throw new IllegalArgumentException("Mule domain name may not contain spaces: " + domainName);
    }

    return createArtitact(findDomain(domainName, deploymentProperties));

  }
}
