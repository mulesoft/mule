/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.checkInstantiable;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.ParameterGroupObjectBuilder;

public final class ParameterGroupArgumentResolver<T> implements ArgumentResolver<T> {

  private final ParameterGroupDescriptor group;

  public ParameterGroupArgumentResolver(ParameterGroupDescriptor group) {
    checkInstantiable(group.getType().getDeclaringClass());
    this.group = group;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T resolve(ExecutionContext executionContext) {
    try {
      return new ParameterGroupObjectBuilder<T>(group).build((EventedExecutionContext) executionContext);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create parameter group"), e);
    }
  }
}
