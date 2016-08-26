/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldsWithGetterAndSetters;

import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.fluent.BaseDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OperationDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.runtime.module.extension.internal.introspection.ParameterGroup;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ExtensionParameter;
import org.mule.runtime.module.extension.internal.introspection.describer.model.FieldElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ParameterElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.Type;
import org.mule.runtime.module.extension.internal.introspection.describer.model.runtime.FieldWrapper;
import org.mule.runtime.module.extension.internal.introspection.describer.model.runtime.MethodWrapper;
import org.mule.runtime.module.extension.internal.introspection.describer.model.runtime.ParameterizableTypeWrapper;
import org.mule.runtime.module.extension.internal.model.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.util.IdempotentDeclarationWalker;

import java.lang.reflect.Field;
import java.util.List;

/**
 * {@link ModelEnricher} implementation that walks through a {@link ExtensionDeclaration} and looks for annotated component
 * parameters with {@link org.mule.runtime.extension.api.annotation.ParameterGroup}
 * <p>
 * The containers of the parameters considered as parameter groups a {@link ParameterGroupModelProperty} will be added describing
 * the structure of this parameter group.
 *
 * @since 4.0
 */
public class ParameterGroupModelEnricher implements ModelEnricher {

  @Override
  public void enrich(DescribingContext describingContext) {
    new IdempotentDeclarationWalker() {

      @Override
      public void onSource(SourceDeclaration declaration) {
        enrich(declaration);
      }

      @Override
      public void onConfiguration(ConfigurationDeclaration declaration) {
        enrich(declaration);
      }

      @Override
      protected void onConnectionProvider(ConnectionProviderDeclaration declaration) {
        enrich(declaration);
      }

      @Override
      protected void onOperation(OperationDeclaration declaration) {
        enrich(declaration);
      }
    }.walk(describingContext.getExtensionDeclarer().getDeclaration());
  }

  /**
   * For each {@link org.mule.runtime.extension.api.annotation.ParameterGroup} annotated parameter the
   * {@link ParameterGroupModelProperty} will be added
   *
   * @param baseDeclaration declaration of a Source, Configuration or ConnectionProvider
   */
  private void enrich(BaseDeclaration<?> baseDeclaration) {
    baseDeclaration.getModelProperty(ImplementingTypeModelProperty.class).ifPresent(implementingType -> {
      final Class<?> type = implementingType.getType();
      final List<FieldElement> parameterGroups =
          new ParameterizableTypeWrapper(type).getAnnotatedFields(org.mule.runtime.extension.api.annotation.ParameterGroup.class);
      if (!parameterGroups.isEmpty()) {
        baseDeclaration.addModelProperty(new ParameterGroupModelProperty(parameterGroups.stream().map(this::toParameterGroup)
            .collect(toList())));
      }
    });
  }

  /**
   * For each {@link org.mule.runtime.extension.api.annotation.ParameterGroup} annotated parameter the
   * {@link ParameterGroupModelProperty} will be added
   *
   * @param operationDeclaration declaration of an Operation
   */
  private void enrich(OperationDeclaration operationDeclaration) {
    operationDeclaration.getModelProperty(ImplementingMethodModelProperty.class).ifPresent(method -> {
      final MethodWrapper methodWrapper = new MethodWrapper(method.getMethod());
      final List<ExtensionParameter> parameterGroups = methodWrapper.getParameterGroups();
      if (!parameterGroups.isEmpty()) {
        operationDeclaration.addModelProperty(new ParameterGroupModelProperty(parameterGroups.stream()
            .map(param -> (ParameterElement) param).map(this::toParameterGroup).collect(toList())));
      }
    });
  }

  /**
   * Given a {@link ParameterElement} representing a {@link java.lang.reflect.Parameter} based parameter, introspect it and
   * returns the {@link ParameterGroup} of this field.
   *
   * @param parameterElement Wrapper of the parameter based parameter
   * @return A {@link ParameterGroup} representing the structure of the given parameter
   */
  private ParameterGroup toParameterGroup(ParameterElement parameterElement) {
    final Type paramGroupType = parameterElement.getType();
    final ParameterGroup parameterGroup =
        new ParameterGroup<>(paramGroupType.getDeclaringClass(), parameterElement.getParameter());
    populateParameterGroup(parameterGroup, paramGroupType);

    return parameterGroup;
  }

  /**
   * Given a {@link FieldElement} representing a {@link Field} based parameter, introspect it and returns the
   * {@link ParameterGroup} of this field.
   *
   * @param fieldElement Wrapper of the field based parameter
   * @return A {@link ParameterGroup} representing the structure of the given parameter
   */
  private ParameterGroup toParameterGroup(FieldElement fieldElement) {
    final Type paramGroupType = fieldElement.getType();
    final Field field = fieldElement.getField();
    field.setAccessible(true);
    final ParameterGroup parameterGroup = new ParameterGroup<>(paramGroupType.getDeclaringClass(), field);
    populateParameterGroup(parameterGroup, paramGroupType);

    return parameterGroup;
  }

  /**
   * Given a empty {@link ParameterGroup} and a {@link Type} representing the type of the Parameter Group, populates it with all
   * the parameters that contains.
   * <p>
   * The parameters are:
   * <p>
   * <ul>
   * <li>The ones that are annotated with {@link Parameter}</li>
   * <li>The ones that have getters and setters</li>
   * </ul>
   * <p>
   * The above conditions are exclusive, so if a field is annotated with {@link Parameter} no field that just have getters and
   * setters will be considered.
   *
   * @param parameterGroup {@link ParameterGroup} to populate
   * @param paramGroupType type of the parameter group
   */
  private void populateParameterGroup(ParameterGroup parameterGroup, Type paramGroupType) {
    List<FieldElement> annotatedFields = paramGroupType.getAnnotatedFields(Parameter.class);
    if (annotatedFields.isEmpty()) {
      annotatedFields =
          getFieldsWithGetterAndSetters(paramGroupType.getDeclaringClass()).stream().map(FieldWrapper::new).collect(toList());
    }

    annotatedFields.forEach(field -> parameterGroup.addParameter(field.getField()));

    final List<FieldElement> parameterGroups =
        paramGroupType.getAnnotatedFields(org.mule.runtime.extension.api.annotation.ParameterGroup.class);
    if (!parameterGroups.isEmpty()) {
      parameterGroup.addModelProperty(new ParameterGroupModelProperty(parameterGroups.stream().map(this::toParameterGroup)
          .collect(toList())));
    }
  }
}
