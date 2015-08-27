/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.mvel;

import org.mule.api.serialization.ObjectSerializer;

class MuleAliasVariableResolver extends MuleVariableResolver<Object>
{
    private static final long serialVersionUID = -4957789619105599831L;
    private String expression;
    private MVELExpressionExecutor executor;
    private MVELExpressionLanguageContext context;

    public MuleAliasVariableResolver(String name,
                                     String expression,
                                     MVELExpressionLanguageContext context,
                                     ObjectSerializer objectSerializer)
    {
        super(name, null, null, null);
        this.expression = expression;
        this.context = context;
        this.executor = new MVELExpressionExecutor(context.parserConfiguration, objectSerializer);
    }

    public MuleAliasVariableResolver(MuleAliasVariableResolver aliasVariableResolver,
                                     MVELExpressionLanguageContext newContext)
    {
        super(aliasVariableResolver.name, null, null, null);
        this.expression = aliasVariableResolver.expression;
        // Use single shared executor for all invocation to enable caching of compiled expressions
        this.executor = aliasVariableResolver.executor;
        this.context = newContext;
    }

    @Override
    public Object getValue()
    {
        return executor.execute(expression, context);
    }

    @Override
    public void setValue(Object value)
    {
        expression = expression + "= ___value";
        context.addFinalVariable("___value", value);
        executor.execute(expression, context);
    }
}
