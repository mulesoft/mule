/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal.plugin;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.endsWithIgnoreCase;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.deployment.internal.plugin.ArtifactPluginDescriptorZipLoader.EXTENSION_ZIP;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.classloader.ClassLoaderFilterFactory;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorFactory;

import java.io.File;

public class ArtifactPluginDescriptorFactory implements ArtifactDescriptorFactory<ArtifactPluginDescriptor> {

  public static final String PLUGIN_PROPERTIES = "plugin.properties";
  public static final String PLUGIN_DEPENDENCIES = "plugin.dependencies";

  private final ClassLoaderFilterFactory classLoaderFilterFactory;

  /**
   * Creates a new instance
   * 
   * @param classLoaderFilterFactory creates classloader filters for the created descriptors. Not null.
   */
  public ArtifactPluginDescriptorFactory(ClassLoaderFilterFactory classLoaderFilterFactory) {
    checkArgument(classLoaderFilterFactory != null, "ClassLoaderFilterFactory cannot be null");

    this.classLoaderFilterFactory = classLoaderFilterFactory;
  }

  /**
   * Creates an {@link ArtifactPluginDescriptor} descriptor from a folder or a ZIP file.
   *
   * @param pluginLocation an existing folder, or an existing ZIP file, containing artifact files for a plugin.
   * @return a non null descriptor
   * @throws ArtifactDescriptorCreateException if the factory is not able to create a descriptor from the folder or the ZIP file.
   */
  @Override
  public ArtifactPluginDescriptor create(File pluginLocation) throws ArtifactDescriptorCreateException {
    if (pluginLocation.isDirectory()) {
      return new ArtifactPluginDescriptorFolderLoader(pluginLocation).load();
    } else if (endsWithIgnoreCase(pluginLocation.getName(), EXTENSION_ZIP)) {
      return new ArtifactPluginDescriptorZipLoader(pluginLocation).load();
    }
    throw new ArtifactDescriptorCreateException(format("Plugins are only supported in ZIP or folders, check '%s'",
                                                       pluginLocation.getAbsolutePath()));
  }
}
