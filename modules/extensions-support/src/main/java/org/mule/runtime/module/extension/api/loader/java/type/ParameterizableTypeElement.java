/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import static java.util.stream.Collectors.toList;

import org.mule.api.annotation.NoImplement;
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
@NoImplement
public interface ParameterizableTypeElement extends Type, WithParameters {

  /**
   * {@inheritDoc}
   */
  default List<ExtensionParameter> getParameters() {
    return getAnnotatedFields(Parameter.class,
                              org.mule.sdk.api.annotation.param.Parameter.class,
                              OAuthParameter.class,
                              org.mule.sdk.api.annotation.connectivity.oauth.OAuthParameter.class,
                              ParameterGroup.class,
                              org.mule.sdk.api.annotation.param.ParameterGroup.class,
                              Connection.class,
                              org.mule.sdk.api.annotation.param.Connection.class,
                              org.mule.sdk.api.annotation.param.Config.class,
                              Config.class)
                                  .stream()
                                  .distinct()
                                  .collect(toList());
  }

  /**
   * {@inheritDoc}
   */
  default List<ExtensionParameter> getParameterGroups() {
    return getAnnotatedFields(ParameterGroup.class, org.mule.sdk.api.annotation.param.ParameterGroup.class)
        .stream()
        .distinct()
        .collect(toList());
  }

  /**
   * {@inheritDoc}
   */
  default List<ExtensionParameter> getParametersAnnotatedWith(Class<? extends Annotation> annotationClass) {
    return getParameters().stream()
        .filter(field -> field.getAnnotation(annotationClass).isPresent())
        .distinct()
        .collect(toList());
  }
}
