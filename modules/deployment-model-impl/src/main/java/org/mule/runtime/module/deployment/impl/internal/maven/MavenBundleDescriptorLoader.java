/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.maven;

import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.EXTENSION_BUNDLE_TYPE;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.MAVEN;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.getPomModelFolder;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.getPomModelFromJar;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptorLoader;
import org.mule.runtime.module.artifact.descriptor.InvalidDescriptorLoaderException;

import java.io.File;
import java.util.Map;

import org.apache.maven.model.Model;

/**
 * Loads a {@link BundleDescriptor} using Maven to extract the relevant information from a Mule artifact's
 * {@value ArtifactPluginDescriptor#MULE_PLUGIN_POM} file.
 */
public class MavenBundleDescriptorLoader implements BundleDescriptorLoader {

  @Override
  public String getId() {
    return MAVEN;
  }

  /**
   * Looks for the POM file within the current {@code pluginFolder} structure (under
   * {@link ArtifactPluginDescriptor#MULE_ARTIFACT_FOLDER} folder) to retrieve the plugin artifact locator.
   *
   * @param artifactFile {@link File} with the content of the artifact to work with. Non null
   * @param attributes collection of attributes describing the loader. Non null.
   * @param artifactType the type of the artifact of the descriptor to be loaded.
   * @return a locator of the coordinates of the current plugin
   * @throws ArtifactDescriptorCreateException if the plugin is missing the {@link ArtifactPluginDescriptor#MULE_PLUGIN_POM} or
   *         there's an issue while reading that file
   */
  @Override
  public BundleDescriptor load(File artifactFile, Map<String, Object> attributes, ArtifactType artifactType)
      throws InvalidDescriptorLoaderException {
    Model model;
    if (artifactFile.isDirectory()) {
      model = getPomModelFolder(artifactFile);
    } else {
      model = getPomModelFromJar(artifactFile);
    }

    return new BundleDescriptor.Builder()
        .setArtifactId(model.getArtifactId())
        .setGroupId(model.getGroupId() != null ? model.getGroupId() : model.getParent().getGroupId())
        .setVersion(model.getVersion() != null ? model.getVersion() : model.getParent().getVersion())
        .setType(EXTENSION_BUNDLE_TYPE)
        .setClassifier(MULE_PLUGIN_CLASSIFIER)
        .build();
  }
}
