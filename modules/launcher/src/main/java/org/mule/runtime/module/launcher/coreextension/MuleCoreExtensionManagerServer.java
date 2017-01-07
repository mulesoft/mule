/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.coreextension;

import org.mule.runtime.container.api.ArtifactClassLoaderManagerAware;
import org.mule.runtime.container.api.MuleCoreExtension;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.module.deployment.api.DeploymentServiceAware;
import org.mule.runtime.module.repository.api.RepositoryServiceAware;
import org.mule.runtime.module.tooling.api.ToolingServiceAware;

/**
 * Manages lifecycle and dependency injection for {@link MuleCoreExtension}
 */
public interface MuleCoreExtensionManagerServer
    extends Lifecycle, DeploymentServiceAware, RepositoryServiceAware, ToolingServiceAware, ArtifactClassLoaderManagerAware {

}
