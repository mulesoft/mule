/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.validator;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.module.extension.internal.resources.manifest.DefaultClassPackageFinder;
import org.mule.runtime.module.extension.internal.resources.manifest.ExportedPackagesCollector;
import org.mule.runtime.module.extension.internal.resources.manifest.ProcessingEnvironmentClassPackageFinder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;

/**
 * {@link ExtensionModelValidator} which validates that the exported packages for the current extension exports API declared
 * packages and doesn't export packages declared as internal.
 *
 * @since 4.1
 */
public class ExportedPackagesValidator implements ExtensionModelValidator {

  public static final String EXPORTED_PACKAGES_VALIDATOR_SKIP = "exportedPackagesValidator.skip";
  public static final String EXPORTED_PACKAGES_VALIDATOR_STRICT_VALIDATION = "exportedPackagesValidator.strictValidation";
  private final ProcessingEnvironment processingEnv;

  public ExportedPackagesValidator(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
  }

  @Override
  public void validate(ExtensionModel model, ProblemsReporter problemsReporter) {
    if (shouldValidate()) {
      ExportedPackagesCollector exportedPackagesCollector = getExportedArtifactsCollector(model);
      Map<String, Collection<String>> exportedPackages = exportedPackagesCollector.getDetailedExportedPackages();

      Map<String, Collection<String>> internalPackages = new HashMap<>();
      Map<String, Collection<String>> noVisibilityDeclaredPackages = new HashMap<>();

      exportedPackages
          .forEach((packageName, classes) -> {
            if (packageName.contains(".api.") || packageName.endsWith(".api")) {
              // valid package
            } else if (packageName.contains(".internal.") || packageName.endsWith(".internal")) {
              internalPackages.put(packageName, classes);
            } else {
              noVisibilityDeclaredPackages.put(packageName, classes);
            }
          });

      if (!internalPackages.isEmpty()) {
        Problem problem = new Problem(model, getErrorMessage(model, internalPackages, "exports the following internal packages"));
        if (strictValidation()) {
          problemsReporter.addError(problem);
        } else {
          problemsReporter.addWarning(problem);
        }
      }

      if (!noVisibilityDeclaredPackages.isEmpty()) {
        problemsReporter
            .addWarning(new Problem(model,
                                    getErrorMessage(model, noVisibilityDeclaredPackages,
                                                    "exports packages which doesn't have a defined visibility, 'api' or 'internal'")));
      }
    }
  }

  private String getErrorMessage(ExtensionModel model, Map<String, Collection<String>> packages, String message) {
    StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append("The extension [").append(model.getName()).append("] ").append(message).append(":\n");
    packages.forEach((packageName, classes) -> {
      messageBuilder.append("-> [").append(packageName).append("]:")
          .append(" because of these classes: \n");
      classes.forEach(clazz -> messageBuilder.append("        * ")
          .append(clazz)
          .append("\n"));
      messageBuilder.append("\n");

    });
    return messageBuilder.toString();
  }

  private boolean shouldValidate() {
    String skip = processingEnv.getOptions().get(EXPORTED_PACKAGES_VALIDATOR_SKIP);
    if (skip == null) {
      skip = System.getProperty(EXPORTED_PACKAGES_VALIDATOR_SKIP);
    }
    return !(skip != null ? Boolean.valueOf(skip) : false);
  }

  private boolean strictValidation() {
    String strictValidation = processingEnv.getOptions().get(EXPORTED_PACKAGES_VALIDATOR_STRICT_VALIDATION);
    if (strictValidation == null) {
      strictValidation = System.getProperty(EXPORTED_PACKAGES_VALIDATOR_STRICT_VALIDATION);
    }
    return strictValidation != null ? Boolean.valueOf(strictValidation) : true;
  }

  private ExportedPackagesCollector getExportedArtifactsCollector(ExtensionModel extensionModel) {
    if (processingEnv != null) {
      DefaultClassPackageFinder defaultClassPackageFinder = new DefaultClassPackageFinder();
      defaultClassPackageFinder.addAdditionalPackageFinder(new ProcessingEnvironmentClassPackageFinder(processingEnv));
      return new ExportedPackagesCollector(extensionModel, defaultClassPackageFinder);
    } else {
      return new ExportedPackagesCollector(extensionModel);
    }
  }
}
