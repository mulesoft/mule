/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.util.Collections.emptyMap;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;

import java.util.Map;

public class NullResolverSet extends ResolverSet {

  private static final ResolverSetResult EMPTY_RESULT = new ResolverSetResult(emptyMap());
  public static final ResolverSet INSTANCE = new NullResolverSet();

  private NullResolverSet() {
    super(null);
  }

  @Override
  public ResolverSet add(String key, ValueResolver resolver) {
    throw new UnsupportedOperationException("Immutable NullResolverSet");
  }

  @Override
  public ResolverSet addAll(Map<String, ValueResolver<?>> resolvers) {
    throw new UnsupportedOperationException("Immutable NullResolverSet");
  }

  @Override
  public boolean isDynamic() {
    return false;
  }

  @Override
  public ResolverSetResult resolve(ValueResolvingContext context) throws MuleException {
    return EMPTY_RESULT;
  }

  @Override
  public ResolverSet merge(ResolverSet resolverSet) {
    return resolverSet;
  }

  @Override
  public Map<String, ValueResolver<?>> getResolvers() {
    return emptyMap();
  }

  @Override
  public void initialise() throws InitialisationException {}
}
