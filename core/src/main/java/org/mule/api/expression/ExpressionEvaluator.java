/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.expression;

import org.mule.api.MuleMessage;
import org.mule.api.NamedObject;
import org.mule.transformer.types.TypedValue;

/**
 * <code>ExpressionEvaluator</code> extracts a property from the message in a generic
 * way. i.e. composite properties can be pulled and aggregated depending on this
 * strategy. This can be used to extract Correlation Ids, Message Ids etc.
 *
 * These objects are used to execute property expressions (usually on the
 * current message) at runtime to extracts dynamic value.
 * 
 * ExpressionEvaluator names most contain only letters, dashes or underscores.
 */
public interface ExpressionEvaluator extends NamedObject
{
    /**
     * Extracts a single property from the message
     * 
     * @param expression the property expression or expression
     * @param message the message to extract from
     * @return the result of the extraction or null if the property was not found
     */
    Object evaluate(String expression, MuleMessage message);

    /**
     * Extracts a single typed property from the message
     *
     * @param expression the property expression or expression
     * @param message the message to extract from
     * @return a no null data type for the extracted value
     */
    TypedValue evaluateTyped(String expression, MuleMessage message);
}
