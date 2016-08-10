/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.coreextension;

import org.mule.runtime.container.api.MuleCoreExtension;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.module.launcher.DeploymentServiceAware;
import org.mule.runtime.module.launcher.RepositoryServiceAware;
import org.mule.runtime.module.launcher.ToolingServiceAware;

/**
 * Manages lifecycle and dependency injection for {@link MuleCoreExtension}
 */
public interface MuleCoreExtensionManagerServer extends Lifecycle, DeploymentServiceAware, RepositoryServiceAware, ToolingServiceAware
{

}
