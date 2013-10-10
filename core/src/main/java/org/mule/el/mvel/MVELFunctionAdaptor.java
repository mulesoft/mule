/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.el.mvel;

import org.mule.api.el.ExpressionLanguageContext;
import org.mule.api.el.ExpressionLanguageFunction;

import org.mvel2.ParserContext;
import org.mvel2.ast.Function;
import org.mvel2.integration.VariableResolverFactory;

@SuppressWarnings("serial")
class MVELFunctionAdaptor extends Function
{
    private ExpressionLanguageFunction function;

    public MVELFunctionAdaptor(String name, ExpressionLanguageFunction function, ParserContext parserContext)
    {
        super(name, new char[]{}, new char[]{}, 0, parserContext);
        this.function = function;
    }

    @Override
    public Object call(Object ctx, Object thisValue, VariableResolverFactory factory, Object[] parms)
    {
        while (!(factory instanceof ExpressionLanguageContext) && factory != null)
        {
            factory = factory.getNextFactory();
        }
        return function.call(parms, (ExpressionLanguageContext) factory);
    }

    @Override
    public void checkArgumentCount(int passing)
    {
        // no-op
    }
}
