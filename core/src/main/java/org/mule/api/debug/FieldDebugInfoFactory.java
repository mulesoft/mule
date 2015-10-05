/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.debug;

import org.mule.api.MuleEvent;
import org.mule.util.AttributeEvaluator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates instances of  {@link FieldDebugInfo}
 *
 * @since 3.8.0
 */
public class FieldDebugInfoFactory
{

    private FieldDebugInfoFactory()
    {
    }

    private static final Map<Class, AttributeEvaluatorExecutor> attributeEvaluationExecutors = new HashMap<>();
    private static final AttributeEvaluatorExecutor DEFAULT_ATTRIBUTE_EVALUATOR_EXECUTOR = new ObjectAttributeEvaluatorExecutor();

    static
    {
        attributeEvaluationExecutors.put(String.class, new StringAttributeEvaluatorExecutor());
        attributeEvaluationExecutors.put(Boolean.class, new BooleanAttributeEvaluatorExecutor());
        attributeEvaluationExecutors.put(Integer.class, new IntegerAttributeEvaluatorExecutor());
        attributeEvaluationExecutors.put(Object.class, DEFAULT_ATTRIBUTE_EVALUATOR_EXECUTOR);
    }

    private interface AttributeEvaluatorExecutor
    {
        Object evaluate(MuleEvent event, AttributeEvaluator attributeEvaluator);
    }

    private static class BooleanAttributeEvaluatorExecutor implements AttributeEvaluatorExecutor
    {

        @Override
        public Object evaluate(MuleEvent event, AttributeEvaluator attributeEvaluator)
        {
            return attributeEvaluator.resolveBooleanValue(event);
        }
    }

    private static class StringAttributeEvaluatorExecutor implements AttributeEvaluatorExecutor
    {

        @Override
        public Object evaluate(MuleEvent event, AttributeEvaluator attributeEvaluator)
        {
            return attributeEvaluator.resolveStringValue(event);
        }
    }

    private static class IntegerAttributeEvaluatorExecutor implements AttributeEvaluatorExecutor
    {

        @Override
        public Object evaluate(MuleEvent event, AttributeEvaluator attributeEvaluator)
        {
            return attributeEvaluator.resolveIntegerValue(event);
        }
    }

    private static class ObjectAttributeEvaluatorExecutor implements AttributeEvaluatorExecutor
    {

        @Override
        public Object evaluate(MuleEvent event, AttributeEvaluator attributeEvaluator)
        {
            return attributeEvaluator.resolveValue(event);
        }
    }

    /**
     * Evaluates a field to provide its value
     */
    public interface FieldEvaluator
    {

        /**
         * Provides the value for a field
         *
         * @return the value for a field. Can be null
         * @throws Exception when the evaluation fails
         */
        Object evaluate() throws Exception;
    }

    /**
     * Creates a debug info for a field
     *
     * @param name      field's name. Must be a non blank {@link String}
     * @param type      field's type
     * @param evaluator evaluator used to obtain the field's value
     * @param event     event used on the evaluator to obtain the field's value
     * @return a {@link SimpleFieldDebugInfo} if the field evaluation is successful,
     *         and a {@link ErrorFieldDebugInfo} otherwise
     */
    public static FieldDebugInfo<?> createFieldDebugInfo(String name, Class type, AttributeEvaluator evaluator, MuleEvent event)
    {
        try
        {
            AttributeEvaluatorExecutor attributeEvaluatorExecutor = attributeEvaluationExecutors.get(type);
            if (attributeEvaluatorExecutor == null)
            {
                attributeEvaluatorExecutor = DEFAULT_ATTRIBUTE_EVALUATOR_EXECUTOR;
            }

            final Object value = attributeEvaluatorExecutor.evaluate(event, evaluator);

            return new SimpleFieldDebugInfo(name, type, value);
        }
        catch (Exception e)
        {
            return new ErrorFieldDebugInfo(name, type, e);
        }
    }

    /**
     * Creates a debug info for a field
     *
     * @param name      field's name. Must be a non blank {@link String}
     * @param type      field's type
     * @param evaluator evaluator used to obtain the field's value
     * @return a {@link SimpleFieldDebugInfo} if the field evaluation is successful,
     *         and a {@link ErrorFieldDebugInfo} otherwise
     */
    public static FieldDebugInfo createFieldDebugInfo(String name, Class type, FieldEvaluator evaluator)
    {
        try
        {
            return new SimpleFieldDebugInfo(name, type, evaluator.evaluate());
        }
        catch (Exception e)
        {
            return new ErrorFieldDebugInfo(name, type, e);
        }
    }

    /**
     * Creates a debug info for a simple field
     *
     * @param name  field's name. Must be a non blank {@link String}
     * @param type  field's type
     * @param value field's value
     * @return a non null {@link SimpleFieldDebugInfo}
     */
    public static SimpleFieldDebugInfo createFieldDebugInfo(String name, Class type, Object value)
    {
        return new SimpleFieldDebugInfo(name, type, value);
    }


    /**
     * Creates a debug info for an object field
     *
     * @param name  field's name. Must be a non blank {@link String}
     * @param type  field's type
     * @param fields
     * @return a non null {@link ObjectFieldDebugInfo}
     */
    public static ObjectFieldDebugInfo createFieldDebugInfo(String name, Class type, List<FieldDebugInfo> fields)
    {
        return new ObjectFieldDebugInfo(name, type, fields);
    }

    /**
     * Creates an error debug info for a field
     *
     * @param name  field's name. Must be a non blank {@link String}
     * @param type  field's type
     * @param value value  error found during the value resolution of the field
     * @return a non null {@link ErrorFieldDebugInfo}
     */
    public static ErrorFieldDebugInfo createFieldDebugInfo(String name, Class type, Exception value)
    {
        return new ErrorFieldDebugInfo(name, type, value);
    }
}
