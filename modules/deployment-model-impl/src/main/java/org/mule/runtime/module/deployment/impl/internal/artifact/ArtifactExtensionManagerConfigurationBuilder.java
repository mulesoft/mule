/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.artifact;

import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.module.extension.api.manager.ExtensionManagerFactory;

import java.util.List;

/**
 * Implementation of {@link ConfigurationBuilder} that registers a {@link ExtensionManager}
 *
 * @since 4.0
 */
public class ArtifactExtensionManagerConfigurationBuilder extends AbstractConfigurationBuilder {

  public static final String META_INF_FOLDER = "META-INF";

  private final List<ArtifactPlugin> artifactPlugins;
  private final ExtensionManagerFactory extensionManagerFactory;

  /**
   * Creates an instance of the configuration builder.
   *
   * @param artifactPlugins {@link List} of {@link ArtifactPlugin ArtifactPlugins} to be registered.
   * @param extensionManagerFactory creates the extension manager for this artifact. Non null.
   */
  public ArtifactExtensionManagerConfigurationBuilder(List<ArtifactPlugin> artifactPlugins,
                                                      ExtensionManagerFactory extensionManagerFactory) {
    checkNotNull(artifactPlugins, "artifactPlugins cannot be null");
    checkNotNull(extensionManagerFactory, "extensionManagerFactory cannot be null");

    this.artifactPlugins = artifactPlugins;
    this.extensionManagerFactory = extensionManagerFactory;
  }

  @Override
  protected void doConfigure(MuleContext muleContext) throws Exception {
    ExtensionManager extensionManager = extensionManagerFactory.create(muleContext);

    muleContext.setExtensionManager(extensionManager);
  }
}
