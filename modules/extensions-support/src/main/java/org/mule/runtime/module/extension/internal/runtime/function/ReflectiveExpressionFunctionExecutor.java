/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.function;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.metadata.DataType.fromObject;
import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.setContextClassLoader;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.ReflectionUtils.invokeMethod;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.model.function.FunctionModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.FunctionParameter;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingParameterModelProperty;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * Executes a task associated to a {@link ExecutionContext} by invoking a given {@link Method}
 *
 * @since 4.0
 */
public class ReflectiveExpressionFunctionExecutor implements Lifecycle, FunctionExecutor {

  private static final Logger LOGGER = getLogger(ReflectiveExpressionFunctionExecutor.class);

  private final Method method;
  private final FunctionModel model;
  private final DataType returnType;
  private final Object componentInstance;
  private final ClassLoader extensionClassLoader;
  private final List<FunctionParameter> functionParameters;
  private final Function<Object[], Object[]> parametersResolver;

  @Inject
  private MuleContext muleContext;

  @Inject
  private TransformationService transformationService;

  public ReflectiveExpressionFunctionExecutor(FunctionModel model, DataType returnType,
                                              List<FunctionParameter> functionParameters, Method method,
                                              Object componentInstance) {
    this.model = model;
    this.method = method;
    this.returnType = returnType;
    this.componentInstance = componentInstance;
    this.functionParameters = functionParameters;
    this.extensionClassLoader = method.getDeclaringClass().getClassLoader();
    this.parametersResolver = getTypedValueArgumentsResolver(method);
  }

  @Override
  public Object call(Object[] parameters, BindingContext context) {
    Thread thread = Thread.currentThread();
    ClassLoader currentClassLoader = thread.getContextClassLoader();
    setContextClassLoader(thread, currentClassLoader, extensionClassLoader);
    try {
      return invokeMethod(method, componentInstance, parametersResolver.apply(parameters));
    } finally {
      setContextClassLoader(thread, extensionClassLoader, currentClassLoader);
    }
  }

  @Override
  public Optional<DataType> returnType() {
    return ofNullable(returnType);
  }

  @Override
  public List<FunctionParameter> parameters() {
    return functionParameters;
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(componentInstance, muleContext);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(componentInstance);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(componentInstance);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(componentInstance, LOGGER);
  }

  private Function<Object[], Object[]> getTypedValueArgumentsResolver(Method method) {
    Parameter[] parameters = method.getParameters();
    final Function<Object, Object>[] resolvers = new Function[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      Parameter parameter = parameters[i];
      Class<?> type = parameter.getType();
      if (TypedValue.class.isAssignableFrom(type)) {
        resolvers[i] = getWrapper(parameter);
      } else {
        resolvers[i] = getUnWrapper(type);
      }
    }

    return args -> {
      if (args == null) {
        return null;
      }

      if (args.length == 0) {
        return args;
      }

      final Object[] values = new Object[args.length];
      for (int i = 0; i < args.length; i++) {
        values[i] = resolvers[i].apply(args[i]);
      }

      return values;
    };
  }

  private Function<Object, Object> getWrapper(Parameter parameter) {
    Type[] generics = parameter.getType().getGenericInterfaces();
    Class<?> type;
    if (generics.length == 0) {
      type = Object.class;
    } else {
      type = model.getAllParameterModels().stream()
          .filter(p -> p.getModelProperty(ImplementingParameterModelProperty.class)
              .map(mp -> mp.getParameter().getName().equals(parameter.getName()))
              .orElse(false))
          .map(p -> getType(p.getType()).orElse(Object.class))
          .findFirst()
          .orElseThrow(() -> new IllegalStateException(format("Missing parameter with name [%s]", parameter.getName())));
    }

    DataType expected = DataType.fromType(type);

    return value -> {
      if (value == null) {
        return null;
      }

      if (value instanceof TypedValue) {
        if (((TypedValue) value).getDataType().equals(DataType.TYPED_VALUE)) {
          // DW will wrap anything of type TypedValue in a new TypedValue with DataType.TYPED_VALUE
          value = ((TypedValue) value).getValue();
        }

        TypedValue typedValue = (TypedValue) value;
        // We have to check for transformations of the value because weave won't be able to validate types
        return type.isInstance(typedValue.getValue())
            ? value
            : new TypedValue<>(transformationService.transform(typedValue.getValue(), typedValue.getDataType(), expected),
                               typedValue.getDataType());
      } else {

        return new TypedValue<>(value, fromObject(value));
      }
    };
  }

  private Function<Object, Object> getUnWrapper(Class<?> parameterType) {
    DataType expected = fromType(parameterType);
    return value -> doUnWrap(value, expected);
  }

  private Object doUnWrap(Object value, DataType expected) {
    return value instanceof TypedValue
        ? doUnWrap(((TypedValue) value).getValue(), expected)
        : ClassUtils.isInstance(expected.getType(), value) ? value
            : transformationService.transform(value, fromObject(value), expected);
  }

}
