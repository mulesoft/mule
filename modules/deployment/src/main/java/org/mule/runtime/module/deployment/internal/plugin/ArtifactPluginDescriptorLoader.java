/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.plugin;

import static org.apache.commons.lang.StringUtils.removeEnd;
import static org.mule.runtime.core.util.FileUtils.unzip;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

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
   * @param unpackDestination the destination to use to unpack the zip file
   * @return the plugin {@code ArtifactPluginDescriptor}
   * @throws IOException if there was a problem trying to read the artifact plugin zip file or using the {@code unpackDestination}
   *         location
   */
  public ArtifactPluginDescriptor load(File pluginZip, File unpackDestination) throws IOException {
    checkArgument(pluginZip != null, "plugin zip cannot be null");
    checkArgument(unpackDestination != null, "unpack destination cannot be null");
    checkArgument(pluginZip.getName().endsWith("zip"),
                  "plugin zip must be a zip file ending with .zip, but the file name was " + pluginZip.getAbsolutePath());

    final String pluginName = removeEnd(pluginZip.getName(), ".zip");
    // must unpack as there's no straightforward way for a ClassLoader to use a jar within another jar/zip
    final File tmpDir = new File(unpackDestination, pluginName);
    unzip(pluginZip, tmpDir);
    return artifactPluginDescriptorFactory.create(tmpDir);
  }

}
