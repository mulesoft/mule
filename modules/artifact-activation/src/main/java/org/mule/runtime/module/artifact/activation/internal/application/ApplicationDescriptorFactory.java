/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.application;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.artifact.activation.api.deployable.ArtifactModelResolver.applicationModelResolver;
import static org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor.DEFAULT_CONFIGURATION_RESOURCE;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorUtils.isCompatibleVersion;
import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.DEFAULT_DOMAIN_NAME;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.deployment.meta.MuleApplicationModel;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.api.descriptor.DomainDescriptorResolutionException;
import org.mule.runtime.module.artifact.activation.api.descriptor.DomainDescriptorResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginDescriptorResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginModelResolver;
import org.mule.runtime.module.artifact.activation.internal.deployable.AbstractDeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Creates an artifact descriptor for an application.
 */
public class ApplicationDescriptorFactory
    extends AbstractDeployableArtifactDescriptorFactory<MuleApplicationModel, ApplicationDescriptor> {

  private final DomainDescriptorResolver domainDescriptorResolver;

  public ApplicationDescriptorFactory(DeployableProjectModel deployableProjectModel,
                                      Map<String, String> deploymentProperties, PluginModelResolver pluginModelResolver,
                                      PluginDescriptorResolver pluginDescriptorResolver,
                                      ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder,
                                      DomainDescriptorResolver domainDescriptorResolver) {
    super(deployableProjectModel, deploymentProperties, pluginModelResolver, pluginDescriptorResolver,
          artifactDescriptorValidatorBuilder);
    this.domainDescriptorResolver = domainDescriptorResolver;
  }

  @Override
  protected MuleApplicationModel createArtifactModel() {
    return applicationModelResolver().resolve(getArtifactLocation());
  }

  @Override
  protected void doValidation(ApplicationDescriptor descriptor) {
    super.doValidation(descriptor);
    Set<ArtifactPluginDescriptor> domainPlugins =
        getApplicationDomainDescriptor(descriptor).map(DeployableArtifactDescriptor::getPlugins).orElse(emptySet());
    List<ArtifactPluginDescriptor> resolvedArtifactPluginDescriptors =
        getPluginDependenciesResolver().resolve(domainPlugins, new ArrayList<>(descriptor.getPlugins()), true);

    // Refreshes the list of plugins on the descriptor with the resolved from domain and transitive plugin dependencies
    descriptor.setPlugins(new LinkedHashSet<>(resolvedArtifactPluginDescriptors));
  }

  private Optional<DomainDescriptor> getApplicationDomainDescriptor(ApplicationDescriptor descriptor) {
    Optional<DomainDescriptor> resolvedDomainDescriptor = resolveApplicationDomain(descriptor);
    descriptor.setDomainName(resolvedDomainDescriptor.map(ArtifactDescriptor::getName).orElse(DEFAULT_DOMAIN_NAME));
    return resolvedDomainDescriptor;
  }

  private Optional<DomainDescriptor> resolveApplicationDomain(ApplicationDescriptor descriptor) {
    String configuredDomainName = descriptor.getDomainName();
    Optional<BundleDescriptor> domainBundleDescriptor = descriptor.getDomainDescriptor();

    boolean shouldUseDefaultDomain = (configuredDomainName == null) || DEFAULT_DOMAIN_NAME.equals(configuredDomainName);
    if (!shouldUseDefaultDomain && !domainBundleDescriptor.isPresent()) {
      throw new IllegalStateException(format("Dependency for domain '%s' was not declared", configuredDomainName));
    }

    if (!domainBundleDescriptor.isPresent()) {
      return empty();
    }

    if (domainDescriptorResolver == null) {
      throw new IllegalStateException(format("Application depends on domain '%s', a domain descriptor resolver must be provided",
                                             configuredDomainName));
    }

    DomainDescriptor domainDescriptor = domainDescriptorResolver.resolve(domainBundleDescriptor.get());

    if (domainDescriptor == null) {
      throw new IllegalStateException(format("Domain '%s' couldn't be fetched", configuredDomainName));
    }

    if (!isCompatibleBundle(domainDescriptor.getBundleDescriptor(), domainBundleDescriptor.get())) {
      throw new DomainDescriptorResolutionException(createStaticMessage("Domain was found, but the bundle descriptor is incompatible"));
    }

    return of(domainDescriptor);
  }

  /**
   * Determines if a bundle descriptor is compatible with another one.
   *
   * @param available bundle descriptor that is available to use.
   * @param expected  bundle descriptor that is expected.
   * @return true if match in group and artifact id, have the same classifier and the versions are compatible, false otherwise.
   */
  private static boolean isCompatibleBundle(BundleDescriptor available, BundleDescriptor expected) {
    if (!available.getClassifier().equals(expected.getClassifier())) {
      return false;
    }

    if (!available.getGroupId().equals(expected.getGroupId())) {
      return false;
    }

    if (!available.getArtifactId().equals(expected.getArtifactId())) {
      return false;
    }

    return isCompatibleVersion(available.getVersion(), expected.getVersion());
  }

  @Override
  protected void doDescriptorConfig(ApplicationDescriptor descriptor) {
    super.doDescriptorConfig(descriptor);
    getArtifactModel().getDomain().ifPresent(descriptor::setDomainName);
  }

  @Override
  protected String getDefaultConfigurationResource() {
    return DEFAULT_CONFIGURATION_RESOURCE;
  }

  @Override
  protected ApplicationDescriptor doCreateArtifactDescriptor() {
    throw new RuntimeException("A!");
    // return new ApplicationDescriptor(getArtifactLocation().getName(), getDeploymentProperties());
  }
}
