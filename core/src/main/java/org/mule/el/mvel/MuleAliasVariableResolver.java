/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel;

class MuleAliasVariableResolver extends MuleVariableResolver<Object>
{
    private static final long serialVersionUID = -4957789619105599831L;
    private String expression;
    private MVELExpressionLanguageContext context;
    private MVELExpressionExecutor executor;

    public MuleAliasVariableResolver(String name, String expression, MVELExpressionLanguageContext context)
    {
        super(name, null, null, null);
        this.expression = expression;
        this.context = context;
        this.executor = new MVELExpressionExecutor(context.parserContext);
    }

    @Override
    public Object getValue()
    {
        return executor.execute(expression, context);
    }

    @Override
    public void setValue(Object value)
    {
        MVELExpressionLanguageContext newContext = new MVELExpressionLanguageContext(context);
        expression = expression + "= ___value";
        newContext.addFinalVariable("___value", value);
        executor.execute(expression, newContext);
    }
}
