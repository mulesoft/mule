/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer.model;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.UseConfig;

import com.google.common.collect.ImmutableList;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * A generic contract for any kind of component that is based in a {@link Class} and can define parameters
 *
 * @since 4.0
 */
public interface ParameterizableTypeElement extends Type, WithParameters {

  /**
   * {@inheritDoc}
   */
  default List<ExtensionParameter> getParameters() {
    return ImmutableList.<ExtensionParameter>builder().addAll(getAnnotatedFields(Parameter.class))
        .addAll(getAnnotatedFields(ParameterGroup.class)).addAll(getAnnotatedFields(Connection.class))
        .addAll(getAnnotatedFields(UseConfig.class)).build();
  }

  /**
   * {@inheritDoc}
   */
  default List<ExtensionParameter> getParameterGroups() {
    return copyOf(getAnnotatedFields(ParameterGroup.class));
  }

  /**
   * {@inheritDoc}
   */
  default List<ExtensionParameter> getParametersAnnotatedWith(Class<? extends Annotation> annotationClass) {
    return getParameters().stream().filter(field -> field.getAnnotation(annotationClass).isPresent()).collect(toList());
  }
}
