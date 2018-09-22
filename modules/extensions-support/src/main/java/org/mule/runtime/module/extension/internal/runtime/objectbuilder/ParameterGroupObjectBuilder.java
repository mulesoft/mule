/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.objectbuilder;

import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.runtime.objectbuilder.ObjectBuilderUtils.createInstance;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverUtils.resolveCursor;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverUtils.resolveValue;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext.from;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.checkInstantiable;
import static org.springframework.util.ReflectionUtils.setField;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.module.extension.api.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedExecutionContext;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.List;
import java.util.Map;

/**
 * An {@link ObjectBuilder} used to build pojos which are used with the {@link ParameterGroup} annotation.
 *
 * @param <T> the generic type of the object being built
 */
public class ParameterGroupObjectBuilder<T> {

  private final Class<T> prototypeClass;
  private final List<FieldElement> groupDescriptorFields;

  /**
   * Create a new instance
   *
   * @param groupDescriptor the descriptor for the group being built
   * @param reflectionCache the cache for expensive reflection lookups
   */
  public ParameterGroupObjectBuilder(ParameterGroupDescriptor groupDescriptor, ReflectionCache reflectionCache) {
    this.prototypeClass = (Class<T>) groupDescriptor.getType().getDeclaringClass().get();
    checkInstantiable(prototypeClass, reflectionCache);
    this.groupDescriptorFields = reflectionCache.fieldElementsFor(groupDescriptor);
    this.groupDescriptorFields.forEach(f -> f.getField().ifPresent(field -> field.setAccessible(true)));
  }

  public T build(EventedExecutionContext executionContext) throws MuleException {
    Map<String, Object> params = executionContext.getParameters();
    return doBuild(params, from(executionContext.getEvent(), executionContext.getConfiguration()));
  }

  public T build(ResolverSetResult result) throws MuleException {
    final Map<String, Object> resultMap = result.asMap();
    CoreEvent initialiserEvent = null;
    try {
      initialiserEvent = getInitialiserEvent();
      return doBuild(resultMap, from(initialiserEvent));
    } finally {
      if (initialiserEvent != null) {
        ((BaseEventContext) initialiserEvent.getContext()).success();
      }
    }
  }

  private T doBuild(Map<String, Object> resultMap, ValueResolvingContext context)
      throws MuleException {
    T object = createInstance(prototypeClass);

    for (FieldElement field : groupDescriptorFields) {
      Object value = resultMap.get(field.getName());
      if (value != null) {
        Object resolvedValue = resolveValue(new StaticValueResolver<>(value), context);
        setField(field.getField().get(), object,
                 context == null || context.resolveCursors() ? resolveCursor(resolvedValue) : resolvedValue);
      }
    }

    return object;
  }
}
