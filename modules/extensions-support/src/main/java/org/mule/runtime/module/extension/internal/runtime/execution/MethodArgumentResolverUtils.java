/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.runtime.parameter.CorrelationInfo;
import org.mule.runtime.extension.api.runtime.parameter.Literal;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.runtime.extension.api.runtime.source.SourceCompletionCallback;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;
import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Helper class for the {@link MethodArgumentResolverDelegate} that has the logic of the relation between the mule-extensions-api
 * and the mule-sdk-api.
 *
 * @since 4.5
 */
public class MethodArgumentResolverUtils {

  private MethodArgumentResolverUtils() {}

  public static boolean isConfigParameter(Map<Class<? extends Annotation>, Annotation> parameterAnnotations) {
    return parameterAnnotations.containsKey(Config.class)
        || parameterAnnotations.containsKey(org.mule.sdk.api.annotation.param.Config.class);
  }

  public static boolean isConnectionParameter(Map<Class<? extends Annotation>, Annotation> parameterAnnotations) {
    return parameterAnnotations.containsKey(Connection.class)
        || parameterAnnotations.containsKey(org.mule.sdk.api.annotation.param.Connection.class);
  }

  public static boolean isDefaultEncoding(Map<Class<? extends Annotation>, Annotation> parameterAnnotations) {
    return parameterAnnotations.containsKey(DefaultEncoding.class)
        || parameterAnnotations.containsKey(org.mule.sdk.api.annotation.param.DefaultEncoding.class);
  }

  public static boolean isLiteralType(Class<?> parameterType) {
    return Literal.class.equals(parameterType) || org.mule.sdk.api.runtime.parameter.Literal.class.equals(parameterType);
  }

  public static boolean isParameterResolverType(Class<?> parameterType) {
    return ParameterResolver.class.equals(parameterType)
        || org.mule.sdk.api.runtime.parameter.ParameterResolver.class.equals(parameterType);
  }

  public static boolean isStreamingHelperType(Class<?> parameterType) {
    return StreamingHelper.class.equals(parameterType)
        || org.mule.sdk.api.runtime.streaming.StreamingHelper.class.equals(parameterType);
  }

  public static boolean isSourceCompletionCallbackType(Class<?> parameterType) {
    return SourceCompletionCallback.class.equals(parameterType)
        || org.mule.sdk.api.runtime.source.SourceCompletionCallback.class.equals(parameterType);
  }

  public static boolean isCorrelationInfoType(Class<?> parameterType) {
    return CorrelationInfo.class.equals(parameterType)
        || org.mule.sdk.api.runtime.parameter.CorrelationInfo.class.equals(parameterType);
  }

  public static boolean isDistributedTraceContextManagerType(Class<?> parameterType) {
    return DistributedTraceContextManager.class.equals(parameterType);
  }

}
