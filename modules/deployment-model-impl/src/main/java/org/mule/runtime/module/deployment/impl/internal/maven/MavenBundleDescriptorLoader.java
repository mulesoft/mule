/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.maven;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.PLUGIN;
import static org.mule.runtime.deployment.model.api.application.ApplicationDescriptor.MULE_APPLICATION_CLASSIFIER;
import static org.mule.runtime.deployment.model.api.application.ApplicationDescriptor.MULE_DOMAIN_CLASSIFIER;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.deployment.impl.internal.maven.AbstractMavenClassLoaderModelLoader.CLASSLOADER_MODEL_JSON_DESCRIPTOR;
import static org.mule.runtime.module.deployment.impl.internal.maven.AbstractMavenClassLoaderModelLoader.CLASSLOADER_MODEL_JSON_DESCRIPTOR_LOCATION;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.getPomModelFolder;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.getPomModelFromJar;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.getPomPropertiesFolder;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.getPomPropertiesFromJar;
import static org.mule.tools.api.classloader.ClassLoaderModelJsonSerializer.deserialize;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorLoader;
import org.mule.runtime.module.artifact.api.descriptor.InvalidDescriptorLoaderException;
import org.mule.runtime.module.deployment.impl.internal.plugin.PluginExtendedBundleDescriptorAttributes;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.model.Model;

/**
 * Loads a {@link BundleDescriptor} using Maven to extract the relevant information from a Mule artifact's
 * {@value ArtifactPluginDescriptor#MULE_PLUGIN_POM} file.
 */
public class MavenBundleDescriptorLoader implements BundleDescriptorLoader {

  private static final String JAR = "jar";
  public static final String OVERRIDE_ARTIFACT_ID_KEY = "override.artifactId";

  @Override
  public String getId() {
    return MULE_LOADER_ID;
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
    if (isHeavyPackage(artifactFile)) {
      File classLoaderModelDescriptor = getClassLoaderModelDescriptor(artifactFile);

      org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel = deserialize(classLoaderModelDescriptor);

      String artifactId = overrideArtifactId(attributes, packagerClassLoaderModel.getArtifactCoordinates().getArtifactId());
      return new BundleDescriptor.Builder()
          .setArtifactId(artifactId)
          .setGroupId(packagerClassLoaderModel.getArtifactCoordinates().getGroupId())
          .setVersion(packagerClassLoaderModel.getArtifactCoordinates().getVersion())
          .setBaseVersion(packagerClassLoaderModel.getArtifactCoordinates().getVersion())
          .setType(packagerClassLoaderModel.getArtifactCoordinates().getType())
          .setClassifier(packagerClassLoaderModel.getArtifactCoordinates().getClassifier())
          .build();
    } else {
      if (attributes instanceof PluginExtendedBundleDescriptorAttributes) {
        return ((PluginExtendedBundleDescriptorAttributes) attributes).getPluginBundleDescriptor();
      }
      return getBundleDescriptor(artifactFile, artifactType, attributes);
    }
  }

  private String overrideArtifactId(Map<String, Object> attributes, String configuredValue) {
    if (attributes.containsKey(OVERRIDE_ARTIFACT_ID_KEY)) {
      return String.valueOf(attributes.get(OVERRIDE_ARTIFACT_ID_KEY));
    } else {
      return configuredValue;
    }
  }

  private BundleDescriptor getBundleDescriptor(File artifactFile, ArtifactType artifactType, Map<String, Object> attributes) {
    BundleDescriptor.Builder builder = new BundleDescriptor.Builder();

    if (artifactType.equals(APP) || artifactType.equals(DOMAIN)) {
      Properties pomProperties;
      if (artifactFile.isDirectory()) {
        pomProperties = getPomPropertiesFolder(artifactFile);
      } else {
        pomProperties = getPomPropertiesFromJar(artifactFile);
      }

      String artifactId = overrideArtifactId(attributes, pomProperties.getProperty("artifactId"));
      return builder.setGroupId(pomProperties.getProperty("groupId"))
          .setArtifactId(artifactId)
          .setVersion(pomProperties.getProperty("version"))
          .setClassifier(artifactType.equals(APP) ? MULE_APPLICATION_CLASSIFIER : MULE_DOMAIN_CLASSIFIER)
          .build();
    } else {
      Model model;
      if (artifactFile.isDirectory()) {
        model = getPomModelFolder(artifactFile);
      } else {
        model = getPomModelFromJar(artifactFile);
      }

      String artifactId = overrideArtifactId(attributes, model.getArtifactId());
      return new BundleDescriptor.Builder()
          .setArtifactId(artifactId)
          .setGroupId(model.getGroupId() != null ? model.getGroupId() : model.getParent().getGroupId())
          .setVersion(model.getVersion() != null ? model.getVersion() : model.getParent().getVersion())
          .setType(JAR)
          // Handle manually the packaging for mule plugin as the mule plugin maven plugin defines the packaging as mule-extension
          .setClassifier(artifactType.equals(PLUGIN) ? MULE_PLUGIN_CLASSIFIER : model.getPackaging())
          .build();
    }
  }

  private boolean isHeavyPackage(File artifactFile) {
    return getClassLoaderModelDescriptor(artifactFile).exists();
  }

  protected File getClassLoaderModelDescriptor(File artifactFile) {
    if (artifactFile.isDirectory()) {
      return new File(artifactFile, CLASSLOADER_MODEL_JSON_DESCRIPTOR_LOCATION);
    } else {
      return new File(artifactFile.getParent(), CLASSLOADER_MODEL_JSON_DESCRIPTOR);
    }
  }
}
