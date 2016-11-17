/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.deployment.model.internal.AbstractArtifactClassLoaderBuilder.getArtifactPluginId;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginRepository;
import org.mule.runtime.module.artifact.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactory;
import org.mule.runtime.module.deployment.impl.internal.artifact.MuleContextListenerFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainRepository;
import org.mule.runtime.module.deployment.impl.internal.plugin.DefaultArtifactPlugin;
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
  private final ClassLoaderRepository classLoaderRepository;
  private MuleContextListenerFactory muleContextListenerFactory;

  public DefaultApplicationFactory(ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory,
                                   ApplicationDescriptorFactory applicationDescriptorFactory,
                                   ArtifactPluginRepository artifactPluginRepository, DomainRepository domainRepository,
                                   ServiceRepository serviceRepository,
                                   ClassLoaderRepository classLoaderRepository) {
    this.classLoaderRepository = classLoaderRepository;
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

  public void setMuleContextListenerFactory(MuleContextListenerFactory muleContextListenerFactory) {
    this.muleContextListenerFactory = muleContextListenerFactory;
  }

  public Application createArtifact(File appDir) throws IOException {
    String appName = appDir.getName();
    if (appName.contains(" ")) {
      throw new IllegalArgumentException("Mule application name may not contain spaces: " + appName);
    }

    final ApplicationDescriptor descriptor = applicationDescriptorFactory.create(appDir);

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

    MuleDeployableArtifactClassLoader applicationClassLoader =
        applicationClassLoaderBuilderFactory.createArtifactClassLoaderBuilder()
            .setDomain(domain).addArtifactPluginDescriptors(descriptor.getPlugins().toArray(new ArtifactPluginDescriptor[0]))
            .setArtifactId(descriptor.getName()).setArtifactDescriptor(descriptor).build();

    List<ArtifactPluginDescriptor> applicationPluginDescriptors =
        concat(artifactPluginRepository.getContainerArtifactPluginDescriptors().stream(), descriptor.getPlugins().stream())
            .collect(toList());

    List<ArtifactPlugin> artifactPlugins = createArtifactPluginList(applicationClassLoader, applicationPluginDescriptors);

    DefaultMuleApplication delegate =
        new DefaultMuleApplication(descriptor, applicationClassLoader, artifactPlugins, domainRepository,
                                   serviceRepository, descriptor.getArtifactLocation(), classLoaderRepository);

    if (muleContextListenerFactory != null) {
      delegate.setMuleContextListener(muleContextListenerFactory.create(descriptor.getName()));
    }

    return new ApplicationWrapper(delegate);
  }

  private List<ArtifactPlugin> createArtifactPluginList(MuleDeployableArtifactClassLoader applicationClassLoader,
                                                        List<ArtifactPluginDescriptor> plugins) {
    return plugins.stream()
        .map(artifactPluginDescriptor -> new DefaultArtifactPlugin(getArtifactPluginId(applicationClassLoader.getArtifactId(),
                                                                                       artifactPluginDescriptor.getName()),
                                                                   artifactPluginDescriptor, applicationClassLoader
                                                                       .getArtifactPluginClassLoaders().stream()
                                                                       .filter(artifactClassLoader -> artifactClassLoader
                                                                           .getArtifactId()
                                                                           .endsWith(artifactPluginDescriptor.getName()))
                                                                       .findFirst().get()))
        .collect(toList());
  }

}
