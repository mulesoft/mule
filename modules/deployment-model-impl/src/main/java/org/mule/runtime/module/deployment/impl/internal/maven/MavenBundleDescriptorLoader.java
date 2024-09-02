/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import static org.mule.maven.pom.parser.api.MavenPomParserProvider.discoverProvider;
import static org.mule.runtime.api.artifact.ArtifactType.APP;
import static org.mule.runtime.api.artifact.ArtifactType.DOMAIN;
import static org.mule.runtime.api.artifact.ArtifactType.PLUGIN;
import static org.mule.runtime.api.artifact.ArtifactType.POLICY;
import static org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor.MULE_APPLICATION_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor.MULE_DOMAIN_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.deployment.impl.internal.maven.AbstractMavenClassLoaderConfigurationLoader.CLASSLOADER_MODEL_JSON_DESCRIPTOR;
import static org.mule.runtime.module.deployment.impl.internal.maven.AbstractMavenClassLoaderConfigurationLoader.CLASSLOADER_MODEL_JSON_DESCRIPTOR_LOCATION;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.getPomPropertiesFolder;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.getPomPropertiesFromJar;
import static org.mule.tools.api.classloader.ClassLoaderModelJsonSerializer.deserialize;

import static java.util.Collections.singletonMap;

import org.mule.maven.pom.parser.api.model.MavenPomModel;
import org.mule.maven.pom.parser.api.model.PomParentCoordinates;
import org.mule.runtime.api.artifact.ArtifactType;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorLoader;
import org.mule.runtime.module.artifact.api.descriptor.InvalidDescriptorLoaderException;
import org.mule.runtime.module.deployment.impl.internal.plugin.PluginExtendedBundleDescriptorAttributes;
import org.mule.tools.api.classloader.AppClassLoaderModelJsonSerializer;

import java.io.File;
import java.util.Map;
import java.util.Properties;

/**
 * Loads a {@link BundleDescriptor} using Maven to extract the relevant information from a Mule artifact's
 * {@value ArtifactPluginDescriptor#MULE_PLUGIN_POM} file.
 */
public class MavenBundleDescriptorLoader implements BundleDescriptorLoader {

  private static final String JAR = "jar";

  @Override
  public String getId() {
    return MULE_LOADER_ID;
  }

  /**
   * Looks for the POM file within the current {@code pluginFolder} structure (under
   * {@link ArtifactPluginDescriptor#MULE_ARTIFACT_FOLDER} folder) to retrieve the plugin artifact locator.
   *
   * @param artifactFile {@link File} with the content of the artifact to work with. Non null
   * @param attributes   collection of attributes describing the loader. Non null.
   * @param artifactType the type of the artifact of the descriptor to be loaded.
   * @return a locator of the coordinates of the current plugin
   * @throws ArtifactDescriptorCreateException if the plugin is missing the {@link ArtifactPluginDescriptor#MULE_PLUGIN_POM} or
   *                                           there's an issue while reading that file
   */
  @Override
  public BundleDescriptor load(File artifactFile, Map<String, Object> attributes, ArtifactType artifactType)
      throws InvalidDescriptorLoaderException {
    File classLoaderModelDescriptor = getClassLoaderModelDescriptor(artifactFile);
    // if is heavyweight
    if (classLoaderModelDescriptor.exists()) {
      org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel;
      if (APP.equals(artifactType) || DOMAIN.equals(artifactType) || POLICY.equals(artifactType)) {
        packagerClassLoaderModel = AppClassLoaderModelJsonSerializer.deserialize(classLoaderModelDescriptor);
      } else {
        packagerClassLoaderModel = deserialize(classLoaderModelDescriptor);
      }

      return new BundleDescriptor.Builder()
          .setArtifactId(packagerClassLoaderModel.getArtifactCoordinates().getArtifactId())
          .setGroupId(packagerClassLoaderModel.getArtifactCoordinates().getGroupId())
          .setVersion(packagerClassLoaderModel.getArtifactCoordinates().getVersion())
          .setBaseVersion(packagerClassLoaderModel.getArtifactCoordinates().getVersion())
          .setType(packagerClassLoaderModel.getArtifactCoordinates().getType())
          .setClassifier(packagerClassLoaderModel.getArtifactCoordinates().getClassifier())
          .setMetadata(singletonMap(org.mule.tools.api.classloader.model.ClassLoaderModel.class.getName(),
                                    packagerClassLoaderModel))
          .build();
    } else {
      if (attributes instanceof PluginExtendedBundleDescriptorAttributes) {
        return ((PluginExtendedBundleDescriptorAttributes) attributes).getPluginBundleDescriptor();
      }
      return getBundleDescriptor(artifactFile, artifactType);
    }
  }

  private BundleDescriptor getBundleDescriptor(File artifactFile, ArtifactType artifactType) {
    BundleDescriptor.Builder builder = new BundleDescriptor.Builder();

    if (artifactType.equals(APP) || artifactType.equals(DOMAIN)) {
      Properties pomProperties;
      if (artifactFile.isDirectory()) {
        pomProperties = getPomPropertiesFolder(artifactFile);
      } else {
        pomProperties = getPomPropertiesFromJar(artifactFile);
      }
      String version = pomProperties.getProperty("version");
      return builder.setGroupId(pomProperties.getProperty("groupId"))
          .setArtifactId(pomProperties.getProperty("artifactId"))
          .setVersion(version)
          .setBaseVersion(version)
          .setClassifier(artifactType.equals(APP) ? MULE_APPLICATION_CLASSIFIER : MULE_DOMAIN_CLASSIFIER)
          .build();
    } else {

      MavenPomModel model = discoverProvider().createMavenPomParserClient(artifactFile.toPath()).getModel();

      String version =
          model.getVersion() != null ? model.getVersion() : model.getParent().map(PomParentCoordinates::getVersion).orElse(null);
      return new BundleDescriptor.Builder()
          .setArtifactId(model.getArtifactId())
          .setGroupId(model.getGroupId() != null ? model.getGroupId()
              : model.getParent().map(PomParentCoordinates::getGroupId).orElse(null))
          .setVersion(version)
          .setBaseVersion(version)
          .setType(JAR)
          // Handle manually the packaging for mule plugin as the mule plugin maven plugin defines the packaging as mule-extension
          .setClassifier(artifactType.equals(PLUGIN) ? MULE_PLUGIN_CLASSIFIER : model.getPackaging())
          .build();
    }
  }

  protected File getClassLoaderModelDescriptor(File artifactFile) {
    if (artifactFile.isDirectory()) {
      return new File(artifactFile, CLASSLOADER_MODEL_JSON_DESCRIPTOR_LOCATION);
    } else {
      return new File(artifactFile.getParent(), CLASSLOADER_MODEL_JSON_DESCRIPTOR);
    }
  }
}
