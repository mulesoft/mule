/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.policy;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.deployment.model.internal.DefaultRegionPluginClassLoadersFactory.getArtifactPluginId;
import static org.mule.runtime.module.artifact.api.classloader.DefaultArtifactClassLoaderFilter.NULL_CLASSLOADER_FILTER;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactoryUtils.validateArtifactLicense;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.plugin.resolver.PluginDependenciesResolver;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.artifact.api.Artifact;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.deployment.impl.internal.plugin.DefaultArtifactPlugin;
import org.mule.runtime.module.license.api.LicenseValidator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Creates {@link DefaultPolicyTemplate} instances.
 */
public class DefaultPolicyTemplateFactory implements PolicyTemplateFactory {

  private final PolicyTemplateClassLoaderBuilderFactory policyTemplateClassLoaderBuilderFactory;
  private final PluginDependenciesResolver pluginDependenciesResolver;
  private final LicenseValidator licenseValidator;

  /**
   * Creates a new factory
   *
   * @param policyTemplateClassLoaderBuilderFactory creates class loader builders to create the class loaders for the created
   *                                                policy templates. Non null.
   */
  public DefaultPolicyTemplateFactory(PolicyTemplateClassLoaderBuilderFactory policyTemplateClassLoaderBuilderFactory,
                                      PluginDependenciesResolver pluginDependenciesResolver,
                                      LicenseValidator licenseValidator) {
    checkArgument(policyTemplateClassLoaderBuilderFactory != null, "policyTemplateClassLoaderBuilderFactory cannot be null");

    this.policyTemplateClassLoaderBuilderFactory = policyTemplateClassLoaderBuilderFactory;
    this.pluginDependenciesResolver = pluginDependenciesResolver;
    this.licenseValidator = licenseValidator;
  }

  @Override
  public PolicyTemplate createArtifact(Application application, PolicyTemplateDescriptor descriptor) {
    MuleDeployableArtifactClassLoader ownPolicyClassLoader;
    MuleDeployableArtifactClassLoader policyClassLoader;

    final List<ArtifactPluginDescriptor> resolvedPolicyPluginsDescriptors =
        resolvePolicyPluginDescriptors(application, descriptor);
    final List<ArtifactPluginDescriptor> ownResolvedPluginDescriptors =
        pluginDependenciesResolver.resolve(emptySet(), new ArrayList<>(descriptor.getPlugins()), false);

    try {
      ownPolicyClassLoader = policyTemplateClassLoaderBuilderFactory.createArtifactClassLoaderBuilder()
          .addArtifactPluginDescriptors(ownResolvedPluginDescriptors
              .toArray(new ArtifactPluginDescriptor[ownResolvedPluginDescriptors.size()]))
          .setParentClassLoader(policyTemplateClassLoaderBuilderFactory.getPolicyParentClassloader())
          .setArtifactDescriptor(descriptor).build();

      // This classloader needs to be created after ownPolicyClassLoader so its inner classloaders override the entries in the
      // ClassLoaderRepository for the application
      policyClassLoader = policyTemplateClassLoaderBuilderFactory.createArtifactClassLoaderBuilder()
          .addArtifactPluginDescriptors(resolvedPolicyPluginsDescriptors
              .toArray(new ArtifactPluginDescriptor[resolvedPolicyPluginsDescriptors.size()]))
          .setParentClassLoader(policyTemplateClassLoaderBuilderFactory.getPolicyParentClassloader())
          .setArtifactDescriptor(descriptor).build();
    } catch (Exception e) {
      throw new PolicyTemplateCreationException(createPolicyTemplateCreationErrorMessage(descriptor.getName()), e);
    }

    application.getRegionClassLoader().addClassLoader(policyClassLoader, NULL_CLASSLOADER_FILTER);

    List<ArtifactPlugin> artifactPlugins = createArtifactPluginList(policyClassLoader, resolvedPolicyPluginsDescriptors);

    validateArtifactLicense(policyClassLoader.getClassLoader(), artifactPlugins, licenseValidator);

    return new DefaultPolicyTemplate(policyClassLoader.getArtifactId(), descriptor, policyClassLoader,
                                     artifactPlugins,
                                     resolveOwnArtifactPlugins(artifactPlugins, ownResolvedPluginDescriptors,
                                                               ownPolicyClassLoader));
  }

  // Need all the plugins that the policy itself depends on, while keeping a relationship with the appropriate classloader.
  private List<ArtifactPlugin> resolveOwnArtifactPlugins(List<ArtifactPlugin> artifactPlugins,
                                                         List<ArtifactPluginDescriptor> ownResolvedPluginDescriptors,
                                                         MuleDeployableArtifactClassLoader ownPolicyClassLoader) {
    return ownResolvedPluginDescriptors.stream()
        .map(pluginDescriptor -> artifactPlugins.stream()
            .filter(artifactPlugin -> artifactPlugin.getDescriptor().getName().equals(pluginDescriptor.getName())).findFirst()
            .orElseGet(() -> new DefaultArtifactPlugin(getArtifactPluginId(ownPolicyClassLoader.getArtifactId(),
                                                                           pluginDescriptor.getName()),
                                                       pluginDescriptor, ownPolicyClassLoader
                                                           .getArtifactPluginClassLoaders().stream()
                                                           .filter(artifactClassLoader -> artifactClassLoader
                                                               .getArtifactId()
                                                               .endsWith(pluginDescriptor.getName()))
                                                           .findFirst().get())))
        .collect(toList());
  }

  private List<ArtifactPluginDescriptor> resolvePolicyPluginDescriptors(Application application,
                                                                        PolicyTemplateDescriptor descriptor) {
    LinkedList<ArtifactPlugin> providedArtifactPlugins = new LinkedList<>();
    providedArtifactPlugins.addAll(application.getArtifactPlugins());
    providedArtifactPlugins.addAll(application.getDomain().getArtifactPlugins());

    Set<ArtifactPluginDescriptor> providedArtifactPluginsDescriptors =
        providedArtifactPlugins.stream().map(Artifact::getDescriptor).collect(toSet());

    return pluginDependenciesResolver.resolve(providedArtifactPluginsDescriptors, new ArrayList<>(descriptor.getPlugins()),
                                              false);
  }

  private List<ArtifactPlugin> createArtifactPluginList(MuleDeployableArtifactClassLoader policyClassLoader,
                                                        List<ArtifactPluginDescriptor> plugins) {
    return plugins.stream()
        .filter(artifactPluginDescriptor -> policyClassLoader.getArtifactPluginClassLoaders().stream()
            .anyMatch(artifactClassLoader -> artifactClassLoader
                .getArtifactId()
                .endsWith(artifactPluginDescriptor.getName())))
        .map(artifactPluginDescriptor -> new DefaultArtifactPlugin(getArtifactPluginId(policyClassLoader.getArtifactId(),
                                                                                       artifactPluginDescriptor.getName()),
                                                                   artifactPluginDescriptor, policyClassLoader
                                                                       .getArtifactPluginClassLoaders().stream()
                                                                       .filter(artifactClassLoader -> artifactClassLoader
                                                                           .getArtifactId()
                                                                           .endsWith(artifactPluginDescriptor.getName()))
                                                                       .findFirst().get()))
        .collect(toList());
  }

  /**
   * @param policyName name of the policy that cannot be created
   * @return the error message to indicate policy creation failure
   */
  static String createPolicyTemplateCreationErrorMessage(String policyName) {
    return format("Cannot create policy template '%s'", policyName);
  }
}
