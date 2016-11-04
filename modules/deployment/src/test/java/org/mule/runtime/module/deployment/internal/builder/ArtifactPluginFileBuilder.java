/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.PLUGIN_PROPERTIES;
import static org.mule.runtime.deployment.model.internal.plugin.descriptor.PropertiesClassloaderDescriptor.PLUGINPROPERTIES;
import static org.mule.runtime.deployment.model.internal.plugin.loader.AbstractPluginDescriptorLoader.PLUGIN_JSON_DESCRIPTOR_FILE;
import static org.mule.runtime.module.deployment.internal.plugin.ArtifactPluginDescriptorFactory.PLUGIN_DEPENDENCIES;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.module.artifact.builder.AbstractArtifactFileBuilder;
import org.mule.tck.ZipUtils.ZipResource;

import com.google.gson.Gson;

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
      customResources.add(createMulePluginJsonResource());
    }

    return customResources;
  }

  @Override
  public String getConfigFile() {
    return null;
  }

  /**
   * When generating a plugin for testing with plugin.properties, it must also have the mule-plugin.json file in it so that
   * the descriptors are properly initialized
   * @return a resource that represents the mule-plugin.json
     */
  private ZipResource createMulePluginJsonResource() {
    final File mulePluginJsonFile =
        new File(getTempFolder(), PLUGIN_JSON_DESCRIPTOR_FILE.getPath());
    mulePluginJsonFile.deleteOnExit();

    MulePluginJsonFileBuilder.ClassloaderDescriptor classloaderDescriptor =
        new MulePluginJsonFileBuilder.ClassloaderDescriptor(PLUGINPROPERTIES);
    MulePluginJsonFileBuilder mulePluginJsonFileBuilder =
        new MulePluginJsonFileBuilder(this.getId(), "4.0.0", classloaderDescriptor);
    String jsonContent = new Gson().toJson(mulePluginJsonFileBuilder);
    try {
      FileUtils.writeStringToFile(mulePluginJsonFile, jsonContent);
    } catch (IOException e) {
      throw new IllegalStateException(String.format("there was an issue generating the %s file",
                                                    PLUGIN_JSON_DESCRIPTOR_FILE.getPath()));
    }
    return new ZipResource(mulePluginJsonFile.getAbsolutePath(),
                           PLUGIN_JSON_DESCRIPTOR_FILE.getPath());
  }

  private static final class MulePluginJsonFileBuilder {

    //TODO MULE-10875 this class should be provided in mule-module-artifact, as well as a builder for it so it can be serialized in one place
    public final String name;
    public final String minMuleVersion;
    public final ClassloaderDescriptor classloaderDescriptor;

    public MulePluginJsonFileBuilder(String name, String minMuleVersion, ClassloaderDescriptor classloaderDescriptor) {
      this.name = name;
      this.minMuleVersion = minMuleVersion;
      this.classloaderDescriptor = classloaderDescriptor;
    }

    private static final class ClassloaderDescriptor {

      public final String id;

      public ClassloaderDescriptor(String id) {
        this.id = id;
      }
    }
  }
}
