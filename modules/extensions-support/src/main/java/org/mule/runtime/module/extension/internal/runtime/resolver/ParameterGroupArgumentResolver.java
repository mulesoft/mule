/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkState;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedExecutionContext;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.ParameterGroupObjectBuilder;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

public final class ParameterGroupArgumentResolver<T> implements ArgumentResolver<T> {

  private final ParameterGroupObjectBuilder<T> parameterGroupObjectBuilder;

  public ParameterGroupArgumentResolver(ParameterGroupDescriptor group,
                                        ReflectionCache reflectionCache,
                                        ExpressionManager expressionManager) {
    checkState(group.getType().isInstantiable(), "Class %s cannot be instantiated.");
    this.parameterGroupObjectBuilder = new ParameterGroupObjectBuilder<>(group, reflectionCache, expressionManager);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T resolve(ExecutionContext executionContext) {
    try {
      return parameterGroupObjectBuilder.build((EventedExecutionContext) executionContext);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create parameter group"), e);
    }
  }
}
