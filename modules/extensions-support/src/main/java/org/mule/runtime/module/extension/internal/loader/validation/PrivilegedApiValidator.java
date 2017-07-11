/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;

/**
 * Validates that the privileged API is properly defined.
 * 
 * @since 1.0
 */
public class PrivilegedApiValidator implements ExtensionModelValidator {

  static final String NO_PRIVILEGED_ARTIFACTS_ERROR = "Extension has privileged packages but no privileged artifacts defined";
  static final String NO_PRIVILEGED_PACKAGES_ERROR = "Extension has privileged artifacts but no privileged packages defined";

  @Override
  public void validate(ExtensionModel model, ProblemsReporter problemsReporter) throws IllegalModelDefinitionException {
    final boolean hasPrivilegedPackages = model.getPrivilegedPackages() != null && !model.getPrivilegedPackages().isEmpty();
    final boolean hasPrivilegedArtifacts = model.getPrivilegedArtifacts() != null && !model.getPrivilegedArtifacts().isEmpty();

    if (hasPrivilegedPackages != hasPrivilegedArtifacts) {
      if (hasPrivilegedPackages) {
        problemsReporter.addError(new Problem(model, NO_PRIVILEGED_ARTIFACTS_ERROR));
      } else {
        problemsReporter.addError(new Problem(model, NO_PRIVILEGED_PACKAGES_ERROR));
      }
    }
  }
}
