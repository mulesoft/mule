/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type;

import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.OAuthParameter;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

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
    return getAnnotatedFields(Parameter.class,
                              OAuthParameter.class,
                              ParameterGroup.class,
                              Connection.class,
                              Config.class)
                                  .stream()
                                  .distinct()
                                  .collect(new ImmutableListCollector<>());
  }

  /**
   * {@inheritDoc}
   */
  default List<ExtensionParameter> getParameterGroups() {
    return getAnnotatedFields(ParameterGroup.class)
        .stream()
        .distinct()
        .collect(new ImmutableListCollector<>());
  }

  /**
   * {@inheritDoc}
   */
  default List<ExtensionParameter> getParametersAnnotatedWith(Class<? extends Annotation> annotationClass) {
    return getParameters().stream()
        .filter(field -> field.getAnnotation(annotationClass).isPresent())
        .distinct()
        .collect(new ImmutableListCollector<>());
  }
}
