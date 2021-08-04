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
import org.mule.runtime.api.meta.model.declaration.fluent.ExecutableComponentDeclarer;
import org.mule.runtime.extension.api.annotation.Streaming;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.runtime.extension.api.runtime.route.Route;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.module.extension.api.loader.ModelLoaderDelegate;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Utility class for {@link ModelLoaderDelegate model loaders}
 *
 * @since 1.0
 */
public final class ModelLoaderUtils {

  private ModelLoaderUtils() {}

  public static boolean isScope(MethodElement methodElement) {
    return methodElement.getParameters().stream().anyMatch(ModelLoaderUtils::isProcessorChain);
  }

  public static boolean isRouter(MethodElement methodElement) {
    return !getRoutes(methodElement).isEmpty();
  }

  public static List<ExtensionParameter> getRoutes(MethodElement methodElement) {
    return methodElement.getParameters().stream()
        .filter(ModelLoaderUtils::isRoute)
        .collect(toList());
  }

  public static boolean isRoute(ExtensionParameter parameter) {
    return parameter.getType().isAssignableTo(Route.class)
        || parameter.getType().isAssignableTo(org.mule.sdk.api.runtime.route.Route.class);
  }

  public static boolean isNonBlocking(MethodElement method) {
    return !getCompletionCallbackParameters(method).isEmpty();
  }

  public static List<ExtensionParameter> getCompletionCallbackParameters(MethodElement method) {
    return method.getParameters().stream()
        .filter(p -> p.getType().isAssignableTo(CompletionCallback.class) ||
            p.getType().isAssignableTo(org.mule.sdk.api.runtime.process.CompletionCallback.class))
        .collect(toList());
  }

  public static boolean isAutoPaging(MethodElement operationMethod) {
    return operationMethod.getReturnType().isAssignableTo(PagingProvider.class)
        || operationMethod.getReturnType().isAssignableTo(org.mule.sdk.api.runtime.streaming.PagingProvider.class);
  }

  public static boolean isProcessorChain(ExtensionParameter parameter) {
    return parameter.getType().isAssignableTo(Chain.class)
        || parameter.getType().isAssignableTo(org.mule.sdk.api.runtime.route.Chain.class);
  }

  public static void handleByteStreaming(Method method, ExecutableComponentDeclarer executableComponent,
                                         MetadataType outputType) {
    executableComponent.supportsStreaming(isInputStream(outputType)
        || method.getAnnotation(Streaming.class) != null
        || method.getAnnotation(org.mule.sdk.api.annotation.Streaming.class) != null);
  }

  public static void handleByteStreaming(MethodElement method, ExecutableComponentDeclarer executableComponent,
                                         MetadataType outputType) {
    executableComponent.supportsStreaming(isInputStream(outputType)
        || method.isAnnotatedWith(Streaming.class)
        || method.isAnnotatedWith(org.mule.sdk.api.annotation.Streaming.class));
  }

  /**
   * @param type a {@link MetadataType}
   * @return whether the given {@code type} represents an {@link InputStream} or not
   */
  public static boolean isInputStream(MetadataType type) {
    return isAssignableFrom(type, InputStream.class);
  }

  private static boolean isAssignableFrom(MetadataType metadataType, Class<?> type) {
    return getType(metadataType).map(clazz -> type.isAssignableFrom(clazz)).orElse(false);
  }
}
