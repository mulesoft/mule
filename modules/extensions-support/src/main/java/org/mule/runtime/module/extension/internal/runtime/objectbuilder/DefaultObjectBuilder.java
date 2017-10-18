/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.objectbuilder;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.extension.internal.runtime.objectbuilder.ObjectBuilderUtils.createInstance;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverUtils.resolveValue;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.checkInstantiable;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getField;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.injectFields;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.hasAnyDynamic;
import static org.springframework.util.ReflectionUtils.setField;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of {@link ObjectBuilder} which creates instances through a provided {@link Class}.
 *
 * @since 3.7.0
 */
public class DefaultObjectBuilder<T> implements ObjectBuilder<T> {

  protected final Class<T> prototypeClass;
  protected final Map<Field, ValueResolver<Object>> resolvers = new HashMap<>();
  private String name = null;
  private String encoding = null;

  /**
   * Creates a new instance that will build instances of {@code prototypeClass}.
   *
   * @param prototypeClass a {@link Class} which needs to have a public defualt constructor
   */
  public DefaultObjectBuilder(Class<T> prototypeClass) {
    checkInstantiable(prototypeClass);
    this.prototypeClass = prototypeClass;
  }

  /**
   * Adds a property which value is to be obtained from a {@link ValueResolver}
   *
   * @param propertyName the name of the property in which the value is to be assigned
   * @param resolver a {@link ValueResolver} used to provide the actual value
   * @return this builder
   * @throws {@link java.lang.IllegalArgumentException} if method or resolver are {@code null}
   */
  public ObjectBuilder<T> addPropertyResolver(String propertyName, ValueResolver<? extends Object> resolver) {
    checkArgument(!isBlank(propertyName), "property name cannot be blank");
    checkArgument(resolver != null, "resolver cannot be null");

    Field field = getField(prototypeClass, propertyName).orElseThrow(
                                                                     () -> new IllegalArgumentException(String
                                                                         .format("Class '%s' does not contain property '%s'",
                                                                                 prototypeClass.getName(), propertyName)));

    field.setAccessible(true);
    resolvers.put(field, (ValueResolver<Object>) resolver);
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

    for (Map.Entry<Field, ValueResolver<Object>> entry : resolvers.entrySet()) {
      setField(entry.getKey(), object, resolveValue(entry.getValue(), context));
    }

    injectFields(object, name, encoding);

    return object;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }
}
