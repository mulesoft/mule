/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.internal.launcher;

import org.mule.runtime.container.api.MuleCoreExtension;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.impl.internal.MuleArtifactResourcesRegistry;
import org.mule.runtime.module.launcher.privileged.ContainerServiceProvider;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingService;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingServiceAware;
import org.mule.runtime.module.troubleshooting.internal.DefaultTroubleshootingService;

public class TroubleshootingContainerServiceProvider implements ContainerServiceProvider<TroubleshootingService> {

  @Override
  public Class<TroubleshootingService> getServiceInterface() {
    return TroubleshootingService.class;
  }

  @Override
  public TroubleshootingService getServiceImplementation(DeploymentService deploymentService,
                                                         MuleArtifactResourcesRegistry artifactResourcesRegistry) {
    return new DefaultTroubleshootingService(deploymentService);
  }

  @Override
  public void inject(MuleCoreExtension extension, TroubleshootingService troubleshootingService) {
    if (extension instanceof TroubleshootingServiceAware tsaExtension) {
      tsaExtension.setTroubleshootingService(troubleshootingService);
    }
  }

}
