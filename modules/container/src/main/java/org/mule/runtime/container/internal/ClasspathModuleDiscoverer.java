/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.core.util.PropertiesUtils.discoverProperties;
import static org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter.EXPORTED_RESOURCE_PROPERTY;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Discovers {@link MuleModule} searching for {@link #MODULE_PROPERTIES} files resources available in a given classloader.
 */
public class ClasspathModuleDiscoverer implements ModuleDiscoverer {

  public static final String MODULE_PROPERTIES = "META-INF/mule-module.properties";
  private final ClassLoader classLoader;

  public ClasspathModuleDiscoverer(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public List<MuleModule> discover() {
    List<MuleModule> modules = new LinkedList<>();
    Set<String> moduleNames = new HashSet<>();

    try {
      for (Properties moduleProperties : discoverProperties(classLoader, MODULE_PROPERTIES)) {
        final MuleModule module = createModule(moduleProperties);

        if (moduleNames.contains(module.getName())) {
          throw new IllegalStateException(String.format("Module '%s' was already defined", module.getName()));
        }
        moduleNames.add(module.getName());
        modules.add(module);
      }
    } catch (IOException e) {
      throw new RuntimeException("Cannot discover mule modules", e);
    }

    return modules;
  }

  private MuleModule createModule(Properties moduleProperties) {
    final String moduleName = (String) moduleProperties.get("module.name");
    if (isEmpty(moduleName)) {
      throw new IllegalStateException("Mule-module.properties must contain module.name property");
    }

    Set<String> modulePackages = new HashSet<>();
    Set<String> modulePaths = new HashSet<>();

    final String exportedPackagesProperty = (String) moduleProperties.get(EXPORTED_CLASS_PACKAGES_PROPERTY);
    if (!isEmpty(exportedPackagesProperty)) {
      for (String packageName : exportedPackagesProperty.split(",")) {
        packageName = packageName.trim();
        if (!isEmpty(packageName)) {
          modulePackages.add(packageName);
        }
      }
    }

    final String exportedResourcesProperty = (String) moduleProperties.get(EXPORTED_RESOURCE_PROPERTY);
    if (!isEmpty(exportedResourcesProperty)) {
      for (String path : exportedResourcesProperty.split(",")) {
        if (!isEmpty(path.trim())) {
          if (path.startsWith("/")) {
            path = path.substring(1);
          }
          modulePaths.add(path);
        }
      }
    }

    return new MuleModule(moduleName, modulePackages, modulePaths);
  }

}
