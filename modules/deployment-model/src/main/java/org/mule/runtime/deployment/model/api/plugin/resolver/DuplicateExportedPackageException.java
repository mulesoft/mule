/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.deployment.model.api.plugin.resolver;

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
