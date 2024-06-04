/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.validation;

import static org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver.getDefault;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isScope;

import static java.lang.String.format;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.construct.HasConstructModels;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.dsl.syntax.resolver.SingleExtensionImportTypesStrategy;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.extension.api.property.InfrastructureParameterModelProperty;

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

  /**
   * Validates that the given {@code model} doesn't have parameters defined inline. If found, said parameters will be reported
   * through the {@code problemsReporter}
   *
   * @param model            the model being validated
   * @param kind             the kind of component being validated (Operation, Source, etc)
   * @param problemsReporter a {@link ProblemsReporter}
   * @param dsl              a {@link DslSyntaxResolver}
   */
  public static void validateNoInlineParameters(ParameterizedModel model,
                                                String kind,
                                                ProblemsReporter problemsReporter,
                                                DslSyntaxResolver dsl) {
    model.getParameterGroupModels().stream()
        .forEach(group -> {
          if (group.isShowInDsl()) {
            problemsReporter.addError(new Problem(model,
                                                  format("Invalid parameter group [%s] found in operation [%s], inline groups are not allowed in %s",
                                                         group.getName(), model.getName(), kind)));
          }
          group.getParameterModels().stream()
              // an example of this is error-mappings that are allowed in a scope
              .filter(parameter -> !parameter.getModelProperty(InfrastructureParameterModelProperty.class).isPresent())
              .forEach(parameter -> {
                if (dsl.resolve(parameter).supportsChildDeclaration()) {
                  problemsReporter.addError(new Problem(model,
                                                        format("Invalid parameter [%s] found in group [%s] of operation [%s], "
                                                            + "parameters that allow inline declaration are not allowed in %s. "
                                                            + "Use attribute declaration only for all the parameters.",
                                                               parameter.getName(), group.getName(), model.getName(), kind)));
                }
              });
        });
  }

}
