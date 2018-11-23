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
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.checkInstantiable;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.el.ExpressionManager;
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
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An {@link ObjectBuilder} used to build pojos which are used with the {@link ParameterGroup} annotation.
 *
 * @param <T> the generic type of the object being built
 */
public class ParameterGroupObjectBuilder<T> {

  private final Class<T> prototypeClass;
  private final ExpressionManager expressionManager;
  private final List<FieldElement> groupDescriptorFields;

  /**
   * Create a new instance
   *
   * @param groupDescriptor the descriptor for the group being built
   * @param reflectionCache the cache for expensive reflection lookups
   */
  public ParameterGroupObjectBuilder(ParameterGroupDescriptor groupDescriptor,
                                     ReflectionCache reflectionCache,
                                     ExpressionManager expressionManager) {
    this.prototypeClass = (Class<T>) groupDescriptor.getType().getDeclaringClass().get();
    checkInstantiable(prototypeClass, reflectionCache);
    this.expressionManager = expressionManager;
    this.groupDescriptorFields = reflectionCache.fieldElementsFor(groupDescriptor);
  }

  public T build(EventedExecutionContext executionContext) throws MuleException {
    try (ValueResolvingContext context = ValueResolvingContext.builder(executionContext.getEvent())
        .withExpressionManager(expressionManager)
        .withConfig(executionContext.getConfiguration())
        .build()) {
      return doBuild(executionContext::hasParameter, executionContext::getParameter, context);
    }
  }

  public T build(ResolverSetResult result) throws MuleException {
    final Map<String, Object> resultMap = result.asMap();
    CoreEvent initializerEvent = null;
    ValueResolvingContext context = null;
    try {
      initializerEvent = getInitialiserEvent();
      context = ValueResolvingContext.builder(initializerEvent).build();
      return doBuild(resultMap::containsKey, resultMap::get, context);
    } finally {
      if (initializerEvent != null) {
        ((BaseEventContext) initializerEvent.getContext()).success();
      }
      if (context != null) {
        context.close();
      }
    }
  }

  private T doBuild(Predicate<String> hasParameter, Function<String, Object> parameters, ValueResolvingContext context)
      throws MuleException {
    T object = createInstance(prototypeClass);

    for (FieldElement field : groupDescriptorFields) {
      String name = field.getName();
      if (hasParameter.test(name)) {
        Object resolvedValue = resolveValue(new StaticValueResolver<>(parameters.apply(name)), context);
        Object value = context == null || context.resolveCursors() ? resolveCursor(resolvedValue) : resolvedValue;
        field.set(object, value);
      }
    }

    return object;
  }
}
