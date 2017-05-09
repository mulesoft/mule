/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.util.Collections.unmodifiableMap;

import java.util.Map;

/**
 * {@link ResolverSetResult} extension which adds the capability of being able to know the hash value of the
 * resolved values. This is required for Configuration and Connection Provider cases to be able to cache the
 * instances that are based on dynamic parameters.
 * <p>
 * This classes {@link #equals(Object)} and {@link #hashCode()} methods have been redefined to be consistent with the result
 * objects. This is so that given two instances of this class you can determine if the evaluations they represent have an
 * equivalent outcome
 *
 * @since 4.0
 */
public class HashedResolverSetResult extends ResolverSetResult {

  private int hashCode;

  /**
   * A builder for creating instances of {@link HashedResolverSetResult}. You should use a new builder for each
   * {@link HashedResolverSetResult} you want to create
   *
   * @since 4.0
   */
  public static final class Builder extends ResolverSetResult.Builder {

    private int hashCode = 1;

    private Builder() {
      super();
    }

    /**
     * {@inheritDoc}
     */
    public Builder add(String key, Object value) {
      super.add(key, value);
      hashCode = calculateValueHash(hashCode, value);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResolverSetResult build() {
      return new HashedResolverSetResult(unmodifiableMap(values), hashCode);
    }
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  private HashedResolverSetResult(Map<String, Object> evaluationResult, int hashCode) {
    super(evaluationResult);
    this.hashCode = hashCode;
  }

  /**
   * A hashCode calculated based on the results
   *
   * @return a hashCode
   */
  @Override
  public int hashCode() {
    return hashCode;
  }

}
