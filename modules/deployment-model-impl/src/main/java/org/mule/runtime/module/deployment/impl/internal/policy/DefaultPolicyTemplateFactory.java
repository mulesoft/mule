/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.deployment.model.internal.AbstractArtifactClassLoaderBuilder.getArtifactPluginId;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.NULL_CLASSLOADER_FILTER;
import static org.mule.runtime.module.artifact.descriptor.BundleDescriptorUtils.isCompatibleVersion;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.deployment.model.internal.plugin.PluginDependenciesResolver;
import org.mule.runtime.module.artifact.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptor;
import org.mule.runtime.module.deployment.impl.internal.plugin.DefaultArtifactPlugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Creates {@link DefaultPolicyTemplate} instances.
 */
public class DefaultPolicyTemplateFactory implements PolicyTemplateFactory {

  private final PolicyTemplateClassLoaderBuilderFactory policyTemplateClassLoaderBuilderFactory;
  private final PluginDependenciesResolver pluginDependenciesResolver;

  /**
   * Creates a new factory
   *
   * @param policyTemplateClassLoaderBuilderFactory creates class loader builders to create the class loaders for the created
   *        policy templates. Non null.
   */
  public DefaultPolicyTemplateFactory(PolicyTemplateClassLoaderBuilderFactory policyTemplateClassLoaderBuilderFactory,
                                      PluginDependenciesResolver pluginDependenciesResolver) {
    checkArgument(policyTemplateClassLoaderBuilderFactory != null, "policyTemplateClassLoaderBuilderFactory cannot be null");

    this.policyTemplateClassLoaderBuilderFactory = policyTemplateClassLoaderBuilderFactory;
    this.pluginDependenciesResolver = pluginDependenciesResolver;
  }

  @Override
  public PolicyTemplate createArtifact(Application application, PolicyTemplateDescriptor descriptor) {
    MuleDeployableArtifactClassLoader policyClassLoader;
    Set<ArtifactPluginDescriptor> resolveArtifactPluginDescriptors = pluginDependenciesResolver.resolve(descriptor.getPlugins());
    ArtifactPluginDescriptor[] artifactPluginDescriptors =
        getArtifactPluginDescriptors(resolveArtifactPluginDescriptors, descriptor);
    try {
      policyClassLoader = policyTemplateClassLoaderBuilderFactory.createArtifactClassLoaderBuilder()
          .addArtifactPluginDescriptors(artifactPluginDescriptors)
          .setParentClassLoader(application.getRegionClassLoader()).setArtifactDescriptor(descriptor).build();
    } catch (IOException e) {
      throw new PolicyTemplateCreationException(createPolicyTemplateCreationErrorMessage(descriptor.getName()), e);
    }
    application.getRegionClassLoader().addClassLoader(policyClassLoader, NULL_CLASSLOADER_FILTER);

    List<ArtifactPlugin> artifactPlugins = createArtifactPluginList(policyClassLoader, Arrays.asList(artifactPluginDescriptors));

    DefaultPolicyTemplate policy =
        new DefaultPolicyTemplate(policyClassLoader.getArtifactId(), descriptor, policyClassLoader, artifactPlugins);

    return policy;
  }

  private ArtifactPluginDescriptor[] getArtifactPluginDescriptors(Set<ArtifactPluginDescriptor> appPlugins,
                                                                  PolicyTemplateDescriptor descriptor) {
    List<ArtifactPluginDescriptor> artifactPluginDescriptors = new ArrayList<>();
    for (ArtifactPluginDescriptor policyPluginDescriptor : descriptor.getPlugins()) {
      Optional<ArtifactPluginDescriptor> appPluginDescriptor =
          findPlugin(appPlugins, policyPluginDescriptor.getBundleDescriptor());

      if (!appPluginDescriptor.isPresent()) {
        artifactPluginDescriptors.add(policyPluginDescriptor);
      } else if (!isCompatibleVersion(appPluginDescriptor.get().getBundleDescriptor().getVersion(),
                                      policyPluginDescriptor.getBundleDescriptor().getVersion())) {
        throw new IllegalStateException(
                                        format("Incompatible version of plugin '%s' found. Policy requires version'%s' but application provides version'%s'",
                                               policyPluginDescriptor.getName(),
                                               policyPluginDescriptor.getBundleDescriptor().getVersion(),
                                               appPluginDescriptor.get().getBundleDescriptor().getVersion()));
      }
    }

    return artifactPluginDescriptors.toArray(new ArtifactPluginDescriptor[0]);
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

  private List<ArtifactPlugin> createArtifactPluginList(MuleDeployableArtifactClassLoader policyClassLoader,
                                                        List<ArtifactPluginDescriptor> plugins) {
    return plugins.stream()
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
