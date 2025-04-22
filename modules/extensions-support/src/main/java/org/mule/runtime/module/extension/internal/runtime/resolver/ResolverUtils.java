/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.metadata.api.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.module.extension.internal.loader.java.property.stackabletypes.StackedTypesModelProperty.getStackedTypesModelProperty;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isParameterResolver;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isTypedValue;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.toDataType;

import static java.util.Optional.empty;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.util.message.stream.UnclosableCursorStream;
import org.mule.runtime.extension.api.declaration.type.annotation.LiteralTypeAnnotation;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.loader.java.property.stackabletypes.StackedTypesModelProperty;
import org.mule.runtime.module.extension.internal.runtime.parameter.PropagateAllDistributedTraceContextManager;
import org.mule.runtime.tracer.api.EventTracer;
import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;

import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Utility class to share common behaviour between resolvers
 *
 * @since 4.0
 */
public class ResolverUtils {

  private ResolverUtils() {

  }

  static ValueResolver<?> getFieldDefaultValueValueResolver(ObjectFieldType field,
                                                            TransformationService transformationService,
                                                            ExtendedExpressionManager expressionManager,
                                                            Injector injector) {
    Optional<String> defaultValue = getDefaultValue(field);
    checkArgument(defaultValue.isPresent(), "No default value available for field :" + field.getKey().getName());
    return getExpressionBasedValueResolver(defaultValue.get(), field.getValue(), transformationService, expressionManager,
                                           injector);
  }

  static ValueResolver<?> getFieldDefaultValueValueResolver(MetadataType fieldType, String defaultValue,
                                                            TransformationService transformationService,
                                                            ExtendedExpressionManager expressionManager,
                                                            Injector injector) {
    return getExpressionBasedValueResolver(defaultValue, fieldType, transformationService, expressionManager, injector);
  }

  public static ValueResolver<?> getExpressionBasedValueResolver(String expression, MetadataType metadataType,
                                                                 TransformationService transformationService,
                                                                 ExtendedExpressionManager expressionManager,
                                                                 Injector injector) {
    return getExpressionBasedValueResolver(expression,
                                           () -> isTypedValue(metadataType),
                                           () -> isParameterResolver(metadataType),
                                           empty(),
                                           metadataType,
                                           transformationService,
                                           expressionManager,
                                           injector);
  }

  static ValueResolver<?> getExpressionBasedValueResolver(String expression, ParameterModel operationModel,
                                                          TransformationService transformationService,
                                                          ExtendedExpressionManager expressionManager,
                                                          Injector injector) {
    MetadataType metadataType = operationModel.getType();
    return getExpressionBasedValueResolver(expression,
                                           () -> isTypedValue(metadataType),
                                           () -> isParameterResolver(metadataType),
                                           getStackedTypesModelProperty(operationModel.getModelProperties()),
                                           metadataType,
                                           transformationService,
                                           expressionManager,
                                           injector);
  }

  static ValueResolver<?> getDefaultValueResolver(ParameterModel parameter,
                                                  TransformationService transformationService,
                                                  ExtendedExpressionManager expressionManager,
                                                  Injector injector) {
    Object defaultValue = parameter.getDefaultValue();
    if (defaultValue instanceof String) {
      return getExpressionBasedValueResolver((String) defaultValue, parameter, transformationService, expressionManager,
                                             injector);
    } else if (defaultValue != null) {
      return new StaticValueResolver<>(defaultValue);
    }
    return null;
  }

  public static <T> T resolveRecursively(ValueResolver<T> valueResolver, ValueResolvingContext resolvingContext)
      throws MuleException {
    T resolve = valueResolver.resolve(resolvingContext);
    if (resolve instanceof ValueResolver) {
      resolve = resolveRecursively((ValueResolver<T>) resolve, resolvingContext);
    }
    return resolve;
  }

