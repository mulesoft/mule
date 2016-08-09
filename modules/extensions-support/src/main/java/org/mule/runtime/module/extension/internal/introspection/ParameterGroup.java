/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection;

import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotation;
import org.mule.runtime.extension.api.annotation.ExclusiveOptionals;
import org.mule.runtime.extension.api.introspection.EnrichableModel;
import org.mule.runtime.extension.api.introspection.ModelProperty;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.module.extension.internal.model.property.ParameterGroupModelProperty;

import com.google.common.collect.ImmutableSet;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A metadata class that groups a set of parameters together. It caches reflection objects necessary for handling those parameters
 * so that introspection is not executed every time, resulting in a performance gain.
 * <p>
 * Because groups can be nested, this class also implements {@link EnrichableModel}, allowing for this group to have a
 * {@link ParameterGroupModelProperty} which describes the nested group.
 * <p>
 * To decouple this class from the representation model (which depending on the context could be a {@link ExtensionDeclaration} or
 * an actual {@link ParameterModel}, this class references parameters by name
 *
 * @since 3.7.0
 */
public class ParameterGroup<T> implements EnrichableModel {

  /**
   * The type of the pojo which implements the group
   */
  private final Class<?> type;

  /**
   * The member in which the generated value of {@link #type} is to be assigned. For {@link ParameterGroup} used as fields of a
   * class, this container should be parameterized as a {@link Field}. And if it is used as an argument of an operation it should
   * the corresponding {@link Method}'s {@link java.lang.reflect.Parameter}.
   */
  private final T container;

  /**
   * A {@link Map} in which the keys are parameter names and the values are their corresponding setter methods
   */
  private final Set<Field> parameters = new HashSet<>();

  /**
   * The model properties per the {@link EnrichableModel} interface
   */
  private Map<Class<? extends ModelProperty>, ModelProperty> modelProperties = new HashMap<>();


  public ParameterGroup(Class<?> type, T container) {
    checkArgument(type != null, "type cannot be null");
    checkArgument(container != null, "container cannot be null");

    this.type = type;
    this.container = container;
  }

  /**
   * @return parameterized container of the {@link ParameterGroup}
   */
  public T getContainer() {
    return container;
  }

  /**
   * Adds a parameter to the group
   *
   * @param field the parameter's {@link Field}
   * @return {@code this}
   */
  public ParameterGroup addParameter(Field field) {
    field.setAccessible(true);
    parameters.add(field);
    return this;
  }

  public Class<?> getType() {
    return type;
  }

  public Set<Field> getParameters() {
    return ImmutableSet.copyOf(parameters);
  }

  /**
   *
   * @return set of optional {@link Field}s
   */
  public Set<Field> getOptionalParameters() {
    return parameters.stream()
        .filter(f -> f.getAnnotation(org.mule.runtime.extension.api.annotation.param.Optional.class) != null)
        .collect(Collectors.toSet());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <G extends ModelProperty> Optional<G> getModelProperty(Class<G> propertyType) {
    return Optional.ofNullable((G) modelProperties.get(propertyType));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<ModelProperty> getModelProperties() {
    return ImmutableSet.copyOf(modelProperties.values());
  }

  public void addModelProperty(ModelProperty modelProperty) {
    checkArgument(modelProperty != null, "Cannot add a null model property");
    modelProperties.put(modelProperty.getClass(), modelProperty);
  }

  /**
   * Whether the class is annotated with {@link ExclusiveOptionals} or not
   */
  public boolean hasExclusiveOptionals() {
    return getAnnotation(type, ExclusiveOptionals.class) != null;
  }

  /**
   * Whether the class is annotated with {@link ExclusiveOptionals} and {@link ExclusiveOptionals#isOneRequired()} ()} is set
   */
  public boolean isOneRequired() {
    ExclusiveOptionals annotation = getAnnotation(type, ExclusiveOptionals.class);
    return annotation != null ? annotation.isOneRequired() : false;
  }
}
