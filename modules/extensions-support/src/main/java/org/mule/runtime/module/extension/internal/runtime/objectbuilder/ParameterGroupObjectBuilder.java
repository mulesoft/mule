/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.objectbuilder;

import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.module.extension.internal.introspection.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.runtime.EventedExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class ParameterGroupObjectBuilder<T> extends DefaultObjectBuilder<T> {

  private final ParameterGroupDescriptor groupDescriptor;

  public ParameterGroupObjectBuilder(ParameterGroupDescriptor groupDescriptor) {
    super(groupDescriptor.getType().getDeclaringClass());
    this.groupDescriptor = groupDescriptor;
  }

  public T build(EventedExecutionContext executionContext) throws MuleException {
    return doBuild(executionContext::hasParameter, executionContext::getParameter, executionContext.getEvent());
  }

  public T build(ResolverSetResult result) throws MuleException {
    final Map<String, Object> resultMap = result.asMap();
    return doBuild(resultMap::containsKey, resultMap::get, getInitialiserEvent());

  }

  private T doBuild(Predicate<String> hasParameter, Function<String, Object> parameters, Event event) throws MuleException {
    groupDescriptor.getType().getAnnotatedFields(Parameter.class).forEach(field -> {
      String fieldName = field.getName();
      if (hasParameter.test(fieldName)) {
        addPropertyResolver(fieldName, new StaticValueResolver<>(parameters.apply(fieldName)));
      }
    });

    return build(event);
  }
}
