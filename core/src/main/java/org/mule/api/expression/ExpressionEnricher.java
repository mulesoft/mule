/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.expression;

import org.mule.api.MuleMessage;
import org.mule.api.NameableObject;

/**
 * <code>ExpressionEnricher</code> enriches a message using the extensible mule
 * expression framework allowing the contribution of additional enrichers.
 */
public interface ExpressionEnricher extends NameableObject
{

    /**
     * Enriches the message with the object
     * 
     * @param expression
     * @param message
     * @param object
     */
    void enrich(String expression, MuleMessage message, Object object);

}
