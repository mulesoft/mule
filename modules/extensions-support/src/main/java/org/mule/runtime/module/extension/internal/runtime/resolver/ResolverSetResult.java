/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.extension.api.introspection.parameter.ParameterModel;

import com.google.common.base.Objects;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class represents the outcome of the evaluation of a {@link ResolverSet}.
 * This class maps a set of {@link ParameterModel} to a set of result {@link Object}s.
 * <p/>
 * This classes {@link #equals(Object)} and {@link #hashCode()} methods have been redefined
 * to be consistent with the result objects. This is so that given two instances of this class
 * you can determine if the evaluations they represent have an equivalent outcome
 * <p/>
 * Instances of this class can only be created through a {@link Builder}
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
        private Map<ParameterModel, Object> values = new LinkedHashMap<>();

        private Builder()
        {
        }

        /**
         * Adds a new result {@code value} for the given {@code parameter}
         *
         * @param parameterModel a not {@code null} {@link ParameterModel parameterModel}
         * @param value     the associated value. It can be {@code null}
         * @return this builder
         * @throws IllegalArgumentException is {@code parameter} is {@code null}
         */
        public Builder add(ParameterModel parameterModel, Object value)
        {
            checkArgument(parameterModel != null, "parameter cannot be null");
            values.put(parameterModel, value);
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

    private final Map<ParameterModel, Object> evaluationResult;
    private final Map<String, Object> parameterToResult;
    private final int hashCode;

    private ResolverSetResult(Map<ParameterModel, Object> evaluationResult, int hashCode)
    {
        this.evaluationResult = evaluationResult;
        parameterToResult = new HashMap<>(evaluationResult.size());
        for (Map.Entry<ParameterModel, Object> entry : evaluationResult.entrySet())
        {
            parameterToResult.put(entry.getKey().getName(), entry.getValue());
        }

        this.hashCode = hashCode;
    }

    /**
     * Returns the value associated with the given {@code parameter}
     *
     * @param parameterModel a {@link ParameterModel} which was registered with the builder
     * @return the value associated to that {@code parameter} or {@code null} if no such association exists
     */
    public Object get(ParameterModel parameterModel)
    {
        return evaluationResult.get(parameterModel);
    }

    /**
     * Returns the value associated with the {@link ParameterModel} of the given {@code parameterName}
     *
     * @param parameterName the name of the {@link ParameterModel} which value you seek
     * @return the value associated to that {@code parameterName} or {@code null} if no such association exists
     */
    public Object get(String parameterName)
    {
        return parameterToResult.get(parameterName);
    }

    public Map<ParameterModel, Object> asMap()
    {
        return Collections.unmodifiableMap(evaluationResult);
    }

    /**
     * Defines equivalence by comparing the values in both objects. To consider that
     * two instances are equal, they both must have equivalent results for every
     * registered {@link ParameterModel}. Values will be tested for equality using
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
            for (Map.Entry<ParameterModel, Object> entry : evaluationResult.entrySet())
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