  /**
   * Executes the {@code resolver} using the given {@code context}, applying all the required resolution rules that may apply for
   * the given {@code T} type.
   *
   * @param resolver the {@link ValueResolver} to execute
   * @param context  the {@link ValueResolvingContext} to pass on the {@code resolver}
   * @return the resolved value
   * @throws MuleException
   */
  public static <T> T resolveValue(ValueResolver<T> resolver, ValueResolvingContext context)
      throws MuleException {
    T value = resolveRecursively(resolver, context);
    if (context == null || context.resolveCursors()) {
      return (T) resolveCursor(value);
    } else {
      return value;
    }
  }

  /**
   * Obtains a {@link Cursor} based on the {@code value}, if one is available.
   *
   * @return the given {@code value} but converting a {@link CursorProvider} to a {@link Cursor} if any is present.
   */
  public static Object resolveCursor(Object value) {
    if (value instanceof CursorProvider) {
      return ((CursorProvider<?>) value).openCursor();
    } else if (value instanceof TypedValue) {
      return resolveCursor((TypedValue<?>) value);
    }

    return value;
  }

  /**
   * Obtains a {@link TypedValue} of {@link Cursor} based on the given {@code typedValue}, if one is available.
   *
   * @return the given {@code typedValue} but converting a {@link CursorProvider} to a {@link Cursor} if any is present.
   */
  public static TypedValue<?> resolveCursor(TypedValue<?> typedValue) {
    Object objectValue = typedValue.getValue();

    if (objectValue instanceof CursorProvider) {
      Cursor cursor = ((CursorProvider<?>) objectValue).openCursor();
      return typedValue(typedValue, cursor);
    }

    return typedValue;
  }

  /**
   * Obtains a {@link Cursor} based on the {@code value}, if one is available. Additionally, if the resulting cursor is a
   * {@link CursorStream}, it would be wrapped inside {@link UnclosableCursorStream}.
   *
   * For performance reasons, we want to avoid receiving the decorator as a parameter.
   *
   * @return the given {@code value} but converting a {@link CursorProvider} to a {@link Cursor} if any is present.
   */
  public static Object resolveCursorAsUnclosable(Object value) {
    if (value instanceof CursorProvider) {
      return resolveCursorProviderAsUnclosable((CursorProvider<?>) value);
    } else if (value instanceof TypedValue) {
      return resolveCursorAsUnclosable((TypedValue<?>) value);
    } else if (value instanceof CursorStream) {
      return new UnclosableCursorStream((CursorStream) value);
    }

    return value;
  }

  /**
   * Obtains a {@link TypedValue} of {@link Cursor} based on the given {@code typedValue}, if one is available. Additionally, if
   * the resulting cursor is a {@link CursorStream}, it will be wrapped inside {@link UnclosableCursorStream}.
   *
   * For performance reasons, we want to avoid receiving the decorator as a parameter.
   *
   * @return the given {@code typedValue} but converting a {@link CursorProvider} to a {@link Cursor} if any is present.
   */
  public static TypedValue<?> resolveCursorAsUnclosable(TypedValue<?> typedValue) {
    Object objectValue = typedValue.getValue();

    if (objectValue instanceof CursorProvider) {
      Cursor cursor = resolveCursorProviderAsUnclosable((CursorProvider<?>) objectValue);
      return typedValue(typedValue, cursor);
    } else if (objectValue instanceof CursorStream) {
      return new TypedValue<>(new UnclosableCursorStream((CursorStream) objectValue), typedValue.getDataType(),
                              typedValue.getByteLength());
    }

    return typedValue;
  }

  /**
   * If the value given is a {@link CursorStream} or a {@link TypedValue} of a {@link CursorStream}, it will be wrapped inside
   * {@link UnclosableCursorStream}.
   *
   * @return A decorated value (or typed value), or the same instance if no change was needed.
   */
  public static Object typedValueAsUnclosable(Object value) {
    if (value instanceof TypedValue) {
      return typedValueAsUnclosable((TypedValue<?>) value);
    } else if (value instanceof CursorStream) {
      return new UnclosableCursorStream((CursorStream) value);
    }

    return value;
  }

