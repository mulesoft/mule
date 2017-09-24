/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverUtils.resolveRecursively;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.CoreEvent;

/**
 * {@link ValueResolver} implementation for {@link TypedValue} that are not resolved from an
 * expression.
 * <p>
 * This {@link TypedValueValueResolverWrapper} delegates the resolution to the given {@link ValueResolver} from
 * the constructor.
 *
 * @since 4.0
 * @see TypedValue
 */
public final class TypedValueValueResolverWrapper<T> implements ValueResolver<TypedValue<T>>, Initialisable, MuleContextAware {

  private ValueResolver resolver;

  MuleContext muleContext;

  public TypedValueValueResolverWrapper(ValueResolver resolver) {
    this.resolver = resolver;
  }

  /**
   * Resolves the value of {@link this#resolver} using the given {@link CoreEvent} and returns the correspondent
   * {@link TypedValue}
   *
   * @param context a {@link ValueResolvingContext} to resolve the {@link ValueResolver}
   * @return The {@link TypedValue} of the resolved value
   * @throws MuleException if it fails to resolve the value
   */
  @Override
  public TypedValue<T> resolve(ValueResolvingContext context) throws MuleException {
    return TypedValue.of((T) resolveRecursively(resolver, context));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDynamic() {
    return resolver.isDynamic();
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(resolver, true, muleContext);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}
