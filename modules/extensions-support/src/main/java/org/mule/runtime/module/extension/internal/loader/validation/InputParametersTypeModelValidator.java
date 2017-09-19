/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentModelTypeName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldsWithGetters;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.source.SourceCallbackModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.extension.api.declaration.type.TypeUtils;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Validates the types used as:
 *
 * <ul>
 * <li>Parameter of an {@link OperationModel}</li>
 * <li>Parameter of an {@link SourceCallbackModel}</li>
 * </ul>
 *
 * These types should comply that for each parameter they contain, there must exist a getter method for that parameter.
 * The vice versa is also required, that implies the fact that for each getter method, there should exist a field named with the
 * getter convention.
 *
 * @since 4.0
 */
public final class InputParametersTypeModelValidator implements ExtensionModelValidator {

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problems) {
    final Set<Class<?>> validatedTypes = new HashSet<>();
    new IdempotentExtensionWalker() {

      @Override
      public void onOperation(OperationModel model) {
        model.getAllParameterModels()
            .forEach(parameterModel -> validateJavaType(model, parameterModel.getType(), problems, validatedTypes));
      }

      @Override
      public void onSource(SourceModel model) {
        validateCallback(model, model.getSuccessCallback());
        validateCallback(model, model.getErrorCallback());
        validateCallback(model, model.getTerminateCallback());
      }

      private void validateCallback(SourceModel model, Optional<SourceCallbackModel> callback) {
        callback.ifPresent(cb -> cb.getAllParameterModels()
            .forEach(parameterModel -> validateJavaType(model, parameterModel.getType(), problems, validatedTypes)));
      }
    }.walk(extensionModel);

    extensionModel.getSubTypes().forEach(subTypesModel -> getClassForValidation(subTypesModel.getBaseType())
        .filter(validatedTypes::contains).ifPresent(type -> {
          subTypesModel.getSubTypes()
              .forEach(subtype -> validateSubtypesHaveGetters(extensionModel, subtype, problems, validatedTypes));
        }));
  }

  private void validateJavaType(ComponentModel model, MetadataType type, ProblemsReporter problems,
                                Set<Class<?>> validatedTypes) {
    if (type.getMetadataFormat().equals(JAVA)) {
      validateParameterFieldsHaveGetters(model, type, problems, validatedTypes);
    }
  }

  private void validateSubtypesHaveGetters(ExtensionModel extensionModel, MetadataType subtype, ProblemsReporter problems,
                                           Set<Class<?>> validatedTypes) {
    validateType(format("Extension '%s' defines a subtype", extensionModel.getName()), extensionModel, subtype, problems,
                 validatedTypes);
  }

  private void validateParameterFieldsHaveGetters(ComponentModel model, MetadataType parameterMetadataType,
                                                  ProblemsReporter problems, Set<Class<?>> validatedTypes) {
    validateType(format("%s '%s' has an argument", getComponentModelTypeName(model), model.getName()), model,
                 parameterMetadataType, problems, validatedTypes);
  }

  private void validateType(String message, NamedObject namedObject, MetadataType type, ProblemsReporter problems,
                            Set<Class<?>> validatedTypes) {
    getClassForValidation(type).ifPresent(parameterType -> type.accept(new MetadataTypeVisitor() {

      @Override
      public void visitObject(ObjectType objectType) {
        if (validatedTypes.add(parameterType)) {
          Collection<ObjectFieldType> parameters = objectType.getFields();
          Set<String> fieldsWithGetters =
              getFieldsWithGetters(parameterType).stream().map(TypeUtils::getAlias).map(String::toLowerCase).collect(toSet());
          Set<String> parameterWithoutGetters =
              parameters.stream().map(f -> f.getKey().getName().getLocalPart())
                  .filter(fieldName -> !fieldsWithGetters.contains(fieldName.toLowerCase())).collect(toSet());
          if (!parameterWithoutGetters.isEmpty()) {
            problems.addError(new Problem(namedObject,
                                          format("%s of type '%s' which contains fields (%s) that doesn't have the corresponding getter methods or getter methods that doesn't correspond to any of the present fields",
                                                 message, parameterType.getName(),
                                                 parameterWithoutGetters.stream().collect(joining(", ")))));
          }
        }
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        validateType(message, namedObject, arrayType.getType(), problems, validatedTypes);
      }
    }));
  }

  private Optional<Class<Object>> getClassForValidation(MetadataType type) {
    if (!type.getMetadataFormat().equals(JAVA)) {
      return empty();
    }

    return getType(type);
  }
}
