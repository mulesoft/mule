/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.application;

import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.launcher.plugin.ApplicationPluginDescriptor;

/**
 * Creates an application plugin artifact.
 *
 * @since 4.0
 */
public interface ApplicationPluginFactory
{
    ApplicationPlugin create(ApplicationPluginDescriptor descriptor, ArtifactClassLoader parent);
}
