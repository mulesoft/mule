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
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.connection.HasConnectionProviderModels;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionParameterDescriptorModelProperty;

import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;

/**
 * Validates that POJOs are valid for the model
 *
 * @since 4.2
 */
public class PojosModelValidator implements ExtensionModelValidator {

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problemsReporter) {

    if (!isCompiletime(extensionModel)) {
      return;
    }

    new ExtensionWalker() {

      @Override
      protected void onConfiguration(ConfigurationModel model) {
        validateOverridesEqualsAndHashCode(model, problemsReporter);
      }

      @Override
      protected void onConnectionProvider(HasConnectionProviderModels owner, ConnectionProviderModel model) {
        validateOverridesEqualsAndHashCode(model, problemsReporter);
      }

      @Override
      protected void onParameter(ParameterizedModel owner, ParameterGroupModel groupModel, ParameterModel model) {
        validatePojoHasDefaultConstructor(owner, problemsReporter);
      }
    }.walk(extensionModel);
  }

  private void iterateParametersAndValidate(ParameterizedModel model, ProblemsReporter problemsReporter,
                                            TypeValidator<ParameterizedModel, Type, ProblemsReporter> validator) {
    model.getAllParameterModels().forEach(parameterModel -> {

      Optional<ExtensionParameterDescriptorModelProperty> modelProperty =
          parameterModel.getModelProperty(ExtensionParameterDescriptorModelProperty.class);
      if (modelProperty.isPresent()) {
        Type type = modelProperty.get().getExtensionParameter().getType();
        validator.validateType(model, type, problemsReporter);
      }
    });
  }

  private void validateOverridesEqualsAndHashCode(ParameterizedModel model, ProblemsReporter reporter) {
    iterateParametersAndValidate(model, reporter, (parameterizedModel, type, problemReporter) -> {
      ClassInformationAnnotation classInformationAnnotation = type.getClassInformation();
      if (!classInformationAnnotation.isInterface() && !classInformationAnnotation.isAbstract()
          && type.asMetadataType() instanceof ObjectType) {
        Optional<MethodElement> equals = type.getMethod("equals", Object.class);
        Optional<MethodElement> hashCode = type.getMethod("hashCode");
        if (!equals.isPresent() || !hashCode.isPresent()) {
          reporter
              .addError(new Problem(model,
                                    format("Type '%s' must override equals and hashCode",
                                           type.getName())));
        }
      }
    });
  }

  private void validatePojoHasDefaultConstructor(ParameterizedModel owner, ProblemsReporter problemsReporter) {
    iterateParametersAndValidate(owner, problemsReporter, (parameterizedModel, type, problemReporter) -> {
      if (type.isSameType(TypedValue.class)) {
        type = getFirstGenericType(type).orElse(null);
      }
      if (type != null && type.asMetadataType() instanceof ObjectType) {
        ClassInformationAnnotation classInformationAnnotation = type.getClassInformation();
        if (!classInformationAnnotation.isAbstract() && !classInformationAnnotation.isInterface()
            && !classInformationAnnotation.hasDefaultConstructor()) {
          problemsReporter
              .addError(new Problem(parameterizedModel, format("Type '%s' does not have a default constructor", type.getName())));
        }
      }
    });
  }

  private Optional<Type> getFirstGenericType(Type typeWithGenerics) {
    return !CollectionUtils.isEmpty(typeWithGenerics.getGenerics())
        ? Optional.ofNullable(typeWithGenerics.getGenerics().get(0).getConcreteType()) : Optional.empty();
  }

  @FunctionalInterface
  private interface TypeValidator<M, T, PR> {

    void validateType(M model, T type, PR problemsReporter);
  }
}
