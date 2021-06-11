/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getDefaultValue;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.tls.TlsContextFactory;
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
import org.mule.sdk.api.runtime.parameter.Literal;
import org.mule.sdk.api.runtime.parameter.ParameterResolver;

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
      .add(org.mule.sdk.api.runtime.source.SourceCallbackContext.class)
      .add(CompletionCallback.class)
      .add(org.mule.sdk.api.runtime.process.CompletionCallback.class)
      .add(VoidCompletionCallback.class)
      .add(org.mule.sdk.api.runtime.process.VoidCompletionCallback.class)
      .add(SourceCompletionCallback.class)
      .add(org.mule.sdk.api.runtime.source.SourceCompletionCallback.class)
      .add(MediaType.class)
      .add(AuthenticationHandler.class)
      .add(org.mule.sdk.api.security.AuthenticationHandler.class)
      .add(FlowListener.class)
      .add(org.mule.sdk.api.runtime.operation.FlowListener.class)
      .add(StreamingHelper.class)
      .add(org.mule.sdk.api.runtime.streaming.StreamingHelper.class)
      .add(SourceResult.class)
      .add(org.mule.sdk.api.runtime.source.SourceResult.class)
      .add(ComponentLocation.class)
      .add(Chain.class)
      .add(org.mule.sdk.api.runtime.route.Chain.class)
      .add(CorrelationInfo.class)
      .add(org.mule.sdk.api.runtime.parameter.CorrelationInfo.class)
      .add(NotificationEmitter.class)
      .add(org.mule.sdk.api.notification.NotificationEmitter.class)
      .add(ExtensionsClient.class)
      .add(org.mule.sdk.api.client.ExtensionsClient.class)
      .add(RetryPolicyTemplate.class)
      .build();

  Set<String> IMPLICIT_ARGUMENT_PACKAGES = ImmutableSet.<String>builder()
      .add("org.mule.sdk.api")
      .build();

  Set<Class<?>> EXPLICIT_MULE_ARGUMENT_TYPES = ImmutableSet.<Class<?>>builder()
      .add(TypedValue.class)
      .add(ParameterResolver.class)
      .add(org.mule.runtime.extension.api.runtime.parameter.ParameterResolver.class)
      .add(org.mule.runtime.extension.api.runtime.parameter.Literal.class)
      .add(Literal.class)
      .add(TlsContextFactory.class)
      .add(ObjectStore.class)
      .build();

  /**
   * @return A {@code boolean} indicating whether the parameter should be advertised and added as a {@link ParameterModel} in the
   *         {@link ExtensionModel}
   */
  default boolean shouldBeAdvertised() {
    return !(IMPLICIT_ARGUMENT_TYPES.stream().anyMatch(aClass -> getType().isAssignableTo(aClass))
        || isAnnotatedWith(Config.class) || isAnnotatedWith(org.mule.sdk.api.annotation.param.Config.class)
        || isAnnotatedWith(org.mule.sdk.api.annotation.param.Connection.class) || isAnnotatedWith(Connection.class)
        || isAnnotatedWith(DefaultEncoding.class) || isAnnotatedWith(org.mule.sdk.api.annotation.param.DefaultEncoding.class))
        && (!IMPLICIT_ARGUMENT_PACKAGES.stream().anyMatch(packageName -> getType().getTypeName().startsWith(packageName))
            || EXPLICIT_MULE_ARGUMENT_TYPES.stream().anyMatch(aClass -> getType().isAssignableTo(aClass)));
  }

  /**
   * @return A {@code boolean} indicating whether the parameter is a required or not
   */
  default boolean isRequired() {
    return !(isAnnotatedWith(Optional.class) || isAnnotatedWith(org.mule.sdk.api.annotation.param.Optional.class));
  }

  /**
   * @return The {@link java.util.Optional} default value of the operation
   */
  default java.util.Optional<String> defaultValue() {
    final java.util.Optional<org.mule.sdk.api.annotation.param.Optional> sdkAnnotation =
        getAnnotation(org.mule.sdk.api.annotation.param.Optional.class);
    if (sdkAnnotation.isPresent()) {
      return getDefaultValue(sdkAnnotation.get());
    }
    final java.util.Optional<Optional> annotation = getAnnotation(Optional.class);
    if (annotation.isPresent()) {
      return getDefaultValue(annotation.get());
    }
    return java.util.Optional.empty();
  }

  /**
   * @return The {@link AnnotatedElement} form which {@code this} instance was derived
   */
  java.util.Optional<? extends AnnotatedElement> getDeclaringElement();

  @Override
  java.util.Optional<VariableElement> getElement();
}
