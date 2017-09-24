/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverUtils.resolveRecursively;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;

import java.util.Optional;
import java.util.function.Function;

/**
 * {@link ValueResolver} implementation for {@link ParameterResolver} that wraps another {@link ValueResolver}
 *
 * @since 4.0
 * @see ParameterResolver
 */
public class ParameterResolverValueResolverWrapper<T>
    implements ValueResolver<ParameterResolver<T>>, Initialisable, MuleContextAware {

  private ValueResolver<T> resolver;
  private MuleContext muleContext;
  private Reference<Function<ValueResolvingContext, ParameterResolver>> parameterResolverSupplier = new Reference<>();

  public ParameterResolverValueResolverWrapper(ValueResolver resolver) {
    this.resolver = resolver;
    Function<ValueResolvingContext, ParameterResolver> parameterResolverFactory = (context) -> new ParameterResolver<T>() {

      @Override
      public T resolve() {
        try {
          return resolveRecursively((ValueResolver<T>) resolver, context);
        } catch (MuleException e) {
          throw new MuleRuntimeException(e);
        }
      }

      @Override
      public Optional<String> getExpression() {
        return resolver instanceof ExpressionBasedValueResolver
            ? ofNullable(((ExpressionBasedValueResolver) resolver).getExpression()) : empty();
      }
    };

    if (resolver.isDynamic()) {
      parameterResolverSupplier.set(parameterResolverFactory);
    } else {
      parameterResolverSupplier.set(context -> {
        ParameterResolver staticResolver = parameterResolverFactory.apply(context);
        Function<ValueResolvingContext, ParameterResolver> valueResolvingContextParameterResolverFunction =
            (ctx) -> staticResolver;
        parameterResolverSupplier.set(valueResolvingContextParameterResolverFunction);
        return staticResolver;
      });
    }
  }

  /**
   * Resolves the value of {@link this#resolver} using the given {@link CoreEvent} and wraps it into a
   * {@link StaticParameterResolver}
   *
   * @param context a {@link ValueResolvingContext} to resolve the {@link ValueResolver}
   * @return an {@link ParameterResolver} with the resolved value
   * @throws MuleException if it fails to resolve the value
   */
  @Override
  public ParameterResolver<T> resolve(ValueResolvingContext context) throws MuleException {
    return parameterResolverSupplier.get().apply(context);
  }

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
