/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.mule.runtime.api.util.collection.Collectors.toImmutableMap;
import static org.mule.runtime.module.extension.internal.loader.java.MuleExtensionAnnotationParser.getParamNames;
import static org.mule.runtime.module.extension.internal.loader.java.MuleExtensionAnnotationParser.toMap;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isParameterContainer;
import org.mule.metadata.java.api.JavaTypeLoader;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.FlowListener;
import org.mule.runtime.extension.api.runtime.parameter.Literal;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.process.RouterCompletionCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.extension.api.runtime.source.SourceCompletionCallback;
import org.mule.runtime.extension.api.runtime.source.SourceResult;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;
import org.mule.runtime.extension.api.security.AuthenticationHandler;
import org.mule.runtime.extension.api.tx.OperationTransactionalAction;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.ArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ByParameterNameArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.CompletionCallbackArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ComponentLocationArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConfigurationArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.DefaultEncodingArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ErrorArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.FlowListenerArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.LiteralArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.MediaTypeArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterGroupArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterResolverArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.RouterCallbackArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.SecurityContextHandlerArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.SourceCallbackContextArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.SourceCompletionCallbackArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.SourceResultArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StreamingHelperArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypedValueArgumentResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;


/**
 * Resolves the values of an {@link ComponentModel}'s {@link ParameterModel parameterModels} by matching them to the arguments in
 * a {@link Method}
 *
 * @since 3.7.0
 */
public final class MethodArgumentResolverDelegate implements ArgumentResolverDelegate {

  private static final ArgumentResolver<Object> CONFIGURATION_ARGUMENT_RESOLVER = new ConfigurationArgumentResolver();
  private static final ArgumentResolver<Object> CONNECTOR_ARGUMENT_RESOLVER = new ConnectionArgumentResolver();
  private static final ArgumentResolver<MediaType> MEDIA_TYPE_ARGUMENT_RESOLVER = new MediaTypeArgumentResolver();
  private static final ArgumentResolver<String> DEFAULT_ENCODING_ARGUMENT_RESOLVER = new DefaultEncodingArgumentResolver();
  private static final ArgumentResolver<SourceCallbackContext> SOURCE_CALLBACK_CONTEXT_ARGUMENT_RESOLVER =
      new SourceCallbackContextArgumentResolver();
  private static final ArgumentResolver<Error> ERROR_ARGUMENT_RESOLVER = new ErrorArgumentResolver();
  private static final ArgumentResolver<CompletionCallback> NON_BLOCKING_CALLBACK_ARGUMENT_RESOLVER =
      new CompletionCallbackArgumentResolver();
  private static final ArgumentResolver<RouterCompletionCallback> ROUTER_CALLBACK_ARGUMENT_RESOLVER =
      new RouterCallbackArgumentResolver();
  private static final ArgumentResolver<SourceCompletionCallback> ASYNC_SOURCE_COMPLETION_CALLBACK_ARGUMENT_RESOLVER =
      new SourceCompletionCallbackArgumentResolver();
  private static final ArgumentResolver<AuthenticationHandler> SECURITY_CONTEXT_HANDLER =
      new SecurityContextHandlerArgumentResolver();
  private static final ArgumentResolver<FlowListener> FLOW_LISTENER_ARGUMENT_RESOLVER = new FlowListenerArgumentResolver();
  private static final ArgumentResolver<StreamingHelper> STREAMING_HELPER_ARGUMENT_RESOLVER =
      new StreamingHelperArgumentResolver();
  private static final ArgumentResolver<SourceResult> SOURCE_RESULT_ARGUMENT_RESOLVER =
      new SourceResultArgumentResolver(ERROR_ARGUMENT_RESOLVER, SOURCE_CALLBACK_CONTEXT_ARGUMENT_RESOLVER);
  private static final ArgumentResolver<ComponentLocation> COMPONENT_LOCATION_ARGUMENT_RESOLVER =
      new ComponentLocationArgumentResolver();
  private static final ArgumentResolver<OperationTransactionalAction> OPERATION_TRANSACTIONAL_ACTION_ARGUMENT_RESOLVER =
      new OperationTransactionalActionArgumentResolver();


  private final Method method;
  private final JavaTypeLoader typeLoader = new JavaTypeLoader(this.getClass().getClassLoader());
  private ArgumentResolver<?>[] argumentResolvers;
  private Map<java.lang.reflect.Parameter, ParameterGroupArgumentResolver<?>> parameterGroupResolvers;

  /**
   * Creates a new instance for the given {@code method}
   *
   * @param parameterGroupModels {@link List} of {@link ParameterGroupModel} from the corresponding model
   * @param method               the {@link Method} to be called
   */
  public MethodArgumentResolverDelegate(List<ParameterGroupModel> parameterGroupModels, Method method) {
    this.method = method;
    initArgumentResolvers(parameterGroupModels);
  }

