/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import static org.mule.runtime.module.extension.internal.loader.validation.ModelValidationUtils.isCompiletime;

import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionParameterDescriptorModelProperty;

import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;

public class HasDefaultConstructorModelValidator implements ExtensionModelValidator {

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problemsReporter) {
    if (!isCompiletime(extensionModel)) {
      return;
    }
    new ExtensionWalker() {

      @Override
      protected void onParameter(ParameterizedModel owner, ParameterGroupModel groupModel, ParameterModel model) {
        validatePojoHasDefaultConstructor(owner, problemsReporter);
      }
    }.walk(extensionModel);

  }

  private void validatePojoHasDefaultConstructor(ParameterizedModel owner, ProblemsReporter problemsReporter) {
    owner.getAllParameterModels().forEach(parameterModel -> {
      Optional<ExtensionParameterDescriptorModelProperty> modelProperty =
          parameterModel.getModelProperty(ExtensionParameterDescriptorModelProperty.class);
      if (modelProperty.isPresent()) {
        Type type = modelProperty.get().getExtensionParameter().getType();
        if (TypedValue.class.getName().equals(type.getClassInformation().getClassname())) {
          type = getFirstGenericType(type).orElse(null);
        }
        if (type != null && type.asMetadataType() instanceof ObjectType) {
          ClassInformationAnnotation classInformationAnnotation = type.getClassInformation();
          if (!classInformationAnnotation.isAbstract() && !classInformationAnnotation.isInterface()
              && !classInformationAnnotation.hasDefaultConstructor()) {
            problemsReporter
                .addError(new Problem(parameterModel, format("Type '%s' does not have a default constructor", type.getName())));
          }
        }
      }
    });
  }

  private Optional<Type> getFirstGenericType(Type typeWithGenerics) {
    return !CollectionUtils.isEmpty(typeWithGenerics.getGenerics())
        ? Optional.ofNullable(typeWithGenerics.getGenerics().get(0).getConcreteType()) : Optional.empty();
  }
}
