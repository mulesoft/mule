/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    /**
     * Gets the name of the object
     *
     * @return the name of the object
     */
    public String getName()
    {
        return NAME;
    }
}
