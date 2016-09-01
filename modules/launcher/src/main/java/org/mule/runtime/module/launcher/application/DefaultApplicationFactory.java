/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.application;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.module.launcher.ApplicationClassLoaderBuilderFactory;
import org.mule.runtime.module.launcher.ApplicationDescriptorFactory;
import org.mule.runtime.module.launcher.DeploymentException;
import org.mule.runtime.module.launcher.DeploymentListener;
import org.mule.runtime.module.launcher.MuleApplicationClassLoader;
import org.mule.runtime.module.launcher.artifact.ArtifactFactory;
import org.mule.runtime.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.launcher.domain.Domain;
import org.mule.runtime.module.launcher.domain.DomainRepository;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginRepository;
import org.mule.runtime.module.reboot.MuleContainerBootstrapUtils;
import org.mule.runtime.module.service.ServiceRepository;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Creates default mule applications
 */
public class DefaultApplicationFactory implements ArtifactFactory<Application> {

  private final ApplicationDescriptorFactory applicationDescriptorFactory;
  private final DomainRepository domainRepository;
  private final ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory;
  private final ArtifactPluginRepository artifactPluginRepository;
  private final ServiceRepository serviceRepository;
  protected DeploymentListener deploymentListener;

  public DefaultApplicationFactory(ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory,
                                   ApplicationDescriptorFactory applicationDescriptorFactory,
                                   ArtifactPluginRepository artifactPluginRepository, DomainRepository domainRepository,
                                   ServiceRepository serviceRepository) {
    checkArgument(applicationClassLoaderBuilderFactory != null, "Application classloader builder factory cannot be null");
    checkArgument(applicationDescriptorFactory != null, "Application descriptor factory cannot be null");
    checkArgument(artifactPluginRepository != null, "Artifact plugin repository cannot be null");
    checkArgument(domainRepository != null, "Domain repository cannot be null");
    checkArgument(serviceRepository != null, "Service repository cannot be null");

    this.applicationClassLoaderBuilderFactory = applicationClassLoaderBuilderFactory;
    this.applicationDescriptorFactory = applicationDescriptorFactory;
    this.artifactPluginRepository = artifactPluginRepository;
    this.domainRepository = domainRepository;
    this.serviceRepository = serviceRepository;
  }

  public void setDeploymentListener(DeploymentListener deploymentListener) {
    this.deploymentListener = deploymentListener;
  }

  public Application createArtifact(String appName) throws IOException {
    if (appName.contains(" ")) {
      throw new IllegalArgumentException("Mule application name may not contain spaces: " + appName);
    }

    final File appsDir = MuleContainerBootstrapUtils.getMuleAppsDir();
    final ApplicationDescriptor descriptor = applicationDescriptorFactory.create(new File(appsDir, appName));

    return createAppFrom(descriptor);
  }

  @Override
  public File getArtifactDir() {
    return MuleContainerBootstrapUtils.getMuleAppsDir();
  }

  protected Application createAppFrom(ApplicationDescriptor descriptor) throws IOException {
    Domain domain = domainRepository.getDomain(descriptor.getDomain());

    if (domain == null) {
      throw new DeploymentException(createStaticMessage(format("Domain '%s' has to be deployed in order to deploy Application '%s'",
                                                               descriptor.getDomain(), descriptor.getName())));
    }

    MuleApplicationClassLoader applicationClassLoader = applicationClassLoaderBuilderFactory.createArtifactClassLoaderBuilder()
        .setDomain(domain).addArtifactPluginDescriptors(descriptor.getPlugins().toArray(new ArtifactPluginDescriptor[0]))
        .setArtifactId(descriptor.getName()).setArtifactDescriptor(descriptor).build();

    List<ArtifactPluginDescriptor> applicationPluginDescriptors =
        concat(artifactPluginRepository.getContainerArtifactPluginDescriptors().stream(), descriptor.getPlugins().stream())
            .collect(toList());

    List<ArtifactPlugin> artifactPlugins = createArtifactPluginList(applicationClassLoader, applicationPluginDescriptors);

    DefaultMuleApplication delegate =
        new DefaultMuleApplication(descriptor, applicationClassLoader, artifactPlugins, domainRepository, serviceRepository);

    if (deploymentListener != null) {
      delegate.setDeploymentListener(deploymentListener);
    }

    return new ApplicationWrapper(delegate);
  }

  private List<ArtifactPlugin> createArtifactPluginList(MuleApplicationClassLoader applicationClassLoader,
                                                        List<ArtifactPluginDescriptor> plugins) {
    return plugins.stream()
        .map(artifactPluginDescriptor -> new DefaultArtifactPlugin(artifactPluginDescriptor, applicationClassLoader
            .getArtifactPluginClassLoaders().stream().filter(artifactClassLoader -> artifactClassLoader.getArtifactName()
                .endsWith(artifactPluginDescriptor.getName()))
            .findFirst().get()))
        .collect(toList());
  }


}
