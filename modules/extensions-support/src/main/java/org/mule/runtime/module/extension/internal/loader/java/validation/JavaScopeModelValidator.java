/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.validation;

import static org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver.getDefault;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isScope;
import static org.mule.runtime.extension.internal.util.ExtensionValidationUtils.validateNoInlineParameters;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.construct.HasConstructModels;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.dsl.syntax.resolver.SingleExtensionImportTypesStrategy;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.ProblemsReporter;

/**
 * Validates Java SDK powered scopes, which per the current implementation, have extra limitations compared to manually defined
 * ones.
 *
 * @since 4.5.0
 */
public class JavaScopeModelValidator implements ExtensionModelValidator {

  @Override
  public void validate(ExtensionModel model, ProblemsReporter problemsReporter) {
    DslSyntaxResolver dsl = getDefault(model, new SingleExtensionImportTypesStrategy());

    new ExtensionWalker() {

      @Override
      protected void onOperation(HasOperationModels owner, OperationModel model) {
        validateScope(model, problemsReporter, dsl);
      }

      @Override
      protected void onConstruct(HasConstructModels owner, ConstructModel model) {
        validateScope(model, problemsReporter, dsl);
      }
    }.walk(model);
  }

  private void validateScope(ComponentModel model, ProblemsReporter problemsReporter, DslSyntaxResolver dsl) {
    if (isScope(model)) {
      validateNoInlineParameters(model, "Scope", problemsReporter, dsl);
    }
  }
}
