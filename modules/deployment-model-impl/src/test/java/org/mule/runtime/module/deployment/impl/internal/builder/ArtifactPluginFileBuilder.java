/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static java.io.File.separator;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_ARTIFACT_FOLDER;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_JSON;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.PLUGIN_PROPERTIES;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.REPOSITORY;
import static org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorFactory.PLUGIN_DEPENDENCIES;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.deployment.persistence.MulePluginModelJsonSerializer;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.builder.AbstractArtifactFileBuilder;
import org.mule.tck.ZipUtils.ZipResource;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
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
  }

  /**
   * Creates a new builder from another instance.
   *
   * @param source instance used as template to build the new one. Non null.
   */
  public ArtifactPluginFileBuilder(ArtifactPluginFileBuilder source) {
    super(source);
  }

  /**
   * Create a new builder from another instance and different ID.
   *
   * @param id artifact identifier. Non empty.
   * @param source instance used as template to build the new one. Non null.
   */
  public ArtifactPluginFileBuilder(String id, ArtifactPluginFileBuilder source) {
    super(id, source);
    this.properties.putAll(source.properties);
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
   * @param mulePluginModel the describer to store under {@link ArtifactPluginDescriptor#MULE_ARTIFACT_FOLDER}/{@link ArtifactPluginDescriptor#MULE_PLUGIN_JSON} file
   * @return the same builder instance
   */
  public ArtifactPluginFileBuilder describedBy(MulePluginModel mulePluginModel) {
    checkImmutable();
    checkArgument(mulePluginModel != null, "JSON describer cannot be null");
    this.mulePluginModel = mulePluginModel;
    return this;
  }

  /**
   * Adds a resource file to the application classes folder.
   *
   * @param resourceFile resource file from a external file or test resource.
   * @return the same builder instance
   */
  public ArtifactPluginFileBuilder containingResource(String resourceFile, String alias) {
    checkImmutable();
    checkArgument(!isEmpty(resourceFile), "Resource file cannot be empty");
    resources.add(new ZipResource(resourceFile, "classes/" + alias));
    return this;
  }

  /**
   * Adds a resource file to the artifact folder.
   *
   * @param resourceFile resource file from a external file or test resource.
   * @return the same builder instance
   */
  public ArtifactPluginFileBuilder containingRootResource(String resourceFile, String alias) {
    checkImmutable();
    checkArgument(!isEmpty(resourceFile), "Resource file cannot be empty");
    resources.add(new ZipResource(resourceFile, alias));
    return this;
  }

  /**
   * Adds a resource file to the application {@link ArtifactPluginDescriptor#MULE_ARTIFACT_FOLDER} folder.
   *
   * @param resourceFile resource file from a external file or test resource.
   * @return the same builder instance
   */
  public ArtifactPluginFileBuilder containingMuleArtifactResource(String resourceFile, String alias) {
    checkImmutable();
    checkArgument(!isEmpty(resourceFile), "Resource file cannot be empty");
    resources.add(new ZipResource(resourceFile, MULE_ARTIFACT_FOLDER + "/" + alias));
    return this;
  }

  /**
   * Adds a resource file to the application {@link ArtifactPluginDescriptor#MULE_ARTIFACT_FOLDER} folder.
   *
   * @param resourceFile resource file from a external file or test resource.
   * @param alias relative path of the artifact to "install" in the plugin's {@link ArtifactPluginDescriptor#REPOSITORY}
   *              folder, it must respect the Maven repository format. E.g.: /org/mule/modules/mule-module-ble/4.0-SNAPSHOT/mule-module-ble-4.0-SNAPSHOT.jar
   * @return the same builder instance
   */
  public ArtifactPluginFileBuilder containingRepositoryResource(String resourceFile, String alias) {
    checkImmutable();
    checkArgument(!isEmpty(resourceFile), "Resource file cannot be empty");
    resources.add(new ZipResource(resourceFile, REPOSITORY + "/" + alias));
    return this;
  }

  /**
   * Adds a dependency against another plugin
   *
   * @param pluginName name of the plugin to be dependent. Non empty.
   * @return the same builder instance
   */
  public ArtifactPluginFileBuilder dependingOn(String pluginName) {
    checkImmutable();
    checkArgument(!isEmpty(pluginName), "Plugin name cannot be empty");
    String plugins = properties.getProperty(PLUGIN_DEPENDENCIES);
    if (isEmpty(plugins)) {
      plugins = pluginName;
    } else {
      plugins = plugins + ", " + pluginName;
    }

    properties.setProperty(PLUGIN_DEPENDENCIES, plugins);

    return this;
  }

  @Override
  protected List<ZipResource> getCustomResources() {
    final List<ZipResource> customResources = new LinkedList<>();

    if (!properties.isEmpty()) {
      final File applicationPropertiesFile = new File(getTempFolder(), PLUGIN_PROPERTIES);
      applicationPropertiesFile.deleteOnExit();
      createPropertiesFile(applicationPropertiesFile, properties);

      customResources.add(new ZipResource(applicationPropertiesFile.getAbsolutePath(), PLUGIN_PROPERTIES));
    }

    if (mulePluginModel != null) {
      final File jsonDescriptorFile = new File(getTempFolder(), MULE_ARTIFACT_FOLDER + separator + MULE_PLUGIN_JSON);
      jsonDescriptorFile.deleteOnExit();

      String jsonDescriber = new MulePluginModelJsonSerializer().serialize(mulePluginModel);
      try {
        FileUtils.writeStringToFile(jsonDescriptorFile, jsonDescriber);
      } catch (IOException e) {
        throw new IllegalStateException("There was an issue generating the JSON file for " + this.getId(), e);
      }
      customResources.add(new ZipResource(jsonDescriptorFile.getAbsolutePath(), MULE_ARTIFACT_FOLDER + "/" + MULE_PLUGIN_JSON));
    }

    return customResources;
  }

  @Override
  public String getConfigFile() {
    return null;
  }
}