  private void initArgumentResolvers(List<ParameterGroupModel> parameterGroupModels) {
    final Class<?>[] parameterTypes = method.getParameterTypes();

    if (isEmpty(parameterTypes)) {
      argumentResolvers = new ArgumentResolver[] {};
      return;
    }

    argumentResolvers = new ArgumentResolver[parameterTypes.length];
    Annotation[][] parameterAnnotations = method.getParameterAnnotations();
    Parameter[] parameters = method.getParameters();
    parameterGroupResolvers = getParameterGroupResolvers(parameterGroupModels);
    final List<String> paramNames = getParamNames(method);

    for (int i = 0; i < parameterTypes.length; i++) {
      final Class<?> parameterType = parameterTypes[i];
      Map<Class<? extends Annotation>, Annotation> annotations = toMap(parameterAnnotations[i]);

      ArgumentResolver<?> argumentResolver;

      if (annotations.containsKey(Config.class)) {
        argumentResolver = CONFIGURATION_ARGUMENT_RESOLVER;
      } else if (annotations.containsKey(Connection.class)) {
        argumentResolver = CONNECTOR_ARGUMENT_RESOLVER;
      } else if (annotations.containsKey(DefaultEncoding.class)) {
        argumentResolver = DEFAULT_ENCODING_ARGUMENT_RESOLVER;
      } else if (Error.class.isAssignableFrom(parameterType)) {
        argumentResolver = ERROR_ARGUMENT_RESOLVER;
      } else if (SourceCallbackContext.class.equals(parameterType)) {
        argumentResolver = SOURCE_CALLBACK_CONTEXT_ARGUMENT_RESOLVER;
      } else if (isParameterContainer(annotations.keySet(), typeLoader.load(parameterType)) &&
          !((ParameterGroup) annotations.get(ParameterGroup.class)).showInDsl()) {
        argumentResolver = parameterGroupResolvers.get(parameters[i]);
      } else if (ParameterResolver.class.equals(parameterType)) {
        argumentResolver = new ParameterResolverArgumentResolver<>(paramNames.get(i));
      } else if (TypedValue.class.equals(parameterType)) {
        argumentResolver = new TypedValueArgumentResolver<>(paramNames.get(i));
      } else if (Literal.class.equals(parameterType)) {
        argumentResolver = new LiteralArgumentResolver<>(paramNames.get(i), parameterType);
      } else if (CompletionCallback.class.equals(parameterType)) {
        argumentResolver = NON_BLOCKING_CALLBACK_ARGUMENT_RESOLVER;
      } else if (RouterCompletionCallback.class.equals(parameterType)) {
        argumentResolver = ROUTER_CALLBACK_ARGUMENT_RESOLVER;
      } else if (MediaType.class.equals(parameterType)) {
        argumentResolver = MEDIA_TYPE_ARGUMENT_RESOLVER;
      } else if (AuthenticationHandler.class.equals(parameterType)) {
        argumentResolver = SECURITY_CONTEXT_HANDLER;
      } else if (FlowListener.class.equals(parameterType)) {
        argumentResolver = FLOW_LISTENER_ARGUMENT_RESOLVER;
      } else if (StreamingHelper.class.equals(parameterType)) {
        argumentResolver = STREAMING_HELPER_ARGUMENT_RESOLVER;
      } else if (SourceResult.class.equals(parameterType)) {
        argumentResolver = SOURCE_RESULT_ARGUMENT_RESOLVER;
      } else if (SourceCompletionCallback.class.equals(parameterType)) {
        argumentResolver = ASYNC_SOURCE_COMPLETION_CALLBACK_ARGUMENT_RESOLVER;
      } else if (ComponentLocation.class.equals(parameterType)) {
        argumentResolver = COMPONENT_LOCATION_ARGUMENT_RESOLVER;
      } else if (OperationTransactionalAction.class.equals(parameterType)) {
        argumentResolver = OPERATION_TRANSACTIONAL_ACTION_ARGUMENT_RESOLVER;
      } else {
        argumentResolver = new ByParameterNameArgumentResolver<>(paramNames.get(i));
      }

      argumentResolvers[i] = argumentResolver;
    }
  }

  @Override
  public Object[] resolve(ExecutionContext executionContext, Class<?>[] parameterTypes) {

    Object[] parameterValues = new Object[argumentResolvers.length];
    int i = 0;
    for (ArgumentResolver<?> argumentResolver : argumentResolvers) {
      parameterValues[i++] = argumentResolver.resolve(executionContext);
    }

    return resolvePrimitiveTypes(parameterTypes, parameterValues);
  }

  private Object[] resolvePrimitiveTypes(Class<?>[] parametersType, Object[] parameterValues) {
    Object[] resolvedParameters = new Object[parameterValues.length];
    for (int i = 0; i < parameterValues.length; i++) {
      Object parameterValue = parameterValues[i];
      if (parameterValue == null) {
        resolvedParameters[i] = resolvePrimitiveTypeDefaultValue(parametersType[i]);
      } else {
        resolvedParameters[i] = parameterValue;
      }
    }
    return resolvedParameters;
  }

  private Object resolvePrimitiveTypeDefaultValue(Class<?> type) {
    if (type.equals(byte.class)) {
      return (byte) 0;
    }
    if (type.equals(short.class)) {
      return (short) 0;
    }
    if (type.equals(int.class)) {
      return 0;
    }
    if (type.equals(long.class)) {
      return 0l;
    }
    if (type.equals(float.class)) {
      return 0.0f;
    }
    if (type.equals(double.class)) {
      return 0.0d;
    }
    if (type.equals(boolean.class)) {
      return false;
    }
    if (type.equals(char.class)) {
      return '\u0000';
    }
    return null;
  }

  /**
   * Uses the {@link ParameterGroupModelProperty} obtain the resolvers.
   *
   * @param parameterGroupModels the parameter groups
   * @return mapping between the {@link Method}'s arguments which are parameters groups and their respective resolvers
   */
  private Map<Parameter, ParameterGroupArgumentResolver<? extends Object>> getParameterGroupResolvers(
                                                                                                      List<ParameterGroupModel> parameterGroupModels) {
    return parameterGroupModels.stream()
        .map(group -> group.getModelProperty(ParameterGroupModelProperty.class)
            .map(ParameterGroupModelProperty::getDescriptor).orElse(null))
        .filter(group -> group != null && group.getContainer() instanceof Parameter)
        .collect(toImmutableMap(group -> (Parameter) group.getContainer(), ParameterGroupArgumentResolver::new));
  }
}
