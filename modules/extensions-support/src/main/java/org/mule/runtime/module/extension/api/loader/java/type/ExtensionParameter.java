/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader.java.type;


import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.notification.NotificationEmitter;
import org.mule.runtime.extension.api.runtime.operation.FlowListener;
import org.mule.runtime.extension.api.runtime.parameter.CorrelationInfo;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.process.VoidCompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.extension.api.runtime.source.SourceCompletionCallback;
import org.mule.runtime.extension.api.runtime.source.SourceResult;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;
import org.mule.runtime.extension.api.security.AuthenticationHandler;

import java.lang.reflect.AnnotatedElement;
import java.util.Set;

import javax.lang.model.element.VariableElement;

import com.google.common.collect.ImmutableSet;

/**
 * A contract for any kind of component from which an extension's parameter can be derived
 *
 * @since 4.0
 */
@NoImplement
public interface ExtensionParameter extends WithType, WithAnnotations, NamedObject, WithAlias, WithOwner, WithElement {

  Set<Class<?>> IMPLICIT_ARGUMENT_TYPES = ImmutableSet.<Class<?>>builder()
      .add(Error.class)
      .add(SourceCallbackContext.class)
      .add(CompletionCallback.class)
      .add(VoidCompletionCallback.class)
      .add(SourceCompletionCallback.class)
      .add(MediaType.class)
      .add(AuthenticationHandler.class)
      .add(FlowListener.class)
      .add(StreamingHelper.class)
      .add(SourceResult.class)
      .add(ComponentLocation.class)
      .add(Chain.class)
      .add(CorrelationInfo.class)
      .add(NotificationEmitter.class)
      .add(ExtensionsClient.class)
      .add(RetryPolicyTemplate.class)
      .build();

  /**
   * @return A {@code boolean} indicating whether the parameter should be advertised and added as a {@link ParameterModel} in the
   *         {@link ExtensionModel}
   */
  default boolean shouldBeAdvertised() {
    return !(IMPLICIT_ARGUMENT_TYPES.stream().anyMatch(aClass -> getType().isAssignableTo(aClass))
        || isAnnotatedWith(Config.class) || isAnnotatedWith(Connection.class) || isAnnotatedWith(DefaultEncoding.class));
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
   * @return The {@link AnnotatedElement} form which {@code this} instance was derived
   */
  java.util.Optional<? extends AnnotatedElement> getDeclaringElement();

  @Override
  java.util.Optional<VariableElement> getElement();
}
