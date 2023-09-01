/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.core.internal.util.JarUtils.getUrlWithinJar;
import static org.mule.runtime.core.internal.util.JarUtils.getUrlsWithinJar;
import static org.mule.runtime.core.internal.util.jar.JarLoadingUtils.loadFileContentFrom;
import static org.mule.runtime.core.internal.util.jar.JarLoadingUtils.getJarConnection;
import static org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor.MULE_POM;
import static org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor.MULE_POM_PROPERTIES;
import static org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor.META_INF;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor.MULE_ARTIFACT_PATH_INSIDE_JAR;

import static java.io.File.separator;
import static java.lang.String.format;

import static org.apache.commons.io.filefilter.DirectoryFileFilter.DIRECTORY;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.util.PropertiesUtils;
import org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Provides utility methods to wrk with Maven
 */
public class MavenUtils {

  /**
   * Finds the URL of the {@code pom.properties} file within the artifact file.
   *
   * @param artifactFile the artifact file to search for the {@code pom.properties} file.
   * @return the URL to the {@code pom.properties} file.
   */
  public static URL getPomPropertiesUrlFromJar(File artifactFile) {
    return lookMavenMetadataFileUrlFromJar(artifactFile, MULE_POM_PROPERTIES);
  }

  private static URL lookMavenMetadataFileUrlFromJar(File artifactFile, String mavenMetadataFileName) {
    String mavenMetadataFilePath = MULE_ARTIFACT_PATH_INSIDE_JAR + "/" + mavenMetadataFileName;
    URL possibleUrl;
    try {
      possibleUrl = getUrlWithinJar(artifactFile, mavenMetadataFilePath);
      JarURLConnection jarConnection = getJarConnection(possibleUrl);
      jarConnection.setUseCaches(false);
      try (InputStream ignored = jarConnection.getInputStream()) {
        return possibleUrl;
      } catch (Exception e) {
        List<URL> jarMavenUrls = getUrlsWithinJar(artifactFile, META_INF + "/" + "maven");
        Optional<URL> mavenMetadataUrl =
            jarMavenUrls.stream().filter(url -> url.toString().endsWith(mavenMetadataFileName)).findAny();
        if (!mavenMetadataUrl.isPresent()) {
          throw new ArtifactDescriptorCreateException(format("The identifier '%s' requires the file '%s' within the artifact(error found while reading artifact '%s')",
                                                             artifactFile.getName(), mavenMetadataFileName,
                                                             artifactFile.getAbsolutePath()));
        }
        return mavenMetadataUrl.get();
      }
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
  }

  /**
   * Returns the {@link Properties} with the content of {@code pom.properties} from a given artifact folder.
   *
   * @param artifactFolder folder containing the exploded artifact file.
   * @return the {@link Properties} from the {@value DeployableArtifactDescriptor#MULE_POM_PROPERTIES} file if available
   * @throws ArtifactDescriptorCreateException if the folder does not contain a
   *                                           {@value DeployableArtifactDescriptor#MULE_POM_PROPERTIES} file or the file can' be
   *                                           loaded
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
   * Returns the {@link Properties} with the content of {@code pom.properties} from a given artifact jar file.
   *
   * @param artifactFile the artifact file to search for the pom file.
   * @return the {@link Properties} from the {@value DeployableArtifactDescriptor#MULE_POM_PROPERTIES} file if available
   * @throws ArtifactDescriptorCreateException if the folder does not contain a
   *                                           {@value DeployableArtifactDescriptor#MULE_POM_PROPERTIES} file or the file can' be
   *                                           loaded
   */
  public static Properties getPomPropertiesFromJar(File artifactFile) {
    String pomPropertiesFilePath = MULE_ARTIFACT_PATH_INSIDE_JAR + "/" + MULE_POM_PROPERTIES;
    try {
      return PropertiesUtils
          .loadProperties(new ByteArrayInputStream(loadFileContentFrom(getPomPropertiesUrlFromJar(artifactFile)).get()));
    } catch (IOException e) {
      throw new ArtifactDescriptorCreateException(format("There was an issue reading '%s' for the artifact '%s'",
                                                         pomPropertiesFilePath, artifactFile.getAbsolutePath()),
                                                  e);
    }
  }

  public static File lookupPomFromMavenLocation(File artifactFolder) {
    File artifactPomFile = null;
    File lookupFolder = new File(artifactFolder, "META-INF" + separator + "maven");
    while (lookupFolder != null && lookupFolder.exists()) {
      File possiblePomLocation = new File(lookupFolder, MULE_POM);
      if (possiblePomLocation.exists()) {
        artifactPomFile = possiblePomLocation;
        break;
      }
      File[] directories = lookupFolder.listFiles((FileFilter) DIRECTORY);
      checkState(directories != null || directories.length == 0,
                 format("No directories under %s so pom.xml file for artifact in folder %s could not be found",
                        lookupFolder.getAbsolutePath(), artifactFolder.getAbsolutePath()));
      checkState(directories.length == 1,
                 format("More than one directory under %s so pom.xml file for artifact in folder %s could not be found",
                        lookupFolder.getAbsolutePath(), artifactFolder.getAbsolutePath()));
      lookupFolder = directories[0];
    }

    // final File artifactPomFile = new File(artifactFolder, MULE_ARTIFACT_FOLDER + separator + MULE_POM);
    if (artifactPomFile == null || !artifactPomFile.exists()) {
      throw new ArtifactDescriptorCreateException(format("The Maven bundle loader requires the file pom.xml (error found while reading artifact '%s')",
                                                         artifactFolder.getName()));
    }
    return artifactPomFile;
  }

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
