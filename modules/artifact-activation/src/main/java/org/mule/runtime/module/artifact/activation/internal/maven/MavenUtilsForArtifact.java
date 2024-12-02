/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.maven;

import static java.io.File.separator;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.filefilter.TrueFileFilter.INSTANCE;

import org.mule.runtime.core.api.util.PropertiesUtils;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleScope;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Properties;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;

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
    File dir = new File(artifactFolder, "META-INF" + separator + "maven");

    if (dir != null && dir.exists()) {
      IOFileFilter fileFilter = new NameFileFilter("pom.properties");
      Collection<File> potentialLocations = listFiles(dir, fileFilter, INSTANCE);
      if (!potentialLocations.isEmpty()) {
        mulePropertiesPom = potentialLocations.stream().findFirst().get();
      }
    }

    if (mulePropertiesPom == null || !mulePropertiesPom.exists()) {
      throw new ArtifactDescriptorCreateException(format("The Maven bundle loader requires the file pom.properties (error found while reading artifact '%s')",
                                                         artifactFolder.getName()));
    }
    return mulePropertiesPom;
  }

  public static BundleDependency mavenToArtifact(org.mule.maven.pom.parser.api.model.BundleDependency d) {
    return BundleDependency.builder()
        .setDescriptor(mavenToArtifact(d.getDescriptor()))
        .setBundleUri(d.getBundleUri())
        .setTransitiveDependencies(d.getTransitiveDependencies()
            .stream()
            .map(MavenUtilsForArtifact::mavenToArtifact)
            .collect(toList()))
        .setScope(BundleScope.valueOf(d.getScope().name()))
        .build();
  }

  public static BundleDescriptor mavenToArtifact(org.mule.maven.pom.parser.api.model.BundleDescriptor d) {
    return BundleDescriptor.builder()
        .setGroupId(d.getGroupId())
        .setArtifactId(d.getArtifactId())
        .setClassifier(d.getClassifier().orElse(null))
        .setType(d.getType())
        .setVersion(d.getVersion())
        .setBaseVersion(d.getVersion())
        .build();
  }

  public static org.mule.maven.pom.parser.api.model.BundleDescriptor artifactToMaven(BundleDescriptor d) {
    return new org.mule.maven.pom.parser.api.model.BundleDescriptor.Builder()
        .setGroupId(d.getGroupId())
        .setArtifactId(d.getArtifactId())
        .setClassifier(d.getClassifier().orElse(null))
        .setType(d.getType())
        .setVersion(d.getVersion())
        .setBaseVersion(d.getVersion())
        .build();
  }

}
