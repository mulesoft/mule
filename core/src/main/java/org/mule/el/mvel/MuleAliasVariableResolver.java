/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
