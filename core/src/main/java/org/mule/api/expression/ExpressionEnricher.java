/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
