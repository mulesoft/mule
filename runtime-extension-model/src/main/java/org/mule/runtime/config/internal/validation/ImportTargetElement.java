/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.config.MuleRuntimeFeature.ENFORCE_IMPORT_TARGET_SAME_TYPE;
import static org.mule.runtime.ast.api.validation.ImportValidationConstants.IMPORT_ROOT_ELEMENT_MISMATCH_MESSAGE_PATTERN;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.Validation.Level.WARN;

import org.mule.runtime.api.config.FeatureFlaggingService;

import java.util.Optional;

/**
 * Import elements in config files point to files with the same element type.
 * <p>
 * This validation doesn't do the actual resource resolution, but instead generates the failed validation for any errors that
 * happened during the resolution of the imports during the artifact parsing.
 *
 * @since 4.8
 */
public class ImportTargetElement extends AbstractImportValidation {

  private final Optional<FeatureFlaggingService> featureFlaggingService;

  public ImportTargetElement(Optional<FeatureFlaggingService> featureFlaggingService) {
    this.featureFlaggingService = featureFlaggingService;
  }

  @Override
  public String getName() {
    return "Imported files have same element type";
  }

  @Override
  public String getDescription() {
    return "Import elements in config files point to files with the same element type.";
  }

  @Override
  public Level getLevel() {
    return featureFlaggingService.map(ffs -> ffs.isEnabled(ENFORCE_IMPORT_TARGET_SAME_TYPE)).orElse(true)
        ? ERROR
        : WARN;
  }

  @Override
  protected boolean appliesValidationFor(String resolutionFailureMessage) {
    return IMPORT_ROOT_ELEMENT_MISMATCH_MESSAGE_PATTERN.matcher(resolutionFailureMessage).matches();
  }

}
