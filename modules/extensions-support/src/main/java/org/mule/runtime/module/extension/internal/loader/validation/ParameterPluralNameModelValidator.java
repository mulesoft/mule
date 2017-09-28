/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import static org.mule.runtime.api.meta.ExpressionSupport.REQUIRED;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentModelTypeName;
import static org.mule.runtime.extension.api.util.NameUtils.singularize;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.function.FunctionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;

/**
 * Validates that the classes through which parameter groups are implemented are valid
 * </ul>
 *
 * @since 4.0
 */
public final class ParameterPluralNameModelValidator implements ExtensionModelValidator {

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problemsReporter) {
    new ExtensionWalker() {

      @Override
      public void onParameter(ParameterizedModel owner, ParameterGroupModel groupModel, ParameterModel model) {
        validateParameterIsPlural(model, owner, problemsReporter);
      }
    }.walk(extensionModel);
  }

  private void validateParameterIsPlural(final ParameterModel parameterModel,
                                         ParameterizedModel owner,
                                         ProblemsReporter problemsReporter) {
    if (!(owner instanceof FunctionModel || parameterModel.getExpressionSupport().equals(REQUIRED))) {
      ParameterDslConfiguration dslConfiguration = parameterModel.getDslConfiguration();
      if (dslConfiguration.allowsInlineDefinition() || dslConfiguration.allowTopLevelDefinition())
        parameterModel.getType().accept(new MetadataTypeVisitor() {

          @Override
          public void visitArrayType(ArrayType arrayType) {
            if (parameterModel.getName().equals(singularize(parameterModel.getName()))) {
              problemsReporter
                  .addError(new Problem(parameterModel,
                                        format("Parameter '%s' in the %s '%s' is a collection and its name should be plural",
                                               parameterModel.getName(), getComponentModelTypeName(owner), owner.getName())));
            }
          }
        });
    }
  }
}
