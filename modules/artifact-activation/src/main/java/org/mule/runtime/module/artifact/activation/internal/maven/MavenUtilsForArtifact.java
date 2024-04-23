/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.maven;

import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor.MULE_POM_PROPERTIES;

import static java.io.File.separator;
import static java.lang.String.format;

import static org.apache.commons.io.filefilter.DirectoryFileFilter.DIRECTORY;

import org.mule.runtime.core.api.util.PropertiesUtils;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Provides utility methods for artifact-activation module to work with Maven
 */
public class MavenUtilsForArtifact {

  /**
   * Returns the {@link Properties} with the content of {@code pom.properties} from a given artifact folder.
   *
   * @param artifactFolder folder containing the exploded artifact file.
   */
  public static Properties getPomPropertiesFolder(File artifactFolder) {
    final File artifactPomProperties = lookupPomPropertiesMavenLocation(artifactFolder);
    Properties properties;
    try (InputStream artifactPomPropertiesInputStream = new FileInputStream(artifactPomProperties)) {
      properties = PropertiesUtils.loadProperties(artifactPomPropertiesInputStream);
    } catch (IOException e) {
      throw new ArtifactDescriptorCreateException(format("There was an issue reading '%s' for the artifact '%s'",
                                                         artifactPomProperties.getName(), artifactFolder.getAbsolutePath()),
                                                  e);
    }
    return properties;
  }

  /**
   * Look up pom properties.
   */
  public static File lookupPomPropertiesMavenLocation(File artifactFolder) {
    File mulePropertiesPom = null;
    File lookupFolder = new File(artifactFolder, "META-INF" + separator + "maven");
    while (lookupFolder != null && lookupFolder.exists()) {
      File possiblePomPropertiesLocation = new File(lookupFolder, MULE_POM_PROPERTIES);
      if (possiblePomPropertiesLocation.exists()) {
        mulePropertiesPom = possiblePomPropertiesLocation;
        break;
      }
      File[] directories = lookupFolder.listFiles((FileFilter) DIRECTORY);
      checkState(directories != null || directories.length == 0,
                 format("No directories under %s so pom.properties file for artifact in folder %s could not be found",
                        lookupFolder.getAbsolutePath(), artifactFolder.getAbsolutePath()));
      checkState(directories.length == 1,
                 format("More than one directory under %s so pom.properties file for artifact in folder %s could not be found",
                        lookupFolder.getAbsolutePath(), artifactFolder.getAbsolutePath()));
      lookupFolder = directories[0];
    }

    // final File mulePropertiesPom = new File(artifactFolder, MULE_ARTIFACT_FOLDER + separator + MULE_PLUGIN_POM);
    if (mulePropertiesPom == null || !mulePropertiesPom.exists()) {
      throw new ArtifactDescriptorCreateException(format("The Maven bundle loader requires the file pom.properties (error found while reading artifact '%s')",
                                                         artifactFolder.getName()));
    }
    return mulePropertiesPom;
  }
}
