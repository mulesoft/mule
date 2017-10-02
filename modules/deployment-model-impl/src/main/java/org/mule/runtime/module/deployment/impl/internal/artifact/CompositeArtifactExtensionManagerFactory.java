/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.artifact;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.deployment.model.api.DeployableArtifact;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;
import org.mule.runtime.module.deployment.impl.internal.policy.ArtifactExtensionManagerFactory;
import org.mule.runtime.module.deployment.impl.internal.policy.CompositeArtifactExtensionManager;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderRepository;
import org.mule.runtime.module.extension.api.manager.ExtensionManagerFactory;

import java.util.List;

/**
 * Creates extension managers for {@link PolicyTemplate} artifacts
 */
public class CompositeArtifactExtensionManagerFactory extends ArtifactExtensionManagerFactory {

  private final DeployableArtifact parentArtifact;

  /**
   * Creates a new factory
   * 
   * @param parentArtifact application on which the policies are applied. Non null.
   * @param extensionModelLoaderRepository {@link ExtensionModelLoaderRepository} with the available extension loaders. Non null.
   * @param artifactPlugins artifact plugins deployed inside the artifact. Non null.
   * @param extensionManagerFactory creates the {@link ExtensionManager} for the artifact. Non null
   */
  public CompositeArtifactExtensionManagerFactory(DeployableArtifact parentArtifact,
                                                  ExtensionModelLoaderRepository extensionModelLoaderRepository,
                                                  List<ArtifactPlugin> artifactPlugins,
                                                  ExtensionManagerFactory extensionManagerFactory) {
    super(artifactPlugins, extensionModelLoaderRepository, extensionManagerFactory);

    checkArgument(parentArtifact != null, "application cannot be null");
    this.parentArtifact = parentArtifact;
  }

  @Override
  public ExtensionManager create(MuleContext muleContext) {
    ExtensionManager policyExtensionManager = super.create(muleContext);

    ExtensionManager applicationExtensionManager =
        parentArtifact.getRegistry().<ExtensionManager>lookupByName(MuleProperties.OBJECT_EXTENSION_MANAGER).get();

    return new CompositeArtifactExtensionManager(applicationExtensionManager, policyExtensionManager);
  }
}
