/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.coreextension;

import org.mule.api.lifecycle.Lifecycle;
import org.mule.module.launcher.DeploymentServiceAware;
import org.mule.module.launcher.PluginClassLoaderManagerAware;

/**
 * Manages lifecycle and dependency injection for {@link org.mule.MuleCoreExtension}
 */
public interface MuleCoreExtensionManager extends Lifecycle, DeploymentServiceAware, PluginClassLoaderManagerAware
{

}
