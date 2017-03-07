/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.plugin;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.util.FileUtils.unzip;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;

import java.io.File;
import java.io.IOException;

/**
 * Loads a {@link ArtifactPluginDescriptor} from a file resource.
 *
 * @since 4.0
 */
public class ArtifactPluginDescriptorLoader {

  private final ArtifactPluginDescriptorFactory artifactPluginDescriptorFactory;

  public ArtifactPluginDescriptorLoader(ArtifactPluginDescriptorFactory artifactPluginDescriptorFactory) {
    this.artifactPluginDescriptorFactory = artifactPluginDescriptorFactory;
  }

  /**
   * Load a {@code ArtifactPluginDescriptor} from a file with the resource of an artifact plugin.
   *
   * @param pluginZip the artifact plugin zip file
   * @return the plugin {@code ArtifactPluginDescriptor}
   * @throws IOException if there was a problem trying to read the artifact plugin zip file or using the {@code unpackDestination}
   *         location
   */
  public ArtifactPluginDescriptor load(File pluginZip) throws IOException {
    //TODO(fernandezlautaro): MULE-11383 all artifacts must be .jar files , when done remove all the code to support zips
    checkArgument(pluginZip != null, "plugin zip cannot be null");
    checkArgument(pluginZip.getName().endsWith("zip") || pluginZip.getName().endsWith("jar"),
                  "plugin zip must be a zip file ending with .zip or .jar, but the file name was " + pluginZip.getAbsolutePath());
    return artifactPluginDescriptorFactory.create(pluginZip);
  }

}
