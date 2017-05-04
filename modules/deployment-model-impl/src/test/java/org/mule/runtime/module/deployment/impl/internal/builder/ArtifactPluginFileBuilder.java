/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static java.io.File.separator;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_RESOURCE_PROPERTY;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_ARTIFACT_FOLDER;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_ARTIFACT_PATH_INSIDE_JAR;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_JSON;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.MAVEN;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptorBuilder;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.deployment.persistence.MulePluginModelJsonSerializer;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.builder.AbstractArtifactFileBuilder;
import org.mule.tck.ZipUtils.ZipResource;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Creates Mule Application Plugin files.
 */
public class ArtifactPluginFileBuilder extends AbstractArtifactFileBuilder<ArtifactPluginFileBuilder> {

  private Properties properties = new Properties();
  private MulePluginModel mulePluginModel;

  /**
   * Creates a new builder
   *
   * @param id artifact identifier. Non empty.
   */
  public ArtifactPluginFileBuilder(String id) {
    super(id);
    withClassifier(MULE_PLUGIN_CLASSIFIER);
  }

  @Override
  protected ArtifactPluginFileBuilder getThis() {
    return this;
  }

  /**
   * Adds a property into the plugin properties file.
   *
   * @param propertyName name fo the property to add. Non empty
   * @param propertyValue value of the property to add. Non null.
   * @return the same builder instance
   */
  public ArtifactPluginFileBuilder configuredWith(String propertyName, String propertyValue) {
    checkImmutable();
    checkArgument(!isEmpty(propertyName), "Property name cannot be empty");
    checkArgument(propertyValue != null, "Property value cannot be null");
    properties.put(propertyName, propertyValue);
    return this;
  }

  /**
   * Adds a describer into the plugin describer file.
   *
   * @param mulePluginModel the describer to store under
   *        {@link ArtifactPluginDescriptor#MULE_ARTIFACT_FOLDER}/{@link ArtifactPluginDescriptor#MULE_PLUGIN_JSON} file
   * @return the same builder instance
   */
  public ArtifactPluginFileBuilder describedBy(MulePluginModel mulePluginModel) {
    checkImmutable();
    checkArgument(mulePluginModel != null, "JSON describer cannot be null");
    this.mulePluginModel = mulePluginModel;
    return this;
  }

  /**
   * Adds a resource file to the plugin root folder.
   *
   * @param resourceFile resource file from a external file or test resource.
   * @return the same builder instance
   */
  public ArtifactPluginFileBuilder containingResource(String resourceFile, String alias) {
    checkImmutable();
    checkArgument(!isEmpty(resourceFile), "Resource file cannot be empty");
    resources.add(new ZipResource(resourceFile, alias));
    return this;
  }

  /**
   * Adds a class file to the artifact classes folder.
   *
   * @param classFile class file to include. Non null.
   * @param alias path where the file must be added inside the app file
   * @return the same builder instance
   */
  @Override
  public ArtifactPluginFileBuilder containingClass(File classFile, String alias) {
    checkImmutable();
    checkArgument(classFile != null, "Class file cannot be null");
    resources.add(new ZipResource(classFile.getAbsolutePath(), alias));
    return getThis();
  }

  @Override
  protected List<ZipResource> getCustomResources() {
    final List<ZipResource> customResources = new LinkedList<>();

    if (!properties.isEmpty()) {

      final MulePluginModel.MulePluginModelBuilder builder = new MulePluginModel.MulePluginModelBuilder();
      builder.setName(getArtifactId())
          .setMinMuleVersion("4.0.0");
      MuleArtifactLoaderDescriptorBuilder classLoaderModelDescriber = builder.withClassLoaderModelDescriber()
          .setId(MAVEN);
      if (properties.containsKey(EXPORTED_CLASS_PACKAGES_PROPERTY)) {
        classLoaderModelDescriber.addProperty(EXPORTED_PACKAGES,
                                              ((String) properties.get(EXPORTED_CLASS_PACKAGES_PROPERTY)).split(","));
      }
      if (properties.containsKey(EXPORTED_RESOURCE_PROPERTY)) {
        classLoaderModelDescriber.addProperty(EXPORTED_RESOURCES,
                                              ((String) properties.get(EXPORTED_RESOURCE_PROPERTY)).split(","));
      }

      builder.withBundleDescriptorLoader(new MuleArtifactLoaderDescriptor(MAVEN, emptyMap()));

      mulePluginModel = builder.build();
    }

    if (mulePluginModel != null) {
      final File jsonDescriptorFile = new File(getTempFolder(), MULE_ARTIFACT_FOLDER + separator + MULE_PLUGIN_JSON);
      jsonDescriptorFile.deleteOnExit();

      String jsonDescriber = new MulePluginModelJsonSerializer().serialize(mulePluginModel);
      try {
        writeStringToFile(jsonDescriptorFile, jsonDescriber);
      } catch (IOException e) {
        throw new IllegalStateException("There was an issue generating the JSON file for " + this.getId(), e);
      }
      customResources
          .add(new ZipResource(jsonDescriptorFile.getAbsolutePath(), MULE_ARTIFACT_PATH_INSIDE_JAR + "/" + MULE_PLUGIN_JSON));
    }

    return customResources;
  }

  @Override
  public String getConfigFile() {
    return null;
  }

  @Override
  protected String getFileExtension() {
    return ".jar";
  }

  @Override
  protected Optional<MuleArtifactLoaderDescriptor> getBundleDescriptorLoader() {
    return ofNullable(mulePluginModel != null ? mulePluginModel.getBundleDescriptorLoader() : null);
  }
}
