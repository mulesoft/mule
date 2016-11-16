/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.objectbuilder;

import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getField;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.util.GroupValueSetter;
import org.mule.runtime.module.extension.internal.util.SingleValueSetter;
import org.mule.runtime.module.extension.internal.util.ValueSetter;

import java.util.List;
import java.util.Optional;

/**
 * Base implementation of an {@link ObjectBuilder} which generates object based on an {@link EnrichableModel} for with parameter
 * groups have been defined based on a {@link ParameterGroupModelProperty}
 *
 * @param <T> the generic type of the instances to be produced
 * @since 4.0
 */
public abstract class ResolverSetBasedObjectBuilder<T> implements ObjectBuilder<T> {

  protected final ResolverSet resolverSet;
  private final List<ValueSetter> singleValueSetters;
  private final List<ValueSetter> groupValueSetters;

  public ResolverSetBasedObjectBuilder(Class<?> prototypeClass, ParameterizedModel model, ResolverSet resolverSet) {
    this.resolverSet = resolverSet;
    singleValueSetters = createSingleValueSetters(prototypeClass, resolverSet);
    groupValueSetters = GroupValueSetter.settersFor(model);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDynamic() {
    return resolverSet.isDynamic();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final T build(Event event) throws MuleException {
    return build(resolverSet.resolve(event));
  }

  public T build(ResolverSetResult result) throws MuleException {
    T object = instantiateObject();

    setValues(object, result, groupValueSetters);
    setValues(object, result, singleValueSetters);

    return object;
  }

  private List<ValueSetter> createSingleValueSetters(Class<?> prototypeClass, ResolverSet resolverSet) {
    return resolverSet.getResolvers().keySet().stream().map(parameterName -> {
      // if no field, then it means this is a group attribute
      return getField(prototypeClass, parameterName).map(f -> new SingleValueSetter(parameterName, f));
    }).filter(Optional::isPresent).map(Optional::get).collect(new ImmutableListCollector<>());
  }

  private void setValues(Object target, ResolverSetResult result, List<ValueSetter> setters) throws MuleException {
    for (ValueSetter setter : setters) {
      setter.set(target, result);
    }
  }

  /**
   * Creates the instances to be produced
   */
  protected abstract T instantiateObject();
}
