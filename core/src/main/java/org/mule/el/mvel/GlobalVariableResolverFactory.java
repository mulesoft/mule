/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.el.mvel;

import org.mule.api.MuleContext;
import org.mule.api.el.ExpressionLanguageExtension;

import java.util.Map.Entry;

import org.mvel2.ParserContext;
import org.mvel2.ast.Function;

class GlobalVariableResolverFactory extends MVELExpressionLanguageContext
{
    private MVELExpressionLanguageContext parent;

    private static final long serialVersionUID = -6819292692339684915L;

    public GlobalVariableResolverFactory(MVELExpressionLanguage el,
                                         MVELExpressionLanguageContext parent,
                                         ParserContext parserContext,
                                         MuleContext muleContext)
    {
        super(parserContext, muleContext);
        this.parent = parent;
        for (ExpressionLanguageExtension extension : muleContext.getRegistry().lookupObjectsForLifecycle(
            ExpressionLanguageExtension.class))
        {
            extension.configureContext(parent);
        }
        for (Entry<String, Class<?>> function : el.imports.entrySet())
        {
            importClass(function.getKey(), function.getValue());
        }
        for (Entry<String, String> alias : el.aliases.entrySet())
        {
            addAlias(alias.getKey(), alias.getValue());
        }
        for (Entry<String, Function> function : el.globalFunctions.entrySet())
        {
            addFinalVariable(function.getKey(), function.getValue());
        }
    }

    @Override
    MVELExpressionLanguageContext getParentContext()
    {
        return parent;
    }
}
