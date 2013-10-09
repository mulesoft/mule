/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.expression;

import org.mule.api.MuleMessage;
import org.mule.api.NamedObject;

/**
 * <code>ExpressionEvaluator</code> extracts a property from the message in a generic
 * way. i.e. composite properties can be pulled and aggregated depending on this
 * strategy. This can be used to extract Correlation Ids, Message Ids etc.
 *
 * These objects are used to execute property expressions (usually on the
 * current message) at runtime to extracta dynamic value.
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

}
