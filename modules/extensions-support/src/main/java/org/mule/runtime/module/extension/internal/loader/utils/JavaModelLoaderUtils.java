/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.loader.utils;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.extension.api.annotation.source.EmitsResponse;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.runtime.extension.api.runtime.route.Route;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.SourceElement;

import java.io.InputStream;
import java.util.List;

public class JavaModelLoaderUtils {

  public static boolean isScope(MethodElement methodElement) {
    return methodElement.getParameters().stream().anyMatch(JavaModelLoaderUtils::isProcessorChain);
  }

  /**
   * @param methodElement an element representing an operation
   * @return whether the operation is a router
   */
  public static boolean isRouter(MethodElement methodElement) {
    return !getRoutes(methodElement).isEmpty();
  }

  /**
   * @param methodElement an element representing an operation
   * @return a list with the method parameters which represent a {@link Route}
   * @since 4.5.0
   */
  public static List<ExtensionParameter> getRoutes(MethodElement methodElement) {
    return methodElement.getParameters().stream()
        .filter(JavaModelLoaderUtils::isRoute)
        .collect(toList());
  }

  /**
   * @param method a method element
   * @return whether the given {@code method} defines a non-blocking operation
   */
  public static boolean isNonBlocking(MethodElement method) {
    return !getCompletionCallbackParameters(method).isEmpty();
  }

  /**
   * @param method an element representing an operation
   * @return a list with the method parameters which represent a {@link CompletionCallback}
   * @since 4.5.0
   */
  public static List<ExtensionParameter> getCompletionCallbackParameters(MethodElement method) {
    return method.getParameters().stream()
        .filter(p -> p.getType().isAssignableTo(CompletionCallback.class) ||
            p.getType().isAssignableTo(org.mule.sdk.api.runtime.process.CompletionCallback.class))
        .collect(toList());
  }

  /**
   * @param parameter
   * @return whether the given {@code parameter} represents a chain
   */
  public static boolean isProcessorChain(ExtensionParameter parameter) {
    return parameter.getType().isAssignableTo(Chain.class)
        || parameter.getType().isAssignableTo(org.mule.sdk.api.runtime.route.Chain.class);
  }

  /**
   * @param type a {@link MetadataType}
   * @return whether the given {@code type} represents an {@link InputStream} or not
   */
  public static boolean isInputStream(MetadataType type) {
    return isAssignableFrom(type, InputStream.class);
  }

  /**
   * @param metadataType a metadata type
   * @param type         a class
   * @return whether the {@code metadataType} is derived from a java class which is assignable from the {@code type}
   */
  private static boolean isAssignableFrom(MetadataType metadataType, Class<?> type) {
    return getType(metadataType).map(clazz -> type.isAssignableFrom(clazz)).orElse(false);
  }

  /**
   * @param parameter a parameter
   * @return whether the given parameter represents a route
   */
  public static boolean isRoute(ExtensionParameter parameter) {
    return parameter.getType().isAssignableTo(Route.class)
        || parameter.getType().isAssignableTo(org.mule.sdk.api.runtime.route.Route.class);
  }

  /**
   * @param sourceElement a source
   * @return whether the given source emits response or not
   * 
   * @since 4.5.0
   */
  public static boolean emitsResponse(SourceElement sourceElement) {
    return sourceElement.isAnnotatedWith(EmitsResponse.class)
        || sourceElement.isAnnotatedWith(org.mule.sdk.api.annotation.source.EmitsResponse.class);
  }
}
