/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.classloader;

import static org.mule.runtime.core.api.util.StringUtils.isBlank;

import org.mule.api.annotation.NoInstantiate;

import java.util.HashSet;
import java.util.Set;

/**
 * Creates {@link DefaultArtifactClassLoaderFilter} instances
 */
@NoInstantiate
public final class ArtifactClassLoaderFilterFactory implements ClassLoaderFilterFactory {

  private static final String PACKAGE_SEPARATOR = "/";

  @Override
  public ArtifactClassLoaderFilter create(String exportedClassPackages, String exportedResources) {
    Set<String> exportedArtifactPackages = parseExportedResource(exportedClassPackages);
    Set<String> exportedArtifactResources = parseExportedResource(exportedResources);

    if (exportedArtifactPackages.isEmpty() && exportedArtifactResources.isEmpty()) {
      return DefaultArtifactClassLoaderFilter.NULL_CLASSLOADER_FILTER;
    } else {
      return new DefaultArtifactClassLoaderFilter(exportedArtifactPackages, exportedArtifactResources);
    }
  }

  public static Set<String> parseExportedResource(String exportedPackages) {
    Set<String> exported = new HashSet<>();
    if (!isBlank(exportedPackages)) {
      final String[] exports = exportedPackages.split(",");
      for (String export : exports) {
        export = export.trim();
        if (export.startsWith(PACKAGE_SEPARATOR)) {
          export = export.substring(1);
        }
        if (export.endsWith(PACKAGE_SEPARATOR)) {
          export = export.substring(0, export.length() - 1);
        }
        exported.add(export);
      }
    }

    return exported;
  }
}
