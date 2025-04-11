/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.ast.api.validation.ImportValidationConstants.IMPORT_ROOT_ELEMENT_MISMATCH_MESSAGE_PATTERN;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;

/**
 * Import elements in config files point to actual and valid files.
 * <p>
 * This validation doesn't do the actual resource resolution, but instead generates the failed validation for any errors that
 * happened during the resolution of the imports during the artifact parsing.
 *
 * @since 4.5
 */
public class ImportValidTarget extends AbstractImportValidation {

  @Override
  public String getName() {
    return "Imported files exist";
  }

  @Override
  public String getDescription() {
    return "Import elements in config files point to actual and valid files.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  protected boolean appliesValidationFor(String resolutionFailureMessage) {
    // this specific error is added in ast 1.4, but we need to maintain backwards compatibility with previous apps, so this
    // validation ignores this error.
    // Refer to MuleRuntimeFeature#ENFORCE_IMPORT_TARGET_SAME_TYPE
    return !IMPORT_ROOT_ELEMENT_MISMATCH_MESSAGE_PATTERN.matcher(resolutionFailureMessage).matches();
  }

}
