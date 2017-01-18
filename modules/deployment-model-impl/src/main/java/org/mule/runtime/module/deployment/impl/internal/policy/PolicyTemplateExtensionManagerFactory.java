/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderRepository;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerFactory;

import java.util.List;

/**
 * Creates extension managers for {@link PolicyTemplate} artifacts
 */
public class PolicyTemplateExtensionManagerFactory extends ArtifactExtensionManagerFactory {

  private final Application application;

  /**
   * Creates a new factory
   *
   * @param application application on which the policies are applied. Non null.
   * @param extensionModelLoaderRepository {@link ExtensionModelLoaderRepository} with the available extension loaders. Non null.
   * @param artifactPlugins artifact plugins deployed inside the artifact. Non null.
   * @param extensionManagerFactory creates the {@link ExtensionManager} for the artifact. Non null
   */
  public PolicyTemplateExtensionManagerFactory(Application application,
                                               ExtensionModelLoaderRepository extensionModelLoaderRepository,
                                               List<ArtifactPlugin> artifactPlugins,
                                               ExtensionManagerFactory extensionManagerFactory) {
    super(artifactPlugins, extensionModelLoaderRepository, extensionManagerFactory);

    checkArgument(application != null, "application cannot be null");
    this.application = application;
  }

  @Override
  public ExtensionManager create(MuleContext muleContext) {
    ExtensionManager policyExtensionManager = super.create(muleContext);

    ExtensionManager applicationExtensionManager = application.getMuleContext().getExtensionManager();

    return new CompositeArtifactExtensionManager(applicationExtensionManager, policyExtensionManager);
  }
}
