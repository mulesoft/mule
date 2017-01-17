/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.plugin;

import static java.io.File.separator;
import static java.lang.String.format;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_ARTIFACT_FOLDER;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_POM;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.MAVEN;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Provides utility methods to wrk with Maven
 */
public class MavenUtils {

  /**
   * Returns the {@link Model} from a given artifact folder
   * 
   * @param artifactFolder folder containing the exploded artifact file.
   * @return the {@link Model} from the {@value ArtifactPluginDescriptor#MULE_PLUGIN_POM} file if available
   * @throws ArtifactDescriptorCreateException if the folder does not contain a {@value ArtifactPluginDescriptor#MULE_PLUGIN_POM}
   *         file or the file can' be loaded
   */
  public static Model getPomModel(File artifactFolder) {
    final File mulePluginPom = new File(artifactFolder, MULE_ARTIFACT_FOLDER + separator + MULE_PLUGIN_POM);
    if (!mulePluginPom.exists()) {
      throw new ArtifactDescriptorCreateException(format("The identifier '%s' requires the file '%s' (error found while reading plugin '%s')",
                                                         MAVEN, mulePluginPom.getName(),
                                                         artifactFolder.getName()));
    }
    MavenXpp3Reader reader = new MavenXpp3Reader();
    Model model;
    try {
      model = reader.read(new FileReader(mulePluginPom));
    } catch (IOException | XmlPullParserException e) {
      throw new ArtifactDescriptorCreateException(format("There was an issue reading '%s' for the plugin '%s'",
                                                         mulePluginPom.getName(), artifactFolder.getAbsolutePath()),
                                                  e);
    }
    return model;
  }
}
