/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.el.mvel;

import org.mule.api.MuleContext;
import org.mule.el.context.AppContext;
import org.mule.el.context.MuleInstanceContext;
import org.mule.el.context.ServerContext;
import org.mule.el.function.RegexExpressionLanguageFuntion;

import org.mvel2.ParserContext;

class StaticVariableResolverFactory extends MVELExpressionLanguageContext
{

    private static final long serialVersionUID = -6819292692339684915L;

    public StaticVariableResolverFactory(ParserContext parserContext, MuleContext muleContext)
    {
        super(parserContext, muleContext);
        addVariable("server", new ServerContext());
        addVariable("mule", new MuleInstanceContext(muleContext));
        addVariable("app", new AppContext(muleContext));
        declareFunction("regex", new RegexExpressionLanguageFuntion());
    }

}
