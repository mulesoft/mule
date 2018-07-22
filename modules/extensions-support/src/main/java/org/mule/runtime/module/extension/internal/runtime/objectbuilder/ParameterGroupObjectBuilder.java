/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.objectbuilder;

import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext.from;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedExecutionContext;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An {@link ObjectBuilder} used to build pojos which are used with the {@link ParameterGroup} annotation.
 *
 * @param <T> the generic type of the object being built
 */
public class ParameterGroupObjectBuilder<T> extends DefaultObjectBuilder<T> {

  private final ParameterGroupDescriptor groupDescriptor;


  /**
   * Create a new instance
   *
   * @param groupDescriptor the descriptor for the group being built
   * @param reflectionCache the cache for expensive reflection lookups
   */
  public ParameterGroupObjectBuilder(ParameterGroupDescriptor groupDescriptor, ReflectionCache reflectionCache) {
    super((Class<T>) groupDescriptor.getType().getDeclaringClass().get(), reflectionCache);
    this.groupDescriptor = groupDescriptor;
  }

  public T build(EventedExecutionContext executionContext) throws MuleException {
    return doBuild(executionContext::hasParameter, executionContext::getParameter,
                   from(executionContext.getEvent(), executionContext.getConfiguration()));
  }

  public T build(ResolverSetResult result) throws MuleException {
    final Map<String, Object> resultMap = result.asMap();
    CoreEvent initialiserEvent = null;
    try {
      initialiserEvent = getInitialiserEvent();
      return doBuild(resultMap::containsKey, resultMap::get, from(initialiserEvent));
    } finally {
      if (initialiserEvent != null) {
        ((BaseEventContext) initialiserEvent.getContext()).success();
      }
    }
  }

  private T doBuild(Predicate<String> hasParameter, Function<String, Object> parameters, ValueResolvingContext context)
      throws MuleException {
    reflectionCache.fieldElementsFor(groupDescriptor).forEach(field -> {
      String name = field.getName();
      if (hasParameter.test(name)) {
        addPropertyResolver(field.getField().get(), new StaticValueResolver<>(parameters.apply(name)));
      }
    });

    return build(context);
  }
}
