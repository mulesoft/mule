/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.executor;

import static org.apache.commons.lang.ArrayUtils.isEmpty;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.toMap;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isParameterContainer;
import org.mule.metadata.java.api.JavaTypeLoader;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.runtime.operation.OperationContext;
import org.mule.runtime.module.extension.internal.introspection.ParameterGroup;
import org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser;
import org.mule.runtime.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.ArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ByParameterNameArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConfigurationArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.EventArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.MessageArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterGroupArgumentResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * Resolves the values of an {@link OperationModel}'s {@link ParameterModel parameterModels} by matching them to the arguments in
 * a {@link Method}
 *
 * @since 3.7.0
 */
public final class MethodArgumentResolverDelegate implements ArgumentResolverDelegate {

  private static final ArgumentResolver<Object> CONFIGURATION_ARGUMENT_RESOLVER = new ConfigurationArgumentResolver();
  private static final ArgumentResolver<Object> CONNECTOR_ARGUMENT_RESOLVER = new ConnectionArgumentResolver();
  private static final ArgumentResolver<MuleMessage> MESSAGE_ARGUMENT_RESOLVER = new MessageArgumentResolver();
  private static final ArgumentResolver<MuleEvent> EVENT_ARGUMENT_RESOLVER = new EventArgumentResolver();


  private final Method method;
  private final JavaTypeLoader typeLoader = new JavaTypeLoader(this.getClass().getClassLoader());
  private ArgumentResolver<? extends Object>[] argumentResolvers;
  private Map<java.lang.reflect.Parameter, ParameterGroupArgumentResolver<? extends Object>> parameterGroupResolvers;

  /**
   * Creates a new instance for the given {@code method}
   *
   * @param method the {@link Method} to be called
   */
  public MethodArgumentResolverDelegate(OperationModel operationModel, Method method) {
    this.method = method;
    initArgumentResolvers(operationModel);
  }

  private void initArgumentResolvers(OperationModel model) {
    final Class<?>[] parameterTypes = method.getParameterTypes();

    if (isEmpty(parameterTypes)) {
      argumentResolvers = new ArgumentResolver[] {};
      return;
    }

    argumentResolvers = new ArgumentResolver[parameterTypes.length];
    Annotation[][] parameterAnnotations = method.getParameterAnnotations();
    Parameter[] parameters = method.getParameters();
    parameterGroupResolvers = getParameterGroupResolvers(model);
    final List<String> paramNames = MuleExtensionAnnotationParser.getParamNames(method);

    for (int i = 0; i < parameterTypes.length; i++) {
      final Class<?> parameterType = parameterTypes[i];
      Map<Class<? extends Annotation>, Annotation> annotations = toMap(parameterAnnotations[i]);

      ArgumentResolver<?> argumentResolver;

      if (annotations.containsKey(UseConfig.class)) {
        argumentResolver = CONFIGURATION_ARGUMENT_RESOLVER;
      } else if (annotations.containsKey(Connection.class)) {
        argumentResolver = CONNECTOR_ARGUMENT_RESOLVER;
      } else if (MuleEvent.class.isAssignableFrom(parameterType)) {
        argumentResolver = EVENT_ARGUMENT_RESOLVER;
      } else if (MuleMessage.class.isAssignableFrom(parameterType)) {
        argumentResolver = MESSAGE_ARGUMENT_RESOLVER;
      } else if (isParameterContainer(annotations.keySet(), typeLoader.load(parameterType))) {
        argumentResolver = parameterGroupResolvers.get(parameters[i]);
      } else {
        argumentResolver = new ByParameterNameArgumentResolver<>(paramNames.get(i));
      }

      argumentResolvers[i] = argumentResolver;
    }
  }

  @Override
  public Object[] resolve(OperationContext operationContext, Class<?>[] parameterTypes) {

    Object[] parameterValues = new Object[argumentResolvers.length];
    int i = 0;
    for (ArgumentResolver<?> argumentResolver : argumentResolvers) {
      parameterValues[i++] = argumentResolver.resolve(operationContext);
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
   * @param model operation model
   * @return mapping between the {@link Method}'s arguments which are parameters groups and their respective resolvers
   */
  private Map<Parameter, ParameterGroupArgumentResolver<? extends Object>> getParameterGroupResolvers(OperationModel model) {
    Optional<ParameterGroupModelProperty> parameterGroupModelProperty = model.getModelProperty(ParameterGroupModelProperty.class);
    Map<Parameter, ParameterGroupArgumentResolver<? extends Object>> resolverMap = new HashMap<>();

    if (parameterGroupModelProperty.isPresent()) {
      for (ParameterGroup<Parameter> group : parameterGroupModelProperty.get().getGroups()) {
        resolverMap.put(group.getContainer(), new ParameterGroupArgumentResolver<>(group));
      }
    }

    return resolverMap;
  }
}
