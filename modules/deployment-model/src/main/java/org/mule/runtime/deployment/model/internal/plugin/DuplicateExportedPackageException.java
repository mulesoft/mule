/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal.plugin;

import java.util.List;
import java.util.Map;

/**
 * Thrown to indicate that more than one artifact is exporting a given package.
 */
public class DuplicateExportedPackageException extends PluginResolutionError {

  /**
   * {@inheritDoc}
   * 
   * @param pluginsPerPackage a map containing a list of artifact names exporting a given Java package
   */
  public DuplicateExportedPackageException(Map<String, List<String>> pluginsPerPackage) {
    super(buildPackageDuplicationErrorMessage(pluginsPerPackage));
  }

  private static String buildPackageDuplicationErrorMessage(Map<String, List<String>> exportedPackages) {
    StringBuilder errorMessageBuilder = new StringBuilder("There are multiple artifacts exporting the same package:");

    for (String packageName : exportedPackages.keySet()) {
      final List<String> exportedOn = exportedPackages.get((packageName));
      if (exportedOn.size() > 1) {
        errorMessageBuilder.append("\nPackage ").append(packageName).append(" is exported on artifacts: ");
        boolean firstPlugin = true;
        for (String plugin : exportedOn) {
          if (firstPlugin) {
            firstPlugin = false;
          } else {
            errorMessageBuilder.append(", ");
          }
          errorMessageBuilder.append(plugin);
        }
      }
    }

    return errorMessageBuilder.toString();
  }
}
