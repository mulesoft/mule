/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.plugin;

import static java.io.File.separator;
import static java.lang.String.format;
import static org.apache.commons.lang3.ArrayUtils.toPrimitive;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.core.util.JarUtils.loadFileContentFrom;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_ARTIFACT_FOLDER;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_POM;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.MAVEN;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Provides utility methods to wrk with Maven
 */
public class MavenUtils {

  /**
   * Returns the {@link Model} from a given artifact folder
   * 
   * @param artifactFile file containing the artifact content.
   * @return the {@link Model} from the {@value ArtifactPluginDescriptor#MULE_PLUGIN_POM} file if available
   * @throws ArtifactDescriptorCreateException if the artifact jar does not contain a
   *         {@value ArtifactPluginDescriptor#MULE_PLUGIN_POM} file or the file can' be loaded
   */
  public static Model getPomModelFromJar(File artifactFile) {
    String pomFilePath = MULE_ARTIFACT_FOLDER + separator + MULE_PLUGIN_POM;
    try {
      Optional<Byte[]> pomFileContentOptional = loadFileContentFrom(artifactFile, pomFilePath);
      if (!pomFileContentOptional.isPresent()) {
        throw new ArtifactDescriptorCreateException(format("The identifier '%s' requires the file '%s' within the artifact(error found while reading plugin '%s')",
                                                           MAVEN, pomFilePath,
                                                           artifactFile.getName()));
      }
      MavenXpp3Reader reader = new MavenXpp3Reader();
      return reader.read(new ByteArrayInputStream(toPrimitive(pomFileContentOptional.get())));
    } catch (IOException | XmlPullParserException e) {
      throw new ArtifactDescriptorCreateException(format("There was an issue reading '%s' for the plugin '%s'",
                                                         pomFilePath, artifactFile.getAbsolutePath()),
                                                  e);
    }
  }

  /**
   * Returns the {@link Model} from a given artifact folder
   *
   * @param artifactFolder folder containing the exploded artifact file.
   * @return the {@link Model} from the {@value ArtifactPluginDescriptor#MULE_PLUGIN_POM} file if available
   * @throws ArtifactDescriptorCreateException if the folder does not contain a {@value ArtifactPluginDescriptor#MULE_PLUGIN_POM}
   *         file or the file can' be loaded
   */
  public static Model getPomModelFolder(File artifactFolder) {
    final File mulePluginPom = lookupPomFromMavenLocation(artifactFolder);
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

  private static File lookupPomFromMavenLocation(File artifactFolder) {
    File mulePluginPom = null;
    File lookupFolder = new File(artifactFolder, "META-INF" + File.separator + "maven");
    while (lookupFolder != null && lookupFolder.exists()) {
      File possiblePomLocation = new File(lookupFolder, MULE_PLUGIN_POM);
      if (possiblePomLocation.exists()) {
        mulePluginPom = possiblePomLocation;
        break;
      }
      File[] directories = lookupFolder.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);
      checkState(directories != null || directories.length == 0,
                 format("No directories under %s so pom.xml file for artifact in folder %s could not be find",
                        lookupFolder.getAbsolutePath(), artifactFolder.getAbsolutePath()));
      checkState(directories.length == 1,
                 format("More than one directory under %s so pom.xml file for artifact in folder %s could not be find",
                        lookupFolder.getAbsolutePath(), artifactFolder.getAbsolutePath()));
      lookupFolder = directories[0];
    }

    // final File mulePluginPom = new File(artifactFolder, MULE_ARTIFACT_FOLDER + separator + MULE_PLUGIN_POM);
    if (mulePluginPom == null || !mulePluginPom.exists()) {
      throw new ArtifactDescriptorCreateException(format("The identifier '%s' requires the file '%s' (error found while reading plugin '%s')",
                                                         MAVEN, mulePluginPom.getName(),
                                                         artifactFolder.getName()));
    }
    return mulePluginPom;
  }

}
