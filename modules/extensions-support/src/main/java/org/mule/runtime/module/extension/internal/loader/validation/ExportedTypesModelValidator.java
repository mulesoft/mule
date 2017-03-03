/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentModelTypeName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldsWithGettersAndSetters;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceCallbackModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.extension.api.declaration.type.TypeUtils;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Validates that the exported types used as:
 *
 * <ul>
 * <li>Parameters of an {@link OperationModel}</li>
 * <li>Parameters of a {@link SourceCallbackModel}</li>
 * </ul>
 *
 * These exported types should comply that for each parameter they contain, there must exist a getter method for that parameter.
 * The vice versa is also required, that implies the fact that for each getter method, there should exist a field named with the
 * getter convention.
 *
 * @since 4.0
 */
public final class ExportedTypesModelValidator implements ExtensionModelValidator {

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problems) {
    new IdempotentExtensionWalker() {

      @Override
      public void onOperation(OperationModel model) {
        validateParametersTypes(model, model.getAllParameterModels(), problems);
      }

      @Override
      public void onSource(SourceModel model) {
        model.getSuccessCallback()
            .ifPresent(callback -> validateParametersTypes(model, callback.getAllParameterModels(), problems));
        model.getErrorCallback()
            .ifPresent(callback -> validateParametersTypes(model, callback.getAllParameterModels(), problems));
      }

    }.walk(extensionModel);
  }

  private void validateParametersTypes(ParameterizedModel model, List<ParameterModel> parameters, ProblemsReporter problems) {
    parameters.forEach(parameterModel -> validateJavaType(model, parameterModel.getType(), problems));
  }

  private void validateJavaType(ParameterizedModel model, MetadataType type, ProblemsReporter problems) {
    if (type.getMetadataFormat().equals(JAVA)) {
      validateParameterFieldsHaveGetters(model, type, problems);
    }
  }

  private void validateParameterFieldsHaveGetters(ParameterizedModel model, MetadataType parameterMetadataType,
                                                  ProblemsReporter problems) {
    if (!parameterMetadataType.getMetadataFormat().equals(JAVA)) {
      return;
    }

    String componentTypeName = getComponentModelTypeName(model);
    Class<?> parameterType = getType(parameterMetadataType);
    parameterMetadataType.accept(new MetadataTypeVisitor() {

      @Override
      public void visitObject(ObjectType objectType) {
        Collection<ObjectFieldType> parameters = objectType.getFields();
        Set<String> fieldsWithGettersAndSetters =
            getFieldsWithGettersAndSetters(parameterType).stream().map(TypeUtils::getAlias).collect(toSet());
        Set<String> parameterWithoutGettersAndSetters =
            parameters.stream().map(f -> f.getKey().getName().getLocalPart())
                .filter(fieldName -> !fieldsWithGettersAndSetters.contains(fieldName)).collect(toSet());
        if (!parameterWithoutGettersAndSetters.isEmpty()) {
          problems
              .addError(new Problem(model,
                                    format("%s '%s' has an argument of type '%s' which contains fields (%s) that doesn't have the corresponding getter and setter methods or the getter and setter methods doesn't correspond to any of the present fields",
                                           componentTypeName, model.getName(),
                                           parameterType.getName(),
                                           parameterWithoutGettersAndSetters.stream()
                                               .collect(joining(", ")))));
        }
      }
    });
  }
}
