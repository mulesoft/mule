/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.runtime.resolver;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.extensions.introspection.Parameter;

import com.google.common.base.Objects;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * This class represents the outcome of the evaluation of a {@link ResolverSet}.
 * This class maps a set of {@link Parameter} to a set of result {@link Object}s.
 * <p/>
 * This classes {@link #equals(Object)} and {@link #hashCode()} methods have been redefined
 * to be consistent with the result objects. This is so that given two instances of this class
 * you can determine if the evaluations they represent have an equivalent outcome
 * <p/>
 * Instances of this class are immutable and can only be created through a {@link Builder}
 * obtained via {@link #newBuilder()}
 *
 * @since 3.7.0
 */
public class ResolverSetResult
{

    /**
     * A builder for creating instances of {@link ResolverSetResult}. You should use a new
     * builder for each {@link ResolverSetResult} you want to create
     *
     * @since 3.7.0
     */
    public static final class Builder
    {

        private int hashCode = 1;
        private Map<Parameter, Object> values = new LinkedHashMap<>();

        private Builder()
        {
        }

        /**
         * Adds a new result {@code value} for the given {@code parameter}
         *
         * @param parameter a not {@code null} {@link Parameter}
         * @param value     the associated value. It can be {@code null}
         * @return this builder
         * @throws IllegalArgumentException is {@code parameter} is {@code null}
         */
        public Builder add(Parameter parameter, Object value)
        {
            checkArgument(parameter != null, "parameter cannot be null");
            values.put(parameter, value);
            hashCode = 31 * hashCode + (value == null ? 0 : value.hashCode());
            return this;
        }

        /**
         * Creates a new {@link ResolverSetResult}
         *
         * @return the build instance
         */
        public ResolverSetResult build()
        {
            return new ResolverSetResult(Collections.unmodifiableMap(values), hashCode);
        }
    }

    /**
     * Creates a new {@link Builder} instance. You should use a new builder
     * per each instance you want to create
     *
     * @return a {@link Builder}
     */
    public static Builder newBuilder()
    {
        return new Builder();
    }

    private final Map<Parameter, Object> evaluationResult;
    private final Map<String, Object> parameterName2Result;
    private final int hashCode;

    private ResolverSetResult(Map<Parameter, Object> evaluationResult, int hashCode)
    {
        this.evaluationResult = evaluationResult;
        parameterName2Result = new HashMap<>(evaluationResult.size());
        for (Map.Entry<Parameter, Object> entry : evaluationResult.entrySet())
        {
            parameterName2Result.put(entry.getKey().getName(), entry.getValue());
        }

        this.hashCode = hashCode;
    }

    /**
     * Returns the value associated with the given {@code parameter}
     *
     * @param parameter a {@link Parameter} which was registered with the builder
     * @return the value associated to that {@code parameter}
     * @throws NoSuchElementException if the {@code parameter} has not been registered through the builder
     */
    public Object get(Parameter parameter)
    {
        if (!evaluationResult.containsKey(parameter))
        {
            throw new NoSuchElementException("This result contains no information for the parameter: " + parameter.getName());
        }

        return evaluationResult.get(parameter);
    }

    /**
     * Returns the value associated with the {@link Parameter} of the given {@code parameterName}
     *
     * @param parameterName the name of the {@link Parameter} which value you seek
     * @return the value associated to that {@code parameterName}
     * @throws NoSuchElementException if the {@code parameterName} has not been registered through the builder
     */
    public Object get(String parameterName)
    {
        if (!parameterName2Result.containsKey(parameterName))
        {
            throw new NoSuchElementException("This result contains no information for the parameter: " + parameterName);
        }

        return parameterName2Result.get(parameterName);
    }

    public Map<Parameter, Object> asMap()
    {
        return Collections.unmodifiableMap(evaluationResult);
    }

    /**
     * Defines equivalence by comparing the values in both objects. To consider that
     * two instances are equal, they both must have equivalent results for every
     * registered {@link Parameter}. Values will be tested for equality using
     * their own implementation of {@link Object#equals(Object)}. For the case of a
     * {@code null} value, equality requires the other one to be {@code null} as well.
     * <p/>
     * This implementation fails fast. Evaluation is finished at the first non equal
     * value, returning {@code false}
     *
     * @param obj the object to test for equality
     * @return whether the two objects are equal
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof ResolverSetResult)
        {
            ResolverSetResult other = (ResolverSetResult) obj;
            for (Map.Entry<Parameter, Object> entry : evaluationResult.entrySet())
            {
                Object otherValue = other.get(entry.getKey());
                if (!Objects.equal(entry.getValue(), otherValue))
                {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    /**
     * A hashCode calculated based on the results
     *
     * @return a hashCode
     */
    @Override
    public int hashCode()
    {
        return hashCode;
    }
}
