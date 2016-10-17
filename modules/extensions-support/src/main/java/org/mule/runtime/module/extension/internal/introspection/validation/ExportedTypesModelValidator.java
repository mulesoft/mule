/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.declaration.type.TypeUtils;
import org.mule.runtime.module.extension.internal.exception.IllegalParameterModelDefinitionException;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getComponentModelTypeName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldsWithGetters;

/**
 * Validates that the exported types used as:
 *
 * <ul>
 * <li>Return type of an {@link OperationModel}</li>
 * <li>Parameter of an {@link OperationModel}</li>
 * <li>Return type of an {@link SourceModel}</li>
 * </ul>
 *
 * These exported types should comply that for each parameter they contain, there must exist a getter method for that parameter.
 * The viceversa is also required, that implies the fact that for each getter method, there should exist a field named with the
 * getter convention.
 *
 * @since 4.0
 */
public final class ExportedTypesModelValidator implements ModelValidator {

  @Override
  public void validate(ExtensionModel extensionModel) throws IllegalModelDefinitionException {
    new IdempotentExtensionWalker() {

      @Override
      public void onOperation(OperationModel model) {
        model.getParameterModels().stream()
            .forEach(parameterModel -> validateJavaType(model, parameterModel.getType()));

        validateJavaType(model, model.getOutput().getType());
      }

      @Override
      public void onSource(SourceModel model) {
        validateJavaType(model, model.getOutput().getType());
      }
    }.walk(extensionModel);
  }

  private void validateJavaType(ComponentModel model, MetadataType type) {
    if (type.getMetadataFormat().equals(JAVA)) {
      validateParameterFieldsHaveGetters(model, type);
    }
  }

  private void validateParameterFieldsHaveGetters(ComponentModel model, MetadataType parameterMetadataType) {
    String componentTypeName = getComponentModelTypeName(model);
    Class<?> parameterType = getType(parameterMetadataType);
    parameterMetadataType.accept(new MetadataTypeVisitor() {

      @Override
      public void visitObject(ObjectType objectType) {
        Collection<ObjectFieldType> parameters = objectType.getFields();
        Set<String> fieldsWithGetters =
            getFieldsWithGetters(parameterType).stream().map(TypeUtils::getAlias).collect(Collectors.toSet());
        Set<String> parameterWithoutGetters =
            parameters.stream().map(f -> f.getKey().getName().getLocalPart())
                .filter(fieldName -> !fieldsWithGetters.contains(fieldName)).collect(toSet());
        if (!parameterWithoutGetters.isEmpty()) {
          throw new IllegalParameterModelDefinitionException(format("%s '%s' has an argument or return type of type '%s' which contains fields (%s) that doesn't have the corresponding getter methods or getter methods that doesn't correspond to any of the present fields",
                                                                    componentTypeName, model.getName(),
                                                                    parameterType.getName(),
                                                                    parameterWithoutGetters.stream()
                                                                        .collect(joining(", "))));
        }
      }
    });
  }
}