  /**
   * If the value of the given {@link TypedValue} is a {@link CursorStream}, it will be wrapped inside
   * {@link UnclosableCursorStream}.
   *
   * @return A decorated typed value, or the same instance if no change was needed.
   */
  public static TypedValue<?> typedValueAsUnclosable(TypedValue<?> typedValue) {
    Object objectValue = typedValue.getValue();

    if (objectValue instanceof CursorStream) {
      return new TypedValue<>(new UnclosableCursorStream((CursorStream) objectValue), typedValue.getDataType(),
                              typedValue.getByteLength());
    }

    return typedValue;
  }


  public static DistributedTraceContextManager resolveDistributedTraceContextManager(CoreEvent coreEvent,
                                                                                     EventTracer<CoreEvent> coreEventTracer) {
    return new PropagateAllDistributedTraceContextManager(coreEvent, coreEventTracer);
  }

  public static Map<String, String> resolveDistributedTraceContext(CoreEvent event, EventTracer<CoreEvent> coreEventTracer) {
    return coreEventTracer.getDistributedTraceContextMap(event);
  }

  private static Cursor resolveCursorProviderAsUnclosable(CursorProvider<?> cursorProvider) {
    Cursor cursor = cursorProvider.openCursor();
    if (cursor instanceof CursorStream) {
      return new UnclosableCursorStream((CursorStream) cursor);
    }
    return cursor;
  }

  private static TypedValue<?> typedValue(TypedValue<?> typedValue, Object value) {
    return new TypedValue<>(value, DataType.builder()
        .type(value != null ? value.getClass() : Object.class)
        .mediaType(typedValue.getDataType().getMediaType())
        .build(), typedValue.getByteLength());
  }

  private static ValueResolver<?> getExpressionBasedValueResolver(String expression, BooleanSupplier isTypedValue,
                                                                  BooleanSupplier isParameterResolver,
                                                                  Optional<StackedTypesModelProperty> stackedTypesModelProperty,
                                                                  MetadataType type,
                                                                  TransformationService transformationService,
                                                                  ExtendedExpressionManager expressionManager,
                                                                  Injector injector) {

    try {
      ValueResolver resolver;
      if (stackedTypesModelProperty.isPresent()) {
        resolver = stackedTypesModelProperty.get().getValueResolverFactory().getExpressionBasedValueResolver(expression,
                                                                                                             getType(type));
        // TODO MULE-13518: Add support for stacked value resolvers for @Parameter inside pojos
        // The following "IFs" should be removed once implemented
      } else if (isTypedValue.getAsBoolean()) {
        ExpressionTypedValueValueResolver<Object> valueResolver =
            new ExpressionTypedValueValueResolver<>(expression, getType(type));
        valueResolver.setTransformationService(transformationService);
        valueResolver.setExtendedExpressionManager(expressionManager);
        resolver = valueResolver;
      } else if (isParameterResolver.getAsBoolean()) {
        ExpressionBasedParameterResolverValueResolver<Object> valueResolver =
            new ExpressionBasedParameterResolverValueResolver<>(expression, getType(type), toDataType(type));
        valueResolver.setTransformationService(transformationService);
        valueResolver.setExtendedExpressionManager(expressionManager);
        resolver = valueResolver;
      } else if (expressionManager.isExpression(expression)) {
        if (type.getAnnotation(LiteralTypeAnnotation.class).isPresent()) {
          resolver = new StaticLiteralValueResolver<Object>(expression, getType(type));
        } else {
          TypeSafeExpressionValueResolver<Object> valueResolver =
              new TypeSafeExpressionValueResolver<>(expression, getType(type), toDataType(type));
          valueResolver.setTransformationService(transformationService);
          valueResolver.setExtendedExpressionManager(expressionManager);
          resolver = valueResolver;
        }
      } else {
        TypeSafeValueResolverWrapper typeSafeValueResolverWrapper =
            new TypeSafeValueResolverWrapper<>(new StaticValueResolver<>(expression), getType(type));
        typeSafeValueResolverWrapper.setTransformationService(transformationService);
        resolver = typeSafeValueResolverWrapper;
      }

      initialiseIfNeeded(resolver, injector);
      return resolver;
    } catch (InitialisationException e) {
      throw new MuleRuntimeException(e);
    }
  }
}
