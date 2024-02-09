/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.validation;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.TypeGeneric;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

/**
 * Validates that all {@link OperationModel operations} specify a valid return type.
 * <p>
 * A return type is considered valid when it's not {@code null} and not a {@link CoreEvent}
 *
 * @since 4.0
 */
public class OperationReturnTypeModelValidator implements ExtensionModelValidator {

  private static final String MISSING_GENERICS_ERROR_MESSAGE =
      "Operation [%s] in extension [%s] has a '%s' as return type but their generics "
          +
          "were not provided. Please provide the Payload and Attributes generics.";

  private static final String INVALID_GENERICS_ERROR_MESSAGE =
      "Operation [%s] in extension [%s] has a '%s' as return type with Void type for output but non Void type for attributes";

  private final List<Class<?>> illegalReturnTypes = ImmutableList.of(CoreEvent.class, Message.class);

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problemsReporter) {
    new IdempotentExtensionWalker() {

      @Override
      protected void onOperation(OperationModel operationModel) {
        if (operationModel.getOutput() == null || operationModel.getOutput().getType() == null) {
          throw missingReturnTypeException(extensionModel, operationModel);
        }

        operationModel.getModelProperty(ExtensionOperationDescriptorModelProperty.class)
            .ifPresent(mp -> {
              MethodElement<? extends Type> operationMethod = mp.getOperationElement();
              Type returnType = mp.getOperationElement().getReturnType();
              validateNonBlockingCallback(operationMethod, problemsReporter, operationModel, extensionModel);
              validateResultReturnType(returnType, problemsReporter, operationModel, extensionModel);
              validateMessageCollectionsReturnType(returnType, problemsReporter, operationModel, extensionModel);
              validateForbiddenTypesReturnType(returnType, problemsReporter, operationModel, extensionModel);
            });


      }
    }.walk(extensionModel);
  }

  private void validateForbiddenTypesReturnType(Type returnType, ProblemsReporter problemsReporter, OperationModel operationModel,
                                                ExtensionModel extensionModel) {
    illegalReturnTypes.stream()
        .filter(returnType::isAssignableTo)
        .findFirst()
        .ifPresent(forbiddenType -> problemsReporter.addError(new Problem(operationModel, format(
                                                                                                 "Operation '%s' in Extension '%s' specifies '%s' as a return type. Operations are "
                                                                                                     + "not allowed to return objects of that type",
                                                                                                 operationModel.getName(),
                                                                                                 extensionModel.getName(),
                                                                                                 forbiddenType.getName()))));
  }

  private void validateNonBlockingCallback(MethodElement<? extends Type> operationMethod, ProblemsReporter problemsReporter,
                                           OperationModel operationModel, ExtensionModel extensionModel) {
    operationMethod.getParameters().stream()
        .filter(JavaExtensionModelParserUtils::isCompletionCallbackParameter)
        .findFirst().ifPresent(extensionParameter -> {
          List<TypeGeneric> generics = extensionParameter.getType().getGenerics();
          List<Type> superTypeGenerics = extensionParameter.getType().getSuperTypeGenerics(CompletionCallback.class);
          if (!generics.isEmpty()) {
            validateGenerics(extensionModel, extensionParameter, operationModel, genericToConcreteTypeList(generics),
                             CompletionCallback.class, problemsReporter);
          } else if (!superTypeGenerics.isEmpty()) {
            validateGenerics(extensionModel, extensionParameter, operationModel, superTypeGenerics, CompletionCallback.class,
                             problemsReporter);
          } else {
            problemsReporter
                .addError(new Problem(extensionParameter, format(MISSING_GENERICS_ERROR_MESSAGE, operationModel.getName(),
                                                                 extensionModel.getName(), CompletionCallback.class.getName())));
          }
        });
  }

  private void validateResultReturnType(Type returnType, ProblemsReporter problemsReporter, OperationModel operationModel,
                                        ExtensionModel extensionModel) {
    if (returnType.isAssignableTo(Result.class)) {
      List<TypeGeneric> generics = returnType.getGenerics();
      if (generics.isEmpty()) {
        problemsReporter.addError(new Problem(operationModel, format(MISSING_GENERICS_ERROR_MESSAGE, operationModel.getName(),
                                                                     extensionModel.getName(), Result.class)));
      } else {
        validateGenerics(extensionModel, operationModel, operationModel, genericToConcreteTypeList(generics), Result.class,
                         problemsReporter);
      }
    }
  }

  private void validateMessageCollectionsReturnType(Type returnType, ProblemsReporter problemsReporter,
                                                    OperationModel operationModel, ExtensionModel extensionModel) {
    if (returnType.isAssignableTo(Collection.class)) {
      List<TypeGeneric> generics = returnType.getGenerics();
      if (!generics.isEmpty()) {
        Type concreteType = generics.get(0).getConcreteType();
        if (concreteType.isAssignableTo(Result.class)) {
          List<TypeGeneric> concreteTypeGenerics = concreteType.getGenerics();
          if (concreteTypeGenerics.isEmpty()) {
            problemsReporter.addError(new Problem(operationModel, format(MISSING_GENERICS_ERROR_MESSAGE, operationModel.getName(),
                                                                         extensionModel.getName(), Result.class)));
          } else {
            validateGenerics(extensionModel, operationModel, operationModel, genericToConcreteTypeList(concreteTypeGenerics),
                             Result.class,
                             problemsReporter);
          }
        }
      }
    }
  }

  private IllegalModelDefinitionException missingReturnTypeException(ExtensionModel model, OperationModel operationModel) {
    throw new IllegalOperationModelDefinitionException(format("Operation '%s' in Extension '%s' is missing a return type",
                                                              operationModel.getName(), model.getName()));
  }

  private void validateGenerics(ExtensionModel extensionModel, NamedObject namedObject, OperationModel operationModel,
                                List<Type> generics, Class returnType, ProblemsReporter problemsReporter) {
    if (generics.get(0).isSameType(Void.class) && !generics.get(1).isSameType(Void.class)) {
      problemsReporter.addWarning(new Problem(namedObject, format(INVALID_GENERICS_ERROR_MESSAGE, operationModel.getName(),
                                                                  extensionModel.getName(), returnType.getName())));
    }
  }

  private List<Type> genericToConcreteTypeList(List<TypeGeneric> generics) {
    return generics.stream().map(t -> t.getConcreteType()).collect(toList());
  }
}
