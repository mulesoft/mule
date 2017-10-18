/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverUtils.resolveValue;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.ObjectBuilder;

import com.google.common.collect.ImmutableMap;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A {@link ValueResolver} which is based on associating a set of keys -&gt; {@link ValueResolver} pairs. The result of evaluating
 * this resolver is a {@link ResolverSetResult}.
 * <p>
 * The general purpose of this class is to repeatedly evaluate a set of {@link ValueResolver}s which results are to be used in the
 * construction of an object, so that the structure of such can be described only once (by the set of keys and
 * {@link ValueResolver}s but evaluated many times. With this goal in mind is that the return value of this resolver will always
 * be a {@link ResolverSetResult} which then can be used by a {@link ObjectBuilder} to generate an actual object.
 * <p>
 * Instances of this class are to be considered thread safe and reusable
 *
 * @since 3.7.0
 */
public class ResolverSet implements ValueResolver<ResolverSetResult>, Initialisable {

  private Map<String, ValueResolver<?>> resolvers = new LinkedHashMap<>();
  private boolean dynamic = false;
  private final MuleContext muleContext;

  public ResolverSet(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  /**
   * Links the given {@link ValueResolver} to the given identifying {@code key}.
   *
   * @param key      a non-blank {@code key}
   * @param resolver a not {@code null} {@link ValueResolver}
   * @return this resolver set to allow chaining
   * @throws IllegalStateException if the {@code key} was already associated to a {@code resolver}
   * @throws IllegalArgumentException if either {@code key} is empty or {@code resolver} is {@code null}
   */
  public ResolverSet add(String key, ValueResolver resolver) {
    checkArgument(!isBlank(key), "A key for a ValueResolver cannot be blank");
    checkArgument(resolver != null, "Resolver cannot be null");

    if (resolvers.put(key, resolver) != null) {
      throw new IllegalStateException("A value was already given for key " + key);
    }

    if (!dynamic && resolver.isDynamic()) {
      dynamic = true;
    }
    return this;
  }

  /**
   * Links all the given {@link ValueResolver}s to the given identifying {@code key}.
   *
   * @param resolvers a not {@code null} {@link Map} of {@code key}-{@link ValueResolver}
   * @return this resolver set to allow chaining
   * @throws IllegalStateException if any of the {@code key}s were already associated to a {@code resolver}
   * @throws IllegalArgumentException if either {@code key} is empty or {@code resolver} is {@code null},
   * on any of the entries.
   * @see ResolverSet#add(String, ValueResolver)
   */
  public ResolverSet addAll(Map<String, ValueResolver<?>> resolvers) {
    checkArgument(resolvers != null, "Resolvers to be added cannot be null");
    resolvers.forEach(this::add);
    return this;
  }

  /**
   * Whether at least one of the given {@link ValueResolver} are dynamic
   *
   * @return {@code true} if at least one resolver is dynamic. {@code false} otherwise
   */
  @Override
  public boolean isDynamic() {
    return dynamic;
  }

  /**
   * Evaluates all the added {@link ValueResolver}s and returns the results into a {@link ResolverSetResult}
   *
   * @param context a not {@code null} {@link ValueResolvingContext}
   * @return a {@link ResolverSetResult}
   * @throws MuleException if an error occurs creating the {@link ResolverSetResult}
   */
  @Override
  public ResolverSetResult resolve(ValueResolvingContext context) throws MuleException {
    ResolverSetResult.Builder builder = getResolverSetBuilder();

    for (Map.Entry<String, ValueResolver<?>> entry : resolvers.entrySet()) {
      builder.add(entry.getKey(), resolveValue(entry.getValue(), context));
    }

    return builder.build();
  }

  /**
   * Creates a new instance of {@link ResolverSet} containing all the resolvers
   * of both {@code this} {@link ResolverSet} and the given {@code resolverSet}
   *
   * @param resolverSet a {@link ResolverSet} to merge with {@code this} {@link ResolverSet}
   * @return a new instance of {@link ResolverSet} containing all the resolvers.
   * @throws IllegalStateException if a duplicated {@code key} is found during the merge
   * @throws IllegalArgumentException if the given {@code resolverSet} is {@code null}
   */
  public ResolverSet merge(ResolverSet resolverSet) {
    ResolverSet newResolverSet = new ResolverSet(muleContext);
    newResolverSet.addAll(resolvers);
    newResolverSet.addAll(resolverSet.getResolvers());
    return newResolverSet;
  }

  public Map<String, ValueResolver<?>> getResolvers() {
    return ImmutableMap.copyOf(resolvers);
  }

  public void initialise() {

    try {
      for (ValueResolver valueResolver : resolvers.values()) {
        muleContext.getInjector().inject(valueResolver);
        LifecycleUtils.initialiseIfNeeded(valueResolver);
      }
    } catch (MuleException e) {
      throw new MuleRuntimeException(e);
    }
  }

  ResolverSetResult.Builder getResolverSetBuilder() {
    return ResolverSetResult.newBuilder();
  }

}
