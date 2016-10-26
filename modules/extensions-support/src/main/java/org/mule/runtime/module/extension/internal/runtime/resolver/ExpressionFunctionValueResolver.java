/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;

import java.util.function.Function;

/**
 * A {@link ValueResolver} which evaluates an {@link ExpressionFunction} for later resolution of a MEL expression.
 *
 * @since 4.0
 */
public final class ExpressionFunctionValueResolver<T> implements ValueResolver<Function<Event, T>> {

  private final String exp;
  private final MetadataType metadataType;
  private final MuleContext muleContext;

  public ExpressionFunctionValueResolver(String exp, MetadataType metadataType, MuleContext muleContext) {
    this.exp = exp;
    this.metadataType = metadataType;
    this.muleContext = muleContext;
  }

  @Override
  public Function<Event, T> resolve(Event event) throws MuleException {
    return new ExpressionFunction<>(exp, metadataType, muleContext);
  }

  @Override
  public boolean isDynamic() {
    return false;
  }

}
