/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin.descriptor;

import static java.lang.String.format;
import org.mule.runtime.deployment.model.api.plugin.dependency.ArtifactDependency;
import org.mule.runtime.deployment.model.api.plugin.classloadermodel.ClassloaderModel;
import org.mule.runtime.deployment.model.api.plugin.classloadermodel.MalformedClassloaderModelException;
import org.mule.runtime.deployment.model.api.plugin.descriptor.ClassloaderDescriptor;
import org.mule.runtime.deployment.model.internal.plugin.dependency.DefaultArtifactDependency;
import org.mule.runtime.deployment.model.internal.plugin.classloadermodel.DefaultClassloaderModel;
import org.mule.runtime.deployment.model.internal.plugin.descriptor.resource.URLPluginResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents an interpreter for the plugin.properties scenario where the plugin has exported packages and exported
 * resources and dependencies in the plugin.properties file (attributes is an empty map)
 *
 * @since 4.0
 */
public class PropertiesClassloaderDescriptor implements ClassloaderDescriptor {

  public static final String PLUGINPROPERTIES = "pluginproperties";

  @Override
  public String getId() {
    return PLUGINPROPERTIES;
  }

  @Override
  public ClassloaderModel load(URL location, Map<String, Object> attributes) throws MalformedClassloaderModelException {
    Optional<URL> runtimeClasses = ClassloaderModelUtils.parseRuntimeClasses(location);
    URL[] runtimeLibs = ClassloaderModelUtils.parseRuntimeLibs(location);

    Properties properties = getProperties(location);

    Set<String> exportedPackages = getPackages(properties.getProperty("artifact.export.classPackages"));
    Set<String> exportedResources = getPackages(properties.getProperty("artifact.export.resources"));
    Set<ArtifactDependency> dependencies = getArtifactDependencies(properties);

    return new DefaultClassloaderModel(runtimeClasses, runtimeLibs, exportedPackages, exportedResources, dependencies);
  }

  private Properties getProperties(URL location) throws MalformedClassloaderModelException {
    Properties properties = new Properties();
    Optional<InputStream> propertiesInputStream = new URLPluginResourceLoader().loadResource(location, "plugin.properties");
    if (!propertiesInputStream.isPresent()) {
      throw new MalformedClassloaderModelException(format("Couldn't find plugin.properties file in the plugin located at %s",
                                                          location.toString()));
    }
    try {
      properties.load(propertiesInputStream.get());
    } catch (IOException e) {
      throw new MalformedClassloaderModelException("There was an issue reading plugin.properties file", e);
    }
    return properties;
  }

  private Set<String> getPackages(String exportedPackages) {
    Set<String> exported = new HashSet<>();
    if (StringUtils.isNotBlank(exportedPackages)) {
      final String[] exports = exportedPackages.split(",");
      exported = new HashSet<>(Arrays.asList(exports));
    }
    return exported;
  }

  private Set<ArtifactDependency> getArtifactDependencies(Properties properties) {
    Set<ArtifactDependency> dependencies = new HashSet<>();

    String dependenciesString = properties.getProperty("plugin.dependencies");
    if (StringUtils.isNotBlank(dependenciesString)) {
      for (String dependencyName : dependenciesString.split(",")) {
        //TODO MULE-10440 until there's a better way to consume the information from the plugin.properties file, groupID, version, type and classifier will be invented for its later consume.
        dependencies
            .add(new DefaultArtifactDependency(dependencyName + "-groupId",
                                               dependencyName,
                                               "1.2.44-hf1",
                                               "jar",
                                               "mule-plugin"));
      }
    }
    return dependencies;
  }
}
