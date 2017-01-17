/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.plugin;

import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.EXTENSION_BUNDLE_TYPE;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.deployment.impl.internal.plugin.MavenUtils.getPomModel;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptor;

import java.io.File;

import org.apache.maven.model.Model;

/**
 * Loads a {@link BundleDescriptor} using Maven to extract the relevant information from a Mule artifact's
 * {@value ArtifactPluginDescriptor#MULE_PLUGIN_POM} file.
 */
public class MavenBundleDescriptorLoader {

  // TODO(pablo.kraan): MULE-11340: Add BundleDescriptorLoader and ClassLoaderModelDescriptorLoader interfaces
  /**
   * Looks for the POM file within the current {@code pluginFolder} structure (under {@link ArtifactPluginDescriptor#MULE_ARTIFACT_FOLDER} folder)
   * to retrieve the plugin artifact locator.
   *
   * @param pluginFolder {@link File} where the current plugin to work with.
   * @return a locator of the coordinates of the current plugin
   * @throws ArtifactDescriptorCreateException if the plugin is missing the {@link ArtifactPluginDescriptor#MULE_PLUGIN_POM} or
   * there's an issue while reading that file
   */
  public BundleDescriptor loadBundleDescriptor(File pluginFolder) {
    Model model = getPomModel(pluginFolder);

    return new BundleDescriptor.Builder()
        .setArtifactId(model.getArtifactId())
        .setGroupId(model.getGroupId())
        .setVersion(model.getVersion() != null ? model.getVersion() : model.getParent().getVersion())
        .setType(EXTENSION_BUNDLE_TYPE)
        .setClassifier(MULE_PLUGIN_CLASSIFIER)
        .build();
  }
}
