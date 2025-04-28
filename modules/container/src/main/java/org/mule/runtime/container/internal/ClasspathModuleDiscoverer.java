/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.internal;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.container.api.MuleFoldersUtil.getModulesTempFolder;
import static org.mule.runtime.core.api.util.FileUtils.stringToFile;
import static org.mule.runtime.core.api.util.PropertiesUtils.discoverProperties;

import static java.lang.String.format;
import static java.nio.file.Files.createTempFile;
import static java.util.Collections.emptyList;

import static org.apache.commons.io.FileUtils.cleanDirectory;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.container.api.discoverer.ModuleDiscoverer;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.jpms.api.MuleContainerModule;
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
import java.util.function.BiFunction;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovers {@link MuleModule} searching for {@link #MODULE_PROPERTIES} files resources available in a given classloader.
 */
public final class ClasspathModuleDiscoverer implements ModuleDiscoverer {

  private static final String TMP_FOLDER_SUFFIX = "tmp";

  private static Logger logger = LoggerFactory.getLogger(ClasspathModuleDiscoverer.class);

  public static final String MODULE_PROPERTIES = "META-INF/mule-module.properties";

  private final Function<String, File> serviceInterfaceToServiceFile;
  private final BiFunction<String, File, URL> fileToResource;
  private final String modulePropertiesResource;

  public ClasspathModuleDiscoverer() {
    this(createModulesTemporaryFolder());
  }

  public ClasspathModuleDiscoverer(String modulePropertiesResource) {
    this(createModulesTemporaryFolder(), modulePropertiesResource);
  }

  public ClasspathModuleDiscoverer(File temporaryFolder) {
    this(temporaryFolder, MODULE_PROPERTIES);
  }

  public ClasspathModuleDiscoverer(File temporaryFolder, String modulePropertiesResource) {
    this.serviceInterfaceToServiceFile =
        serviceInterface -> wrappingInIllegalStateException(() -> createTempFile(temporaryFolder.toPath(), serviceInterface,
                                                                                 TMP_FOLDER_SUFFIX)
            .toFile(),
                                                            serviceInterface);
    this.fileToResource =
        (serviceInterface, serviceFile) -> wrappingInIllegalStateException(() -> serviceFile.toURI().toURL(), serviceInterface);
    this.modulePropertiesResource = modulePropertiesResource;
  }

  private <T> T wrappingInIllegalStateException(CheckedSupplier<T> supplier, String serviceInterface) {
    try {
      return supplier.get();
    } catch (Exception e) {
      throw new IllegalStateException(format("Error creating temporary service provider file for '%s'", serviceInterface), e);
    }
  }

  public ClasspathModuleDiscoverer(Function<String, File> serviceInterfaceToServiceFile,
                                   BiFunction<String, File, URL> fileToResource,
                                   String modulePropertiesResource) {
    this.serviceInterfaceToServiceFile = serviceInterfaceToServiceFile;
    this.fileToResource = fileToResource;
    this.modulePropertiesResource = modulePropertiesResource;
  }

  protected static File createModulesTemporaryFolder() {
    File modulesTempFolder = getModulesTempFolder();
    if (modulesTempFolder.exists()) {
      try {
        cleanDirectory(modulesTempFolder);
      } catch (IOException e) {
        throw new MuleRuntimeException(createStaticMessage(format(
                                                                  "Could not clean up folder %s, validate that the process has permissions over that directory",
                                                                  modulesTempFolder.getAbsolutePath())));
      }
    } else {
      if (!modulesTempFolder.mkdirs()) {
        throw new MuleRuntimeException(createStaticMessage(format(
                                                                  "Could not create folder %s, validate that the process has permissions over that directory",
                                                                  modulesTempFolder.getAbsolutePath())));
      }
    }
    return modulesTempFolder;
  }

  @Override
  public List<MuleContainerModule> discover() {
    List<MuleContainerModule> modules = new LinkedList<>();
    Set<String> moduleNames = new HashSet<>();

    try {
      for (Properties moduleProperties : discoverProperties(this.getClass().getClassLoader(), getModulePropertiesFileName())) {
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
    return modulePropertiesResource;
  }

  public MuleModule createModule(Properties moduleProperties) {
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
    final String exportedPackagesProperty = (String) moduleProperties.get(exportedServicesProperty);
    List<ExportedService> exportedServices;
    if (!isEmpty(exportedPackagesProperty)) {
      exportedServices = getServicesFromProperty(exportedPackagesProperty);
    } else {
      exportedServices = emptyList();
    }
    return exportedServices;
  }

  private List<ExportedService> getServicesFromProperty(String exportedPackagesProperty) {
    List<ExportedService> exportedServices = new ArrayList<>();

    for (String exportedServiceDefinition : exportedPackagesProperty.split(",")) {
      String[] split = exportedServiceDefinition.split(":");
      String serviceInterface = split[0];
      String serviceImplementation = split[1];
      URL resource;
      try {
        File serviceFile = serviceInterfaceToServiceFile.apply(serviceInterface);
        serviceFile.deleteOnExit();

        stringToFile(serviceFile.getAbsolutePath(), serviceImplementation);
        resource = fileToResource.apply(serviceInterface, serviceFile);
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
