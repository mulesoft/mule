/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang3.builder.ToStringStyle.MULTI_LINE_STYLE;

import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.module.artifact.api.classloader.DefaultArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderFilter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Creates a {@link ClassLoaderFilter} for the container filter what is exposed to Mule artifacts.
 * <p/>
 * Filter is constructed searching for {code}mule-module.properties{code} files in the classpath and then merging the
 * corresponding packages and resources in a new filter.
 */
public class ContainerClassLoaderFilterFactory {

  private static final String EMPTY_PACKAGE = "";
  private static final char RESOURCE_SEPARATOR = '/';

  public ClassLoaderFilter create(Set<String> bootPackages, List<MuleModule> muleModules) {
    final Set<String> resources = getExportedResourcePaths(muleModules);
    final Set<String> packages = getModuleExportedPackages(muleModules);
    final ArtifactClassLoaderFilter artifactClassLoaderFilter = new DefaultArtifactClassLoaderFilter(packages, resources);

    return new ContainerClassLoaderFilter(artifactClassLoaderFilter, bootPackages);
  }

  private Set<String> getExportedResourcePaths(List<MuleModule> muleModules) {
    Set<String> resources = new HashSet<>();

    for (MuleModule muleModule : muleModules) {
      resources.addAll(muleModule.getExportedPaths());
    }

    return resources;
  }

  private Set<String> getModuleExportedPackages(List<MuleModule> muleModules) {
    Set<String> packages = new HashSet<>();
    for (MuleModule muleModule : muleModules) {
      packages.addAll(muleModule.getExportedPackages());
    }

    return packages;
  }

  public static class ContainerClassLoaderFilter implements ClassLoaderFilter {

    public static final String CLASS_PACKAGE_SPLIT_REGEX = "\\.";
    public static final String RESOURCE_PACKAGE_SPLIT_REGEX = "/";
    private final ClassLoaderFilter moduleClassLoaderFilter;
    private final Set<String> bootPackages;

    public ContainerClassLoaderFilter(ClassLoaderFilter moduleClassLoaderFilter, Set<String> bootPackages) {
      this.moduleClassLoaderFilter = moduleClassLoaderFilter;
      this.bootPackages = bootPackages;
    }

    @Override
    public boolean exportsClass(String name) {
      boolean exported = moduleClassLoaderFilter.exportsClass(name);

      if (!exported) {
        exported = isExportedBooPackage(name, CLASS_PACKAGE_SPLIT_REGEX);
      }
      return exported;
    }

    @Override
    public boolean exportsResource(String name) {
      boolean exported = moduleClassLoaderFilter.exportsResource(name);

      if (!exported) {
        final String resourceFolder = getResourceFolder(name);
        exported = moduleClassLoaderFilter.exportsResource(resourceFolder);
        if (!exported) {
          exported = isExportedBooPackage(name, RESOURCE_PACKAGE_SPLIT_REGEX);
        }
      }

      return exported;
    }

    private String getResourceFolder(String resourceName) {
      String resourceFolder = "";
      if (resourceName.length() > 0) {
        resourceFolder = (resourceName.charAt(0) == RESOURCE_SEPARATOR) ? resourceName.substring(1) : resourceName;
        resourceFolder = (resourceFolder.lastIndexOf(RESOURCE_SEPARATOR) < 0) ? EMPTY_PACKAGE
            : resourceFolder.substring(0, resourceFolder.lastIndexOf(RESOURCE_SEPARATOR));
      }
      return resourceFolder;
    }

    private boolean isExportedBooPackage(String name, String splitRegex) {
      boolean exported = false;
      final String[] splitName = name.split(splitRegex);
      final String[] packages = Arrays.copyOf(splitName, splitName.length - 1);
      String candidatePackage = "";

      for (String currentPackage : packages) {
        if (candidatePackage.length() != 0) {
          candidatePackage += ".";
        }
        candidatePackage += currentPackage;

        if (bootPackages.contains(candidatePackage)) {
          exported = true;
          break;
        }

      }
      return exported;
    }

    @Override
    public String toString() {
      return reflectionToString(this, MULTI_LINE_STYLE);
    }
  }

}
