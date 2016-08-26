/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer.model;


import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.Named;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;

import com.google.common.collect.ImmutableSet;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * A contract for any kind of component from which an extension's parameter can be derived
 *
 * @since 4.0
 */
public interface ExtensionParameter extends WithType, WithAnnotations, Named, WithAlias, WithOwner {

  Set<Class<?>> IMPLICIT_ARGUMENT_TYPES = ImmutableSet.<Class<?>>builder().add(MuleEvent.class).add(MuleMessage.class)
      .add(org.mule.runtime.core.api.MuleMessage.class).build();

  /**
   * @return A {@code boolean} indicating whether the parameter should be advertised and added as a {@link ParameterModel} in the
   *         {@link ExtensionModel}
   */
  default boolean shouldBeAdvertised() {
    return !(IMPLICIT_ARGUMENT_TYPES.contains(getType().getDeclaringClass())
        || (isAnnotatedWith(UseConfig.class) || isAnnotatedWith(Connection.class)) || isAnnotatedWith(ParameterGroup.class));
  }

  /**
   * @return A {@code boolean} indicating whether the parameter is a required or not
   */
  default boolean isRequired() {
    return !(isAnnotatedWith(Optional.class));
  }

  /**
   * @return The {@link java.util.Optional} default value of the operation
   */
  default java.util.Optional<String> defaultValue() {
    java.util.Optional<String> optionalDefaultValue = java.util.Optional.empty();
    final java.util.Optional<Optional> annotation = getAnnotation(Optional.class);
    if (annotation.isPresent()) {
      final Optional optionalAnnotation = annotation.get();
      final String defaultValue = optionalAnnotation.defaultValue();
      if (!defaultValue.equals(Optional.NULL)) {
        optionalDefaultValue = java.util.Optional.of(defaultValue);
      }
    }
    return optionalDefaultValue;
  }

  /**
   * @return A {@code boolean} indicating whether the parameter is based as a {@link Field}
   */
  boolean isFieldBased();
}
