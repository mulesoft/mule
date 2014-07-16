/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting.expression;

import org.mule.api.MuleMessage;

/**
 * An {@link org.mule.api.expression.ExpressionEvaluator} that allows the user to define Groovy expressions to extract
 * data from the current message.
 *
 * If a POJO is passed in it is accessible from the 'payload' namespace.  If a {@link MuleMessage} instance is used then
 * it is accessible from the message' namespace and the 'payload' namespace is also available.
 */
public class GroovyExpressionEvaluator extends AbstractScriptExpressionEvaluator
{
    public static final String NAME = "groovy";

    @Override
    public String getName()
    {
        return NAME;
    }
}
