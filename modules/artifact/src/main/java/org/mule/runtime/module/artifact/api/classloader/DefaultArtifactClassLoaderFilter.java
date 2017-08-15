/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.classloader;

import static java.util.Collections.unmodifiableSet;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang3.builder.ToStringStyle.MULTI_LINE_STYLE;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filters classes and resources using a {@link ArtifactDescriptor} describing exported/blocked names.
 * <p>
 * An exact blocked/exported name match has precedence over a prefix match on a blocked/exported prefix. This enables to export
 * classes or subpackages from a blocked package.
 * </p>
 */
public class DefaultArtifactClassLoaderFilter implements ArtifactClassLoaderFilter {

  public static final ArtifactClassLoaderFilter NULL_CLASSLOADER_FILTER =
      new DefaultArtifactClassLoaderFilter(Collections.EMPTY_SET, Collections.EMPTY_SET);

  private static final char PACKAGE_SEPARATOR = '.';
  private static final String EMPTY_PACKAGE = "";
  private static final char RESOURCE_SEPARATOR = '/';

  private final Set<String> exportedClassPackages;
  private final Set<String> exportedResources;

  /**
   * Creates a new classLoader filter
   *
   * @param exportedClassPackages class package names to export. Can be empty
   * @param exportedResources resource file names to export. Can be empty
   */
  public DefaultArtifactClassLoaderFilter(Set<String> exportedClassPackages, Set<String> exportedResources) {
    checkArgument(exportedClassPackages != null, "Exported class packages cannot be null");
    checkArgument(exportedResources != null, "Exported resource cannot be null");

    this.exportedClassPackages = unmodifiableSet(sanitizeClassPackageS(exportedClassPackages));
    this.exportedResources = unmodifiableSet(sanitizeExportedResources(exportedResources));
  }

  private Set<String> sanitizeExportedResources(Set<String> exportedResources) {
    return exportedResources.stream().map(this::sanitizeResourceName).collect(Collectors.toSet());
  }

  private Set<String> sanitizeClassPackageS(Set<String> exportedClassPackages) {
    return exportedClassPackages.stream().map(this::sanitizePackageName).collect(Collectors.toSet());
  }

  private String sanitizePackageName(String exportedClassPackage) {
    exportedClassPackage = exportedClassPackage.trim();
    exportedClassPackage = exportedClassPackage.endsWith(".")
        ? exportedClassPackage.substring(0, exportedClassPackage.length() - 1) : exportedClassPackage;
    return exportedClassPackage;
  }

  @Override
  public boolean exportsClass(String className) {
    checkArgument(!StringUtils.isEmpty(className), "Class name cannot be empty");
    final String packageName = getPackageName(className);

    return exportedClassPackages.contains(packageName);
  }

  @Override
  public boolean exportsResource(String name) {
    checkArgument(name != null, "Resource name cannot be null");
    final String sanitizeResourceName = sanitizeResourceName(name);

    return exportedResources.contains(sanitizeResourceName);
  }

  @Override
  public Set<String> getExportedClassPackages() {
    return exportedClassPackages;
  }

  @Override
  public Set<String> getExportedResources() {
    return exportedResources;
  }

  private String sanitizeResourceName(String resourceName) {
    String sanitizedResource = "";
    if (resourceName.length() > 0) {
      sanitizedResource = (resourceName.charAt(0) == RESOURCE_SEPARATOR) ? resourceName.substring(1) : resourceName;
      if (sanitizedResource.length() > 0) {
        sanitizedResource = sanitizedResource.charAt(sanitizedResource.length() - 1) == RESOURCE_SEPARATOR
            ? sanitizedResource.substring(0, sanitizedResource.length() - 1) : sanitizedResource;
      }
    }
    return sanitizedResource;
  }

  private String getPackageName(String className) {
    return (className.lastIndexOf(PACKAGE_SEPARATOR) < 0) ? EMPTY_PACKAGE
        : className.substring(0, className.lastIndexOf(PACKAGE_SEPARATOR));
  }

  @Override
  public String toString() {
    return reflectionToString(this, MULTI_LINE_STYLE);
  }
}
