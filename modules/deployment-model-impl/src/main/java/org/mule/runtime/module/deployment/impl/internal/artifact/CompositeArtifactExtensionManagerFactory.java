/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.artifact;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXTENSION_MANAGER;

import static java.util.Optional.empty;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.deployment.model.api.DeployableArtifact;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelDiscoverer;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelLoaderRepository;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginClassLoaderSupplier;
import org.mule.runtime.module.deployment.impl.internal.policy.ArtifactExtensionManagerFactory;
import org.mule.runtime.module.extension.api.manager.ExtensionManagerFactory;
import org.mule.runtime.module.extension.internal.manager.CompositeArtifactExtensionManager;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Creates extension managers for {@link PolicyTemplate} artifacts
 */
public class CompositeArtifactExtensionManagerFactory extends ArtifactExtensionManagerFactory {

  private final DeployableArtifact parentArtifact;

  /**
   * Creates a new factory
   *
   * @param parentArtifact                 application on which the policies are applied. Non null.
   * @param extensionModelLoaderRepository {@link ExtensionModelLoaderRepository} with the available extension loaders. Non null.
   * @param artifactPlugins                artifact plugins deployed inside the artifact. Non null.
   * @param extensionManagerFactory        creates the {@link ExtensionManager} for the artifact. Non null
   */
  public CompositeArtifactExtensionManagerFactory(DeployableArtifact parentArtifact,
                                                  ExtensionModelLoaderRepository extensionModelLoaderRepository,
                                                  List<ArtifactPlugin> artifactPlugins,
                                                  ExtensionManagerFactory extensionManagerFactory) {
    this(parentArtifact, extensionModelLoaderRepository, artifactPlugins, extensionManagerFactory, empty());
  }

  /**
   * Creates a new factory
   *
   * @param parentArtifact                 application on which the policies are applied. Non null.
   * @param extensionModelLoaderRepository {@link ExtensionModelLoaderRepository} with the available extension loaders. Non null.
   * @param artifactPlugins                artifact plugins deployed inside the artifact. Non null.
   * @param extensionManagerFactory        creates the {@link ExtensionManager} for the artifact. Non null
   * @param extModelDiscovererOverride     overrides how the the extension models for plugins in a class loader are calculated.
   */
  public CompositeArtifactExtensionManagerFactory(DeployableArtifact parentArtifact,
                                                  ExtensionModelLoaderRepository extensionModelLoaderRepository,
                                                  List<ArtifactPlugin> artifactPlugins,
                                                  ExtensionManagerFactory extensionManagerFactory,
                                                  Optional<BiFunction<PluginClassLoaderSupplier, ExtensionModelLoaderRepository, ExtensionModelDiscoverer>> extModelDiscovererOverride) {
    super(artifactPlugins, extensionModelLoaderRepository, extensionManagerFactory, extModelDiscovererOverride);

    checkArgument(parentArtifact != null, "application cannot be null");
    this.parentArtifact = parentArtifact;
  }

  @Override
  public ExtensionManager create(MuleContext muleContext) {
    ExtensionManager parentExtensionManager =
        parentArtifact.getArtifactContext().getRegistry().<ExtensionManager>lookupByName(OBJECT_EXTENSION_MANAGER).get();

    ExtensionManager extensionManager = super.create(muleContext, parentExtensionManager.getExtensions());

    return new CompositeArtifactExtensionManager(parentExtensionManager, extensionManager);
  }
}
