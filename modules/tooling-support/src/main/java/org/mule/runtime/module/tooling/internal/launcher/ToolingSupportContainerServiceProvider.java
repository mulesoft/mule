/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.launcher;

import org.mule.runtime.container.api.MuleCoreExtension;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.impl.internal.MuleArtifactResourcesRegistry;
import org.mule.runtime.module.launcher.privileged.ContainerServiceProvider;
import org.mule.runtime.module.tooling.api.ToolingService;
import org.mule.runtime.module.tooling.api.ToolingServiceAware;
import org.mule.runtime.module.tooling.internal.DefaultToolingService;

public class ToolingSupportContainerServiceProvider implements ContainerServiceProvider<ToolingService> {

  @Override
  public Class getServiceInterface() {
    return ToolingService.class;
  }

  @Override
  public ToolingService getServiceImplementation(DeploymentService deploymentService,
                                                 MuleArtifactResourcesRegistry artifactResourcesRegistry) {
    return new DefaultToolingService(artifactResourcesRegistry.getDomainRepository(),
                                     artifactResourcesRegistry.getDomainFactory(),
                                     artifactResourcesRegistry.getApplicationFactory(),
                                     artifactResourcesRegistry.getToolingApplicationDescriptorFactory());
  }

  @Override
  public void inject(MuleCoreExtension extension, ToolingService toolingService) {
    if (extension instanceof ToolingServiceAware tsaExtension) {
      tsaExtension.setToolingService(toolingService);
    }
  }

}
