/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.objectbuilder;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.getMuleVersion;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.runtime.objectbuilder.ObjectBuilderUtils.createInstance;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverUtils.resolveCursor;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverUtils.resolveValue;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.checkInstantiable;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getField;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.injectFields;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.hasAnyDynamic;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.extension.internal.runtime.ValueResolvingException;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.util.FieldSetter;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

/**
 * Default implementation of {@link ObjectBuilder} which creates instances through a provided {@link Class}.
 *
 * @since 3.7.0
 */
public class DefaultObjectBuilder<T> implements ObjectBuilder<T>, Initialisable, ParameterValueResolver {

  @Inject
  private MuleContext muleContext;

  protected final Class<T> prototypeClass;
  protected final Map<FieldSetter, ValueResolver<Object>> resolvers = new HashMap<>();
  protected final Map<String, ValueResolver<? extends Object>> resolverByFieldName = new HashMap<>();
  protected ReflectionCache reflectionCache;
  private String name = null;
  private String encoding = null;

  /**
   * Creates a new instance that will build instances of {@code prototypeClass}.
   *
   * @param prototypeClass a {@link Class} which needs to have a public defualt constructor
   */
  public DefaultObjectBuilder(Class<T> prototypeClass, ReflectionCache reflectionCache) {
    checkState(reflectionCache != null, "null reflection cache");
    checkInstantiable(prototypeClass, reflectionCache);
    this.prototypeClass = prototypeClass;
    this.reflectionCache = reflectionCache;
  }

  /**
   * Adds a property which value is to be obtained from a {@link ValueResolver}
   *
   * @param propertyName the name of the property in which the value is to be assigned
   * @param resolver     a {@link ValueResolver} used to provide the actual value
   * @return this builder
   * @throws {@link java.lang.IllegalArgumentException} if method or resolver are {@code null}
   */
  public ObjectBuilder<T> addPropertyResolver(String propertyName, ValueResolver<? extends Object> resolver) {
    checkArgument(!isBlank(propertyName), "property name cannot be blank");

    Field field = getField(prototypeClass, propertyName, reflectionCache)
        .orElseThrow(() -> new IllegalArgumentException(format("Class '%s' does not contain property '%s'",
                                                               prototypeClass.getName(), propertyName)));

    return addPropertyResolver(field, resolver);
  }

  /**
   * Adds a property which value is to be obtained from a {@link ValueResolver}
   *
   * @param field    the property in which the value is to be assigned
   * @param resolver a {@link ValueResolver} used to provide the actual value
   * @return this builder
   * @throws {@link java.lang.IllegalArgumentException} if method or resolver are {@code null}
   */
  public ObjectBuilder<T> addPropertyResolver(Field field, ValueResolver<? extends Object> resolver) {
    checkArgument(resolver != null, "resolver cannot be null");

    resolverByFieldName.put(field.getName(), resolver);

    resolvers.put(new FieldSetter<>(field), (ValueResolver<Object>) resolver);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDynamic() {
    return hasAnyDynamic(resolvers.values());
  }

  @Override
  public T build(ValueResolvingContext context) throws MuleException {
    T object = createInstance(prototypeClass);

    for (Map.Entry<FieldSetter, ValueResolver<Object>> entry : resolvers.entrySet()) {
      final Object resolvedValue = resolveValue(entry.getValue(), context);

      entry.getKey().set(object, context == null || context.resolveCursors() ? resolveCursor(resolvedValue) : resolvedValue);
    }

    injectFields(object, name, encoding, getMuleVersion(), reflectionCache);

    return object;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }


  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(resolvers.values(), muleContext);
  }

  @Override
  public Object getParameterValue(String parameterName) throws ValueResolvingException {
    ValueResolver<?> valueResolver = resolverByFieldName.get(parameterName);
    if (valueResolver == null) {
      return null;
    }
    try {
      return valueResolver.resolve(ValueResolvingContext.builder(getInitialiserEvent()).build());
    } catch (Exception e) {
      throw new ValueResolvingException(format("An error occurred trying to resolve value for parameter [%s]", parameterName), e);
    }
  }

  @Override
  public Map<String, ValueResolver<? extends Object>> getParameters() {
    return unmodifiableMap(resolverByFieldName);
  }
}
