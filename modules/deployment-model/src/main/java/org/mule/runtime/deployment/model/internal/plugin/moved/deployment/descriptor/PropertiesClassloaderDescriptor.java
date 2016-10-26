/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin.moved.deployment.descriptor;

import static java.lang.String.format;
import org.mule.runtime.deployment.model.api.plugin.moved.dependency.ArtifactDependency;
import org.mule.runtime.deployment.model.api.plugin.moved.dependency.Scope;
import org.mule.runtime.deployment.model.api.plugin.moved.deployment.DeploymentModel;
import org.mule.runtime.deployment.model.api.plugin.moved.deployment.MalformedDeploymentModelException;
import org.mule.runtime.deployment.model.internal.plugin.moved.dependency.DefaultArtifactDependency;
import org.mule.runtime.deployment.model.internal.plugin.moved.deployment.DefaultDeploymentModel;
import org.mule.runtime.deployment.model.internal.plugin.moved.resource.URLPluginResourceLoader;
import org.mule.runtime.module.artifact.net.MulePluginUrlStreamHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents a TODO-10785
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
  public DeploymentModel load(URL location, Map<String, Object> attributes) throws MalformedDeploymentModelException {
    Optional<URL> runtimeClasses = parseRuntimeClasses(location);

    Properties properties = getProperties(location);

    Set<String> exportedPackages = getPackages(properties.getProperty("artifact.export.classPackages"));
    Set<String> exportedResources = getPackages(properties.getProperty("artifact.export.resources"));
    Set<ArtifactDependency> dependencies = getArtifactDependencies(properties);

    return new DefaultDeploymentModel(runtimeClasses, exportedPackages, exportedResources, dependencies);
  }

  private Properties getProperties(URL location) throws MalformedDeploymentModelException {
    Properties properties = new Properties();
    Optional<InputStream> propertiesInputStream = new URLPluginResourceLoader().loadResource(location, "plugin.properties");
    if (!propertiesInputStream.isPresent()) {
      throw new MalformedDeploymentModelException(format("Couldn't find plugin.properties file in the plugin located at %s",
                                                         location.toString()));
    }
    try {
      properties.load(propertiesInputStream.get());
    } catch (IOException e) {
      throw new MalformedDeploymentModelException("There was an issue reading plugin.properties file", e);
    }
    return properties;
  }

  private Optional<URL> parseRuntimeClasses(URL location) throws MalformedDeploymentModelException {
    boolean isZip = location.getFile().endsWith(".zip");
    try {
      return Optional.of(isZip ? new URL(MulePluginUrlStreamHandler.PROTOCOL + ":" + location + "!/" + "classes" + "!/")
          : new URL(location, "classes"));
    } catch (MalformedURLException e) {
      throw new MalformedDeploymentModelException("Cannot assembly /classes URL", e);
    }
  }

  private Set<String> getPackages(String exportedPackages) {
    Set<String> exported = new HashSet<>();
    if (StringUtils.isNotBlank(exportedPackages)) {
      final String[] exports = exportedPackages.split(",");
      exported = new HashSet<>(Arrays.asList(exports));
    }
    return exported;
  }

  //TODO MULE-10785 This sanitize must be moved to ArtifactPluginDescriptor when being initialized
  //private final static String PACKAGE_SEPARATOR = "/";
  //private Set<String> getPackages(String[] exports) {
  //  Set<String> exported = new HashSet<>();
  //  for (String export : exports) {
  //    export = export.trim();
  //    if (export.startsWith(PACKAGE_SEPARATOR)) {
  //      export = export.substring(1);
  //    }
  //    if (export.endsWith(PACKAGE_SEPARATOR)) {
  //      export = export.substring(0, export.length() - 1);
  //    }
  //    exported.add(export);
  //  }
  //  return exported;
  //}

  private Set<ArtifactDependency> getArtifactDependencies(Properties properties) {
    Set<ArtifactDependency> dependencies = new HashSet<>();

    String dependenciesString = properties.getProperty("plugin.dependencies");
    if (StringUtils.isNotBlank(dependenciesString)) {
      for (String dependencyName : dependenciesString.split(",")) {
        //TODO WIP until there's a better way to consume the information from the plugin.properties file
        dependencies
            .add(new DefaultArtifactDependency(dependencyName + "-groupId",
                                               dependencyName,
                                               "1.2.44-hf1",
                                               "jar",
                                               "mule-plugin",
                                               Scope.PROVIDED));
      }
    }
    return dependencies;
  }
}
