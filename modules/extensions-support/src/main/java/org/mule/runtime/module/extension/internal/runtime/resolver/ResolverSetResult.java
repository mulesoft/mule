/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.util.Collections.unmodifiableMap;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;

import com.google.common.base.Objects;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class represents the outcome of the evaluation of a {@link ResolverSet}. This class maps a set of {@link ParameterModel}
 * to a set of result {@link Object}s.
 * <p>
 * Instances of this class can only be created through a {@link Builder} obtained via {@link #newBuilder()}
 *
 * @since 3.7.0
 */
public class ResolverSetResult {

  /**
   * A builder for creating instances of {@link ResolverSetResult}. You should use a new builder for each
   * {@link ResolverSetResult} you want to create
   *
   * @since 3.7.0
   */
  public static class Builder {

    LinkedHashMap<String, Object> values = new LinkedHashMap<>();

    Builder() {}

    /**
     * Adds a new result {@code value} for the given {@code key}
     *
     * @param key a not {@code null} key for the value
     * @param value the associated value. It can be {@code null}
     * @return this builder
     * @throws IllegalArgumentException is {@code parameter} is {@code null}
     */
    public Builder add(String key, Object value) {
      checkArgument(key != null, "parameter cannot be null");
      values.put(key, value);
      return this;
    }

    /**
     * Creates a new {@link ResolverSetResult}
     *
     * @return the build instance
     */
    public ResolverSetResult build() {
      return new ResolverSetResult(unmodifiableMap(values));
    }
  }

  /**
   * Creates a new {@link Builder} instance. You should use a new builder per each instance you want to create
   *
   * @return a {@link Builder}
   */
  public static Builder newBuilder() {
    return new Builder();
  }

  private final Map<String, Object> evaluationResult;

  ResolverSetResult(Map<String, Object> evaluationResult) {
    this.evaluationResult = new HashMap<>(evaluationResult);
  }

  /**
   * Returns the value associated with the {@link ParameterModel} of the given {@code parameterName}
   *
   * @param parameterName the name of the {@link ParameterModel} which value you seek
   * @return the value associated to that {@code parameterName} or {@code null} if no such association exists
   */
  public Object get(String parameterName) {
    return evaluationResult.get(parameterName);
  }

  /**
   * Defines equivalence by comparing the values in both objects. To consider that two instances are equal, they both must have
   * equivalent results for every registered {@link ParameterModel}. Values will be tested for equality using their own
   * implementation of {@link Object#equals(Object)}. For the case of a {@code null} value, equality requires the other one to be
   * {@code null} as well.
   * <p>
   * This implementation fails fast. Evaluation is finished at the first non equal value, returning {@code false}
   *
   * @param obj the object to test for equality
   * @return whether the two objects are equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ResolverSetResult) {
      ResolverSetResult other = (ResolverSetResult) obj;
      return evaluationResult.entrySet()
          .stream()
          .allMatch(entry -> Objects.equal(entry.getValue(), other.get(entry.getKey())));
    }

    return false;
  }

  @Override
  public int hashCode() {
    int hashcode = 1;
    for (Object val : evaluationResult.values()) {
      hashcode = calculateValueHash(hashcode, val);
    }
    return hashcode;
  }

  static int calculateValueHash(int hashcode, Object val) {
    return 31 * hashcode + (val == null ? 0 : val.hashCode());
  }

  public Map<String, Object> asMap() {
    return evaluationResult;
  }
}
