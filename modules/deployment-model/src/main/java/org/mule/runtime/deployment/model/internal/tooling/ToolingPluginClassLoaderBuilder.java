/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.tooling;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.UUID.getUUID;
import static org.mule.runtime.deployment.model.internal.DefaultRegionPluginClassLoadersFactory.PLUGIN_CLASSLOADER_IDENTIFIER;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.internal.AbstractArtifactClassLoaderBuilder;
import org.mule.runtime.deployment.model.internal.RegionPluginClassLoadersFactory;
import org.mule.runtime.deployment.model.internal.plugin.PluginDependenciesResolver;
import org.mule.runtime.deployment.model.internal.plugin.PluginResolutionError;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.DisposableClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Given an {@link ArtifactPluginDescriptor} as a starting point, it will generate a {@link ArtifactClassLoader} capable of
 * working with the plugin and any other plugins it relies on.
 * <p>
 * So, if we take HTTP as a sample which depends on Sockets, it will (a) generate a {@link ClassLoader} for HTTP, (b) its Socket
 * dependency and (c) a {@link RegionClassLoader} as well.
 * <p>
 * This builder object will return a wrapper to the HTTP {@link ArtifactClassLoader} to allow further consumers of it to believe
 * they have the actual HTTP, allowing them to {@link DisposableClassLoader#dispose()} properly.
 *
 * @since 4.0
 */
public class ToolingPluginClassLoaderBuilder extends AbstractArtifactClassLoaderBuilder<ToolingPluginClassLoaderBuilder> {

  private static final String TOOLING_EXTENSION_MODEL = "tooling-extension-model";
  private final DeployableArtifactClassLoaderFactory artifactClassLoaderFactory;
  private ArtifactPluginDescriptor artifactPluginDescriptor;
  private final PluginDependenciesResolver pluginDependenciesResolver;

  private ArtifactClassLoader parentClassLoader;

  /**
   * {@inheritDoc}
   *
   * @param artifactClassLoaderFactory factory for the classloader specific to the artifact resource and classes
   * @param artifactPluginDescriptor desired plugin to generate an {@link ArtifactClassLoader} for.
   * @param pluginDependenciesResolver resolver for the plugins on which the {@code artifactPluginDescriptor} declares it depends.
   * @param pluginClassLoadersFactory creates the class loaders for the plugins included in the policy's region. Non null
   * @see #build()
   */
  public ToolingPluginClassLoaderBuilder(DeployableArtifactClassLoaderFactory artifactClassLoaderFactory,
                                         PluginDependenciesResolver pluginDependenciesResolver,
                                         ArtifactPluginDescriptor artifactPluginDescriptor,
                                         RegionPluginClassLoadersFactory pluginClassLoadersFactory) {
    super(pluginClassLoadersFactory);
    this.artifactPluginDescriptor = artifactPluginDescriptor;
    this.artifactClassLoaderFactory = artifactClassLoaderFactory;
    this.pluginDependenciesResolver = pluginDependenciesResolver;
  }

  @Override
  protected ArtifactClassLoader createArtifactClassLoader(String artifactId, RegionClassLoader regionClassLoader) {
    return artifactClassLoaderFactory.create(artifactId, regionClassLoader, artifactDescriptor, artifactPluginClassLoaders);
  }

  /**
   * @param parentClassLoader parent class loader for the artifact class loader that should have all the {@link URL}s needed from
   *        tooling side when loading the {@link ExtensionModel}. Among those, there will be mule-api, extensions-api,
   *        extensions-support and so on.
   * @return the builder
   */
  public ToolingPluginClassLoaderBuilder setParentClassLoader(ArtifactClassLoader parentClassLoader) {
    this.parentClassLoader = parentClassLoader;
    return this;
  }

  @Override
  protected ArtifactClassLoader getParentClassLoader() {
    return parentClassLoader;
  }

  @Override
  protected String getArtifactId(ArtifactDescriptor artifactDescriptor) {
    return TOOLING_EXTENSION_MODEL + getUUID() + "/" + artifactDescriptor.getName();
  }

  @Override
  public ToolingArtifactClassLoader build() throws IOException {
    setArtifactDescriptor(new ArtifactDescriptor(TOOLING_EXTENSION_MODEL));
    List<ArtifactPluginDescriptor> resolvedArtifactPluginDescriptors =
        pluginDependenciesResolver
            .resolve(ImmutableList.<ArtifactPluginDescriptor>builder().add(artifactPluginDescriptor).build());
    this.addArtifactPluginDescriptors(resolvedArtifactPluginDescriptors
        .toArray(new ArtifactPluginDescriptor[resolvedArtifactPluginDescriptors.size()]));
    ArtifactClassLoader ownerArtifactClassLoader = super.build();
    ClassLoader parent = ownerArtifactClassLoader.getClassLoader().getParent();
    if (!(parent instanceof RegionClassLoader)) {
      throw new DeploymentException(createStaticMessage(format("The parent of the current owner must be of type '%s' but found '%s'",
                                                               RegionClassLoader.class.getName(), parent.getClass().getName())));
    }
    final RegionClassLoader regionClassLoader = (RegionClassLoader) parent;
    return new ToolingArtifactClassLoader(regionClassLoader,
                                          getPluginArtifactClassLoader(artifactPluginDescriptor,
                                                                       regionClassLoader.getArtifactPluginClassLoaders()));
  }

  /**
   * @param artifactPluginDescriptor to look for within the collection of {@link ArtifactClassLoader}s
   * @param artifactPluginClassLoaders plugins class loaders to look for, at least one of them must contain the {@code pluginDescriptor}.
   * @return the {@link ArtifactClassLoader} that was generated for the {@code artifactPluginDescriptor}
   * @throws PluginResolutionError if the plugin wasn't found in the collection of {@code artifactPluginClassLoaders}
   */
  protected static ArtifactClassLoader getPluginArtifactClassLoader(ArtifactPluginDescriptor artifactPluginDescriptor,
                                                                    List<ArtifactClassLoader> artifactPluginClassLoaders) {
    return artifactPluginClassLoaders.stream()
        .filter(artifactClassLoader -> artifactClassLoader.getArtifactId()
            .endsWith(PLUGIN_CLASSLOADER_IDENTIFIER + artifactPluginDescriptor.getName()))
        .findFirst()
        .orElseThrow(() -> new PluginResolutionError(format("Cannot generate a tooling ClassLoader as the region ClassLoader is missing the plugin '%s'",
                                                            artifactPluginDescriptor.getName())));
  }

}
