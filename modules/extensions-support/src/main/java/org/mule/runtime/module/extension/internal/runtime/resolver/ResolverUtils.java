/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.util.Optional.empty;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.module.extension.internal.loader.java.property.stackabletypes.StackedTypesModelProperty.getStackedTypesModelProperty;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isParameterResolver;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isTypedValue;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.toDataType;

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
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpanError;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.CoreEventTracer;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.TracingCondition;
import org.mule.runtime.core.internal.trace.DistributedTraceContext;
import org.mule.runtime.core.internal.util.message.stream.UnclosableCursorStream;
import org.mule.runtime.module.extension.internal.loader.java.property.stackabletypes.StackedTypesModelProperty;
import org.mule.runtime.module.extension.internal.runtime.parameter.PropagateAllDistributedTraceContextManager;
import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;

import java.util.HashMap;
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

  static ValueResolver<?> getFieldDefaultValueValueResolver(ObjectFieldType field, MuleContext muleContext) {
    Optional<String> defaultValue = getDefaultValue(field);
    checkArgument(defaultValue.isPresent(), "No default value available for field :" + field.getKey().getName());
    return getExpressionBasedValueResolver(defaultValue.get(), field.getValue(), muleContext);
  }

  static ValueResolver<?> getFieldDefaultValueValueResolver(MetadataType fieldType, String defaultValue,
                                                            MuleContext muleContext) {
    return getExpressionBasedValueResolver(defaultValue, fieldType, muleContext);
  }

  public static ValueResolver<?> getExpressionBasedValueResolver(String expression, MetadataType metadataType,
                                                                 MuleContext muleContext) {
    return getExpressionBasedValueResolver(expression,
                                           () -> isTypedValue(metadataType),
                                           () -> isParameterResolver(metadataType),
                                           empty(),
                                           metadataType,
                                           muleContext);
  }

  static ValueResolver<?> getExpressionBasedValueResolver(String expression, ParameterModel operationModel,
                                                          MuleContext muleContext) {
    MetadataType metadataType = operationModel.getType();
    return getExpressionBasedValueResolver(expression,
                                           () -> isTypedValue(metadataType),
                                           () -> isParameterResolver(metadataType),
                                           getStackedTypesModelProperty(operationModel.getModelProperties()),
                                           metadataType,
                                           muleContext);
  }

  static ValueResolver<?> getDefaultValueResolver(ParameterModel parameter, MuleContext muleContext) {
    Object defaultValue = parameter.getDefaultValue();
    if (defaultValue instanceof String) {
      return getExpressionBasedValueResolver((String) defaultValue, parameter, muleContext);
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
                                                                                     CoreEventTracer coreEventTracer) {
    return new PropagateAllDistributedTraceContextManager(coreEvent, coreEventTracer);
  }

  public static DistributedTraceContext resolveDistributedTraceContext(CoreEvent event, CoreEventTracer coreEventTracer) {
    Map<String, String> map = coreEventTracer.getDistributedTraceContextMap(event);

    return new DistributedTraceContext() {

      @Override
      public Optional<String> getTraceFieldValue(String key) {
        return Optional.ofNullable(map.get(key));
      }

      @Override
      public Map<String, String> tracingFieldsAsMap() {
        return map;
      }

      @Override
      public Optional<String> getBaggageItem(String key) {
        return empty();
      }

      @Override
      public Map<String, String> baggageItemsAsMap() {
        return new HashMap<>();
      }

      @Override
      public DistributedTraceContext copy() {
        return this;
      }

      @Override
      public void endCurrentContextSpan(TracingCondition tracingCondition) {
        // Nothing to do.
      }

      @Override
      public void recordErrorAtCurrentSpan(InternalSpanError error) {
        // Nothing to do.
      }

      @Override
      public void setRootSpanName(String name) {
        // Nothing to do.
      }

      @Override
      public String getRootSpanName() {
        return null;
      }

      @Override
      public void setSpanRootAttribute(String key, String value) {

      }

      @Override
      public Map<String, String> getSpanRootAttributes() {
        return null;
      }

      @Override
      public void setCurrentSpan(InternalSpan span, TracingCondition tracingCondition) {
        // Nothing to do.
      }

      @Override
      public Optional<InternalSpan> getCurrentSpan() {
        return empty();
      }
    };
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
                                                                  MuleContext muleContext) {

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
        valueResolver.setTransformationService(muleContext.getTransformationService());
        valueResolver.setExtendedExpressionManager(muleContext.getExpressionManager());
        resolver = valueResolver;
      } else if (isParameterResolver.getAsBoolean()) {
        ExpressionBasedParameterResolverValueResolver<Object> valueResolver =
            new ExpressionBasedParameterResolverValueResolver<>(expression, getType(type), toDataType(type));
        valueResolver.setTransformationService(muleContext.getTransformationService());
        valueResolver.setExtendedExpressionManager(muleContext.getExpressionManager());
        resolver = valueResolver;
      } else if (muleContext.getExpressionManager().isExpression(expression)) {
        TypeSafeExpressionValueResolver<Object> valueResolver =
            new TypeSafeExpressionValueResolver<>(expression, getType(type), toDataType(type));
        valueResolver.setTransformationService(muleContext.getTransformationService());
        valueResolver.setExtendedExpressionManager(muleContext.getExpressionManager());
        resolver = valueResolver;
      } else {
        TypeSafeValueResolverWrapper typeSafeValueResolverWrapper =
            new TypeSafeValueResolverWrapper<>(new StaticValueResolver<>(expression), getType(type));
        typeSafeValueResolverWrapper.setTransformationService(muleContext.getTransformationService());
        resolver = typeSafeValueResolverWrapper;
      }

      initialiseIfNeeded(resolver, muleContext);
      return resolver;
    } catch (InitialisationException e) {
      throw new MuleRuntimeException(e);
    }
  }
}
