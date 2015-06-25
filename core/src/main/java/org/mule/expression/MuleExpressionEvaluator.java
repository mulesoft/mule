/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.TypedValue;

/**
 * This evaluator provide a powerful expression language for querying mule information
 * at runtime.  It provides a unified language for querying message properties, attachments
 * payload, Mule context information such as the current service or endpoint and access to
 * the registry. Here are some examples:
 *
 * #[mule:message.headers(foo, bar)] - retrieves two headers 'foo' and 'bar' and returns a Map.
 *
 * #[mule:message.attachments-list(attach1, attach2*)] - retrieves two named attachments in a List.  The star on 'attach2'
 * indicates that it is optional
 *
 * #[mule:message.headers(all)] - retrieves all headers and returns as a Map.
 *
 * #[mule:message.payload(org.dom4j.Document)] - return the payload and convert it to a org.dom4j.Document.
 *
 * #[mule:message.correlationId] - return the the correlationId on the message
 *
 * #[mule:message.map-payload(foo)] - expects a Map payload object and will retrive a property 'foo' from the map.
 *
 * #[mule.context.serviceName] - returns the current service Name
 *
 * #[mule.context.modelName] - returns the current model Name
 *
 * #[mule.context.workingDir] - returns the working Directory
 *
 * #[mule.context.serverId] - returns the current server ID
 *
 * #[mule.registry.apple] - returns an object called 'apple' from the registry
 *
 * #[mule.registry.apple*] - returns an object called 'apple' from the registry but is optional
 *
 * #[mule.registry.apple.washed] - returns the property 'washed on an object called 'apple' in the registry
 *
 * @deprecated MuleExpressionEvaluator is a wrapper of the default expression evaluators which ends up redirecting
 * to the {@link org.mule.expression.DefaultExpressionManager} evaluate method. It has been deprecated since it
 * only provides one more way to accomplish the same result.
 * Instead use the default expression evaluators, for example:
 *
 * #[mule:message.headers(foo, bar)] - replaced by #[headers:foo,bar]
 *
 * #[mule:message.attachments-list(attach1, attach2*)] - replaced by #[attachments-list:attach1,attach2*]
 *
 * #[mule:context.serviceName] - replaced by #[context:serviceName]
 *
 * Check the <a href="http://www.mulesoft.org/documentation/display/MULE3USER/Expressions+Configuration+Reference">
 * Expressions configuration reference</a> for more examples of the default expression evaluators syntax.
 *
 */
@Deprecated
public class MuleExpressionEvaluator implements ExpressionEvaluator, MuleContextAware
{
    public static final String NAME = "mule";

    private MuleContext muleContext;

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public Object evaluate(String expression, MuleMessage message)
    {
        if(expression==null)
        {
            return message;
        }
        int i = expression.indexOf(".");

        ExpressionConfig config = getExpressionConfig(expression.substring(0, i), expression.substring(i+1));
        String fullExpression = config.getFullExpression(muleContext.getExpressionManager());
        return muleContext.getExpressionManager().evaluate(fullExpression, message);
    }

    @Override
    public TypedValue evaluateTyped(String expression, MuleMessage message)
    {
        if (expression == null)
        {
            return new TypedValue(message, DataTypeFactory.create(message.getClass(), null));
        }
        int i = expression.indexOf(".");

        ExpressionConfig config = getExpressionConfig(expression.substring(0, i), expression.substring(i + 1));
        String fullExpression = config.getFullExpression(muleContext.getExpressionManager());
        return muleContext.getExpressionManager().evaluateTyped(fullExpression, message);
    }

    protected ExpressionConfig getExpressionConfig(String eval, String expression)
    {

        int i = expression.indexOf("(");
        int x = expression.indexOf(".");
        if(x > 0 && x < i)
        {
            eval = expression.substring(0, x);
            expression = expression.substring(x+1);
        }

        if(i > 0)
        {
            eval = expression.substring(0, i);
            expression=expression.substring(i+1, expression.length()-1);
        }
        return new ExpressionConfig(expression, eval,  null);
    }

    public String getName()
    {
        return NAME;
    }
}
