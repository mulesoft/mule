/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.springframework.util.ClassUtils.isPrimitiveWrapper;
import org.mule.metadata.api.model.BooleanType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;

import java.util.Objects;

/**
 * Validates that the parameter types are valid
 * </ul>
 *
 * @since 4.0
 */
public final class ParameterTypeModelValidator implements ExtensionModelValidator {

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problemsReporter) {
    new ExtensionWalker() {

      @Override
      protected void onParameter(ParameterizedModel owner, ParameterGroupModel groupModel, ParameterModel model) {
        validateParameterType(model, problemsReporter);
      }
    }.walk(extensionModel);
  }

  private void validateParameterType(ParameterModel parameter, ProblemsReporter problemsReporter) {
    parameter.getType().accept(new MetadataTypeVisitor() {

      @Override
      public void visitObject(ObjectType objectType) {
        if (isMap(objectType)) {
          objectType.getAnnotation(ClassInformationAnnotation.class)
              .filter(classInformation -> !classInformation.getGenericTypes().isEmpty())
              .filter(classInformation -> !Objects.equals(classInformation.getGenericTypes().get(0), String.class.getName()))
              .ifPresent(classInformation -> problemsReporter.addError(new Problem(parameter,
                                                                                   format(
                                                                                          "Parameter '%s' is of type '%s' and its key type is not %s ",
                                                                                          parameter.getName(),
                                                                                          getType(objectType).getName(),
                                                                                          String.class.getName()))));
        }
      }

      @Override
      public void visitBoolean(BooleanType booleanType) {
        if (isPrimitiveWrapper(getType(booleanType))) {
          problemsReporter
              .addError(new Problem(parameter, format("Parameter '%s' is of type '%s'. Use primitive type boolean instead.",
                                                      parameter.getName(), Boolean.class.getName())));
        }
      }
    });
  }
}
