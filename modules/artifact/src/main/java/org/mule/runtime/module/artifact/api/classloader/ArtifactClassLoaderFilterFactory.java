/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.classloader;

import static org.mule.runtime.core.api.util.StringUtils.isBlank;

import java.util.HashSet;
import java.util.Set;

/**
 * Creates {@link DefaultArtifactClassLoaderFilter} instances
 */
public class ArtifactClassLoaderFilterFactory implements ClassLoaderFilterFactory {

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
