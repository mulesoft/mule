/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.mvel;

import org.mule.api.MuleContext;
import org.mule.el.context.AppContext;
import org.mule.el.context.MuleInstanceContext;
import org.mule.el.context.ServerContext;
import org.mule.el.function.DateTimeExpressionLanguageFuntion;
import org.mule.el.function.RegexExpressionLanguageFuntion;
import org.mule.mvel2.ParserConfiguration;
import org.mule.mvel2.integration.VariableResolver;

class StaticVariableResolverFactory extends MVELExpressionLanguageContext
{

    private static final long serialVersionUID = -6819292692339684915L;

    public StaticVariableResolverFactory(ParserConfiguration parserConfiguration, MuleContext muleContext)
    {
        super(parserConfiguration, muleContext);
        addVariable("server", new ServerContext());
        addVariable("mule", new MuleInstanceContext(muleContext));
        addVariable("app", new AppContext(muleContext));
        declareFunction("regex", new RegexExpressionLanguageFuntion());
        declareFunction("dateTime", new DateTimeExpressionLanguageFuntion());
    }

    public VariableResolver getVariableResolver(String name)
    {
        VariableResolver variableResolver = variableResolvers.get(name);
        if (variableResolver == null)
        {
            variableResolver = getNextVariableResolver(name);
        }
        return variableResolver;
    }

}
