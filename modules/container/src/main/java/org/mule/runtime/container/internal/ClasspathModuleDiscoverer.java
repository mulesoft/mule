/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import static java.io.File.createTempFile;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.core.api.util.FileUtils.stringToFile;
import static org.mule.runtime.core.api.util.PropertiesUtils.discoverProperties;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.module.artifact.api.classloader.ExportedService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovers {@link MuleModule} searching for {@link #MODULE_PROPERTIES} files resources available in a given classloader.
 */
public class ClasspathModuleDiscoverer implements ModuleDiscoverer {

  private static Logger logger = LoggerFactory.getLogger(ClasspathModuleDiscoverer.class);

  public static final String MODULE_PROPERTIES = "META-INF/mule-module.properties";
  public static final String EXPORTED_CLASS_PACKAGES_PROPERTY = "artifact.export.classPackages";
  public static final String PRIVILEGED_EXPORTED_CLASS_PACKAGES_PROPERTY = "artifact.privileged.classPackages";
  public static final String PRIVILEGED_ARTIFACTS_PROPERTY = "artifact.privileged.artifactIds";
  public static final String EXPORTED_RESOURCE_PROPERTY = "artifact.export.resources";
  public static final String EXPORTED_SERVICES_PROPERTY = "artifact.export.services";

  private final ClassLoader classLoader;

  public ClasspathModuleDiscoverer(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public List<MuleModule> discover() {
    List<MuleModule> modules = new LinkedList<>();
    Set<String> moduleNames = new HashSet<>();

    try {
      for (Properties moduleProperties : discoverProperties(classLoader, getModulePropertiesFileName())) {
        final MuleModule module = createModule(moduleProperties);

        if (moduleNames.contains(module.getName())) {
          logger.warn(format("Ignoring duplicated module '%s'", module.getName()));
        } else {
          moduleNames.add(module.getName());
          modules.add(module);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Cannot discover mule modules", e);
    }

    return modules;
  }

  protected String getModulePropertiesFileName() {
    return MODULE_PROPERTIES;
  }

  private MuleModule createModule(Properties moduleProperties) {
    final String moduleName = (String) moduleProperties.get("module.name");
    Set<String> modulePackages = getExportedPackageByProperty(moduleProperties, EXPORTED_CLASS_PACKAGES_PROPERTY);
    Set<String> modulePaths = getExportedResourcePaths(moduleProperties);
    Set<String> modulePrivilegedPackages =
        getExportedPackageByProperty(moduleProperties, PRIVILEGED_EXPORTED_CLASS_PACKAGES_PROPERTY);
    Set<String> privilegedArtifacts = getPrivilegedArtifactIds(moduleProperties);
    List<ExportedService> exportedServices = getExportedServices(moduleProperties, EXPORTED_SERVICES_PROPERTY);

    return new MuleModule(moduleName, modulePackages, modulePaths, modulePrivilegedPackages, privilegedArtifacts,
                          exportedServices);
  }

  private List<ExportedService> getExportedServices(Properties moduleProperties, String exportedServicesProperty) {
    final String privilegedExportedPackagesProperty = (String) moduleProperties.get(exportedServicesProperty);
    List<ExportedService> exportedServices;
    if (!isEmpty(privilegedExportedPackagesProperty)) {
      exportedServices = getServicesFromProperty(privilegedExportedPackagesProperty);
    } else {
      exportedServices = new ArrayList<>();
    }
    return exportedServices;
  }

  private List<ExportedService> getServicesFromProperty(String privilegedExportedPackagesProperty) {
    List<ExportedService> exportedServices = new ArrayList<>();

    for (String exportedServiceDefinition : privilegedExportedPackagesProperty.split(",")) {
      String[] split = exportedServiceDefinition.split(":");
      String serviceInterface = split[0];
      String serviceImplementation = split[1];
      URL resource;
      try {
        File serviceFile = createTempFile(serviceInterface, "tmp");
        stringToFile(serviceFile.getAbsolutePath(), serviceImplementation);
        resource = serviceFile.toURI().toURL();
      } catch (IOException e) {
        throw new IllegalStateException(format("Error creating temporary service provider file for '%s'", serviceInterface), e);
      }
      exportedServices.add(new ExportedService(serviceInterface, resource));
    }

    return exportedServices;
  }

  private Set<String> getPrivilegedArtifactIds(Properties moduleProperties) {
    Set<String> privilegedArtifacts;
    final String privilegedArtifactsProperty = (String) moduleProperties.get(PRIVILEGED_ARTIFACTS_PROPERTY);
    Set<String> artifactsIds = new HashSet<>();
    if (!isEmpty(privilegedArtifactsProperty)) {
      for (String artifactName : privilegedArtifactsProperty.split(",")) {
        if (!isEmpty(artifactName.trim())) {
          artifactsIds.add(artifactName);
        }
      }
    }
    privilegedArtifacts = artifactsIds;
    return privilegedArtifacts;
  }

  private Set<String> getExportedPackageByProperty(Properties moduleProperties, String privilegedExportedClassPackagesProperty) {
    final String privilegedExportedPackagesProperty = (String) moduleProperties.get(privilegedExportedClassPackagesProperty);
    Set<String> modulePrivilegedPackages;
    if (!isEmpty(privilegedExportedPackagesProperty)) {
      modulePrivilegedPackages = getPackagesFromProperty(privilegedExportedPackagesProperty);
    } else {
      modulePrivilegedPackages = new HashSet<>();
    }
    return modulePrivilegedPackages;
  }

  private Set<String> getExportedResourcePaths(Properties moduleProperties) {
    Set<String> paths = new HashSet<>();
    final String exportedResourcesProperty = (String) moduleProperties.get(EXPORTED_RESOURCE_PROPERTY);
    if (!isEmpty(exportedResourcesProperty)) {
      for (String path : exportedResourcesProperty.split(",")) {
        if (!isEmpty(path.trim())) {
          if (path.startsWith("/")) {
            path = path.substring(1);
          }
          paths.add(path);
        }
      }
    }
    return paths;
  }

  private Set<String> getPackagesFromProperty(String privilegedExportedPackagesProperty) {
    Set<String> packages = new HashSet<>();
    for (String packageName : privilegedExportedPackagesProperty.split(",")) {
      packageName = packageName.trim();
      if (!isEmpty(packageName)) {
        packages.add(packageName);
      }
    }
    return packages;
  }
}
